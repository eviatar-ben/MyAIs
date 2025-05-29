# preprocess_faces.py
import os
import torch
from deepface import DeepFace

FACES_DIR = "faces"
EMBEDS_DIR = "face_embeds"
MODEL_NAME = "VGG-Face"  # You can change this to "Facenet", "ArcFace", etc.

os.makedirs(EMBEDS_DIR, exist_ok=True)

print(f"Using model: {MODEL_NAME}")

for filename in os.listdir(FACES_DIR):
    if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
        path = os.path.join(FACES_DIR, filename)
        print('Processing:', path)
        try:
            # No 'model=' argument
            representations = DeepFace.represent(img_path=path, model_name=MODEL_NAME, enforce_detection=True)
            if not representations:
                print(f"No face found in {filename}")
                continue

            embedding = representations[0]["embedding"]
            name = os.path.splitext(filename)[0]
            torch.save(torch.tensor(embedding), os.path.join(EMBEDS_DIR, f"{name}.pth"))
            print(f"Saved embedding for {filename}")

        except Exception as e:
            print(f"Error processing {filename}: {e}")
