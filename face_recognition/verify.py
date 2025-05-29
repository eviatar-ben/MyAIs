import os
import torch
import numpy as np
from deepface import DeepFace

EMBEDS_DIR = "face_embeds"
MODEL_NAME = "VGG-Face"
THRESHOLD = 0.6  # You may need to adjust this threshold

def cosine_similarity(a, b):
    a, b = np.array(a), np.array(b)
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def load_known_embeddings():
    embeddings = {}
    for file in os.listdir(EMBEDS_DIR):
        if file.endswith(".pth"):
            name = os.path.splitext(file)[0]
            embeddings[name] = torch.load(os.path.join(EMBEDS_DIR, file)).numpy()
    return embeddings

def identify_faces_in_image(image_path):
    try:
        known_embeddings = load_known_embeddings()
        results = []

        # Detect faces and compute their embeddings
        representations = DeepFace.represent(
            img_path=image_path,
            model_name=MODEL_NAME,
            enforce_detection=True
        )

        if not representations:
            print("No faces detected.")
            return results

        for face_repr in representations:
            detected_embedding = face_repr["embedding"]
            best_match = "Unknown"
            best_score = -1

            for name, known_embedding in known_embeddings.items():
                similarity = cosine_similarity(detected_embedding, known_embedding)
                if similarity > best_score:
                    best_score = similarity
                    best_match = name

            if best_score >= THRESHOLD:
                results.append((best_match, best_score))
            else:
                results.append(("Unknown", best_score))

        return results

    except Exception as e:
        print(f"Error during face identification: {e}")
        return []

# Example usage
if __name__ == "__main__":
    image_path = "test_image.jpg"
    matches = identify_faces_in_image(image_path)
    for i, (name, score) in enumerate(matches):
        print(f"Face {i+1}: {name} (similarity: {score:.2f})")
