import os
import numpy as np
from deepface import DeepFace
import cv2

def cosine_similarity(a, b):
    a = np.array(a)
    b = np.array(b)
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

def load_embeds(embeds_dir):
    embeds = {}
    for fname in os.listdir(embeds_dir):
        if fname.endswith('.npy'):
            name = fname.rsplit('.', 1)[0]
            path = os.path.join(embeds_dir, fname)
            embeds[name] = np.load(path)
    return embeds

embeds_dir = "face_embeds"
database = load_embeds(embeds_dir)

def verify_image(img, threshold=0.7):
    tmp_path = f"{os.getcwd()}/.cache/tmp.jpg"
    cv2.imwrite(tmp_path, img)
    try:
        embedding_objs = DeepFace.represent(img_path=img_path, model_name='ArcFace', enforce_detection=True)
    except Exception as e:
        print(f"Error processing image: {e}")
        return

    if not isinstance(embedding_objs, list):
        embedding_objs = [embedding_objs]

    matches = []
    scores = []
    for idx, emb_obj in enumerate(embedding_objs):
        emb = emb_obj['embedding']
        found_match = False
        for db_name, db_emb in database.items():
            sim = cosine_similarity(emb, db_emb)
            if sim > threshold:
                print(f"Face {idx} matches with {db_name} (similarity: {sim:.3f})")
                found_match = True
                matches.append(db_name)
                scores.append(sim)
        if not found_match:
            print(f"Face {idx} did not match anyone in the database.")
        
    print('matches: ', matches)
    print('scores: ', scores)
    return matches, scores

if __name__ == "__main__":

    img_path = "/home/david/Documents/huji/msc/MyAIs/face_recognition/faces/david.jpg"
    img = cv2.imread(img_path)
    verify_image(img)