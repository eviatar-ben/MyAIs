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

# Load environment variables from .env file (if present)
load_dotenv()

app = Flask(__name__)

# --- Configure Gemini API ---
# Load service account key from file (RECOMMENDED FOR PRODUCTION)
# In a real app, load this from a secure location like Google Secret Manager
# or environment variables, NOT directly from a file in your repo.
try:
    with open('service_account_key.json', 'r') as f:
        gemini_credentials = json.load(f)
    genai.configure(credentials=gemini_credentials)
    print("Gemini API configured successfully.")
except FileNotFoundError:
    print("Error: service_account_key.json not found. Ensure it's in the same directory.")
    exit(1)  # Exit if credentials are not found

# Initialize the generative model
# Use a multimodal model that supports video input (e.g., gemini-pro-vision, gemini-1.5-flash)
# As of current Gemini Pro Vision only supports images for now. For video, you would typically
# process it into keyframes or use specialized video models when they become available.
# For simplicity, we'll assume sending a series of images (keyframes) for now.
# NOTE: Gemini 1.5 Flash and Pro are the most capable models for multimodal input.
# You might need to adjust the model name based on the latest available models that
# support video processing. For general visual tasks with image sequences, 'gemini-1.5-flash' is good.
model = genai.GenerativeModel('gemini-1.5-flash')  # Or 'gemini-1.5-pro' for more advanced reasoning


# --- Helper Function for Video Processing ---
def extract_keyframes(video_bytes, interval_ms=500, max_frames=20):
    """
    Extracts keyframes from a video byte stream.
    Args:
        video_bytes (bytes): The raw bytes of the video file.
        interval_ms (int): Interval in milliseconds to extract frames.
        max_frames (int): Maximum number of frames to extract.
    Returns:
        list: A list of base64 encoded image strings (JPEG format).
    """
    keyframes = []
    video_stream = io.BytesIO(video_bytes)

    # Write to a temporary file because cv2.VideoCapture needs a file path
    # In a production environment, consider using a cloud storage bucket
    # for larger files or direct in-memory processing if opencv supports it more directly.
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=True) as temp_video_file:
        temp_video_file.write(video_bytes)
        temp_video_file_path = temp_video_file.name

        cap = cv2.VideoCapture(temp_video_file_path)

        if not cap.isOpened():
            print("Error: Could not open video file.")
            return []

        fps = cap.get(cv2.CAP_PROP_FPS)
        if fps == 0:
            print("Warning: Could not get FPS, assuming 30 FPS.")
            fps = 30

        frame_count = 0
        current_time_ms = 0

        while True:
            cap.set(cv2.CAP_PROP_POS_MSEC, current_time_ms)
            ret, frame = cap.read()

            if not ret or frame_count >= max_frames:
                break

            # Encode frame to JPEG
            _, buffer = cv2.imencode('.jpg', frame)
            jpg_as_text = base64.b64encode(buffer).decode('utf-8')
            keyframes.append(jpg_as_text)

            current_time_ms += interval_ms
            frame_count += 1

        cap.release()

    print(f"Extracted {len(keyframes)} keyframes.")
    return keyframes


# --- API Endpoint ---
@app.route('/process_video', methods=['POST'])
def process_video():
    if 'video' not in request.files:
        return jsonify({"error": "No video file provided"}), 400
    if 'prompt' not in request.form:
        return jsonify({"error": "No prompt provided"}), 400

    video_file = request.files['video']
    prompt_text = request.form['prompt']

    # Read video bytes
    video_bytes = video_file.read()

    # Extract keyframes from the video
    keyframes = extract_keyframes(video_bytes, interval_ms=300,
                                  max_frames=30)  # Adjust interval and max_frames as needed

    if not keyframes:
        return jsonify({"error": "Failed to extract keyframes from video"}), 500

    # Prepare content for Gemini API
    # The content list alternates between text and image parts
    # Gemini 1.5 models are excellent for this.
    parts = [prompt_text]
    for frame in keyframes:
        parts.append({
            'mime_type': 'image/jpeg',
            'data': base64.b64decode(frame)  # Decode base64 back to bytes for Gemini API
        })

    try:
        # Call Gemini API
        print("Sending request to Gemini API...")
        response = model.generate_content(parts)

        # Extract the text response from Gemini
        gemini_response_text = response.text
        print(f"Gemini response: {gemini_response_text}")

        # Generate audio from the Gemini response
        tts = gTTS(text=gemini_response_text, lang='en', slow=False)  # You can adjust language

        audio_buffer = io.BytesIO()
        tts.write_to_fp(audio_buffer)
        audio_buffer.seek(0)  # Rewind to the beginning

        # Encode audio to base64 for sending back to Android
        audio_base64 = base64.b64encode(audio_buffer.read()).decode('utf-8')

        return jsonify({
            "message": "Processing successful",
            "gemini_text_response": gemini_response_text,
            "audio_base64": audio_base64
        }), 200

    except Exception as e:
        print(f"Error calling Gemini API or processing: {e}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    # For development, you can run on a specific port.
    # For production, use a WSGI server like Gunicorn or uWSGI.
    app.run(host='0.0.0.0', port=5000, debug=True)