import os
import numpy as np
from deepface import DeepFace
import cv2

faces_dir = "faces"
embeds_dir = "face_embeds"
os.makedirs(embeds_dir, exist_ok=True)

for filename in os.listdir(faces_dir):
    if filename.lower().endswith(('.jpg', '.jpeg', '.png')):
        img_path = os.path.join(faces_dir, filename)
        try:
            embedding_objs = DeepFace.represent(img_path=img_path, model_name='ArcFace', enforce_detection=True)
            if isinstance(embedding_objs, list):
                # If multiple faces, take the first (or you can loop through all)
                for idx, emb in enumerate(embedding_objs):
                    name, _ = os.path.splitext(filename)
                    out_name = f"{name}_{idx}.npy" if len(embedding_objs) > 1 else f"{name}.npy"
                    out_path = os.path.join(embeds_dir, out_name)
                    np.save(out_path, emb["embedding"])
            else:
                name, _ = os.path.splitext(filename)
                out_path = os.path.join(embeds_dir, f"{name}.npy")
                np.save(out_path, embedding_objs["embedding"])
            print(f"Processed {filename}")
        except Exception as e:
            print(f"Error processing {filename}: {e}")