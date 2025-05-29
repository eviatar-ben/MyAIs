import os
import io
import json
from flask import Flask, request, jsonify
import cv2
import base64
import google.generativeai as genai
from dotenv import load_dotenv
from gtts import gTTS
import tempfile
import speech_recognition as sr # Import SpeechRecognition
from pydub import AudioSegment # Import pydub

from google.cloud import aiplatform

HAZARD_DETECTION_PROMPT = """
You are an AI assistant specialized in providing critical, immediate hazard warnings for a blind person.
Analyze the provided sequence of video frames to identify any potential dangers or obstacles.

**Focus:** Identify hazards like:
- Uneven ground (e.g., potholes, cracks, curbs, broken pavement)
- Obstacles (e.g., objects sticking out, low-hanging branches, parked bikes, trash cans)
- Changes in elevation (e.g., stairs up/down, ramps, sudden drops)
- Moving objects (e.g., approaching vehicles, people, pets)
- Other immediate environmental dangers.

**Output Rule (IMPORTANT):**
Your response MUST be ONLY a very short, actionable phrase (maximum 6 words).
If a significant hazard is detected, describe it clearly and concisely.
If NO significant hazard is detected, your response MUST be: "Path clear."

**Examples of desired output:**
- "Pothole directly ahead!"
- "Stairs up front to your left."
- "Object on right."
- "Path clear."
- "Approaching vehicle!"
- "Low hanging branch ahead."

Now, analyze the frames and provide the warning or 'Path clear'.
"""


OBJECT_DETECTION_PROMPT_TEMPLATE = """
You are an AI assistant that helps blind individuals locate objects.
Your task is to analyze the provided sequence of video frames and fulfill the user's specific request.

**User Query:** {user_query}

**Goal:**
1.  **Locate Object:** Find the object mentioned in the "User Query" within the frames.
2.  **Describe Position:** Provide a very concise description of the object's relative position (e.g., "to your left," "on the shelf," "straight ahead").
3.  **Concise Message:** The message should be extremely short and actionable.
4.  **Default/Not Found:** If the object is not found in the frames, return a specific default string.

**Output Rule (IMPORTANT):**
Your response MUST be ONLY a very short, actionable phrase (maximum 6 words).
If the object is found, describe its location.
If the object is NOT found, your response MUST be: "Object not seen."

**Examples of desired output for User Query "where is the salt jar?":**
- "Salt jar, far right on shelf."
- "Salt jar directly ahead."
- "Object not seen."

**Examples of desired output for User Query "find my keys":**
- "Keys on table to left."
- "Keys on floor straight ahead."
- "Object not seen."

Now, analyze the frames based on the User Query and provide the location or 'Object not seen'.
"""

# Load environment variables from .env file (if present)
load_dotenv()

app = Flask(__name__)

# --- Configure Gemini API ---
SERVICE_ACCOUNT_KEY_PATH = "service_account_key.json"
VERTEX_AI_REGION = "us-central1"
VERTEX_AI_MODEL_ID = "gemini-1.5-flash"

def authenticate_gcp(service_account_path):
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = service_account_path
    with open(service_account_path, 'r') as f:
        project_id = json.load(f).get("project_id")
    aiplatform.init(project=project_id, location=VERTEX_AI_REGION)
    return project_id

def load_gemini_model(model_id):
    return genai.GenerativeModel(
        model_name=model_id,
        generation_config={"temperature": 0.4}
    )

def base64_to_image_part(b64_image):
    return {
        "mime_type": "image/jpeg",
        "data": base64.b64decode(b64_image)
    }

def run_gemini_inference(model, image_parts, prompt):
    contents = [prompt] + image_parts
    response = model.generate_content(contents)
    return response.text.strip() if response and response.text else None

def synthesize_speech_to_base64(text):
    tts = gTTS(text=text, lang='en')
    with io.BytesIO() as audio_bytes_io:
        tts.write_to_fp(audio_bytes_io)
        audio_bytes_io.seek(0)
        audio_base64 = base64.b64encode(audio_bytes_io.read()).decode("utf-8")
    return audio_base64

authenticate_gcp(SERVICE_ACCOUNT_KEY_PATH)
model = load_gemini_model(VERTEX_AI_MODEL_ID)

def extract_keyframes(video_bytes, threshold=0.6, max_frames=30, target_width=320, target_height=240):
    keyframes_list = []
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=True) as temp_video_file:
        temp_video_file.write(video_bytes)
        cap = cv2.VideoCapture(temp_video_file.name)

        if not cap.isOpened():
            print("Error: Could not open video file.")
            return []

        success, prev_frame = cap.read()
        if not success:
            cap.release()
            print("Error: Could not read first frame.")
            return []

        prev_frame = cv2.resize(prev_frame, (target_width, target_height))
        prev_gray = cv2.cvtColor(prev_frame, cv2.COLOR_BGR2GRAY)
        prev_hist = cv2.calcHist([prev_gray], [0], None, [256], [0, 256])
        prev_hist = cv2.normalize(prev_hist, prev_hist).flatten()

        _, buffer = cv2.imencode('.jpg', prev_frame)
        keyframes_list.append(base64.b64encode(buffer).decode('utf-8'))

        while True:
            success, frame = cap.read()
            if not success or len(keyframes_list) >= max_frames:
                break
            frame = cv2.resize(frame, (target_width, target_height))
            curr_gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
            curr_hist = cv2.calcHist([curr_gray], [0], None, [256], [0, 256])
            curr_hist = cv2.normalize(curr_hist, curr_hist).flatten()
            diff = cv2.compareHist(prev_hist, curr_hist, cv2.HISTCMP_CORREL)

            if diff < threshold:
                _, buffer = cv2.imencode('.jpg', frame)
                keyframes_list.append(base64.b64encode(buffer).decode('utf-8'))
                prev_hist = curr_hist

        cap.release()
    return keyframes_list


# --- API Endpoint ---
@app.route('/process_video', methods=['POST'])
def process_video():
    if 'video' not in request.files:
        return jsonify({"error": "No video file provided"}), 400

    if 'audio_prompt' in request.form:
        audio_file = request.files['audio_prompt']  # Get the audio file
        audio_bytes = audio_file.read()
        user_prompt_text = "unintelligible speech or no prompt"  # Default in case ASR fails
        r = sr.Recognizer()
        try:
            # pydub can help convert various formats to a format recognize_google likes (like WAV)
            audio_segment = AudioSegment.from_file(io.BytesIO(audio_bytes))
            # Export to WAV in memory for SpeechRecognition
            wav_buffer = io.BytesIO()
            audio_segment.export(wav_buffer, format="wav")
            wav_buffer.seek(0)  # Rewind to beginning

            with sr.AudioFile(wav_buffer) as source:
                audio_data = r.record(source)  # Read the entire audio file

            print("Attempting ASR...")
            user_prompt_text = r.recognize_google(audio_data, language="en-US")  # You can specify language
            print(f"ASR recognized: '{user_prompt_text}'")

        except sr.UnknownValueError:
            print("ASR could not understand audio")
            user_prompt_text = "unintelligible speech"
        except sr.RequestError as e:
            print(f"Could not request results from ASR service; {e}")
            user_prompt_text = "ASR service error"
        except Exception as e:  # Catch pydub or other potential audio processing errors
            print(f"General error during audio processing for ASR: {e}")
            user_prompt_text = "audio processing failed"
        print("User prompt transcript: ", user_prompt_text)
        prompt_text = OBJECT_DETECTION_PROMPT_TEMPLATE.format(user_query=user_prompt_text)
    else:
        prompt_text = HAZARD_DETECTION_PROMPT

    video_file = request.files['video']

    # Read video bytes
    video_bytes = video_file.read()

    # Extract keyframes from the video
    keyframes_b64 = extract_keyframes(video_bytes)  # Adjust interval and max_frames as needed

    if not keyframes_b64:
        return jsonify({"error": "Failed to extract keyframes from video"}), 500

    image_parts = [base64_to_image_part(b64) for b64 in keyframes_b64]
    # Step 2: Run Gemini inference
    try:
        print("querying Gemini")
        gemini_response = run_gemini_inference(model, image_parts, prompt_text)
        if not gemini_response:
            raise RuntimeError("Gemini returned no result.")
        print("Response: " + gemini_response)
        audio_b64 = synthesize_speech_to_base64(gemini_response)

        return jsonify({
            "message": "Processing successful",
            "gemini_text_response": gemini_response,
            "audio_base64": audio_b64
        }), 200

    except Exception as e:
        print(f"Error calling Gemini API or processing: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    # For development, you can run on a specific port.
    # For production, use a WSGI server like Gunicorn or uWSGI.
    app.run(host='0.0.0.0', port=5001, debug=True)