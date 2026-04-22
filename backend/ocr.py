import io
import re
import base64
import numpy as np
import cv2
import pytesseract
from PIL import Image


def preprocess_image(pil_image: Image.Image) -> list:
    img = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)

    target_w = 1200
    h, w = img.shape[:2]
    if w < target_w:
        scale = target_w / w
        img = cv2.resize(img, (target_w, int(h * scale)), interpolation=cv2.INTER_CUBIC)

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    versions = []

    denoised = cv2.medianBlur(gray, 3)

    thresh1 = cv2.adaptiveThreshold(denoised, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                     cv2.THRESH_BINARY, 31, 10)
    versions.append(("adaptive", cv2.cvtColor(thresh1, cv2.COLOR_GRAY2BGR)))

    _, thresh2 = cv2.threshold(denoised, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    versions.append(("otsu", cv2.cvtColor(thresh2, cv2.COLOR_GRAY2BGR)))

    kernel = np.array([[-1,-1,-1],[-1,9,-1],[-1,-1,-1]])
    sharpened = cv2.filter2D(gray, -1, kernel)
    versions.append(("sharp", cv2.cvtColor(sharpened, cv2.COLOR_GRAY2BGR)))

    clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8,8))
    enhanced = clahe.apply(gray)
    versions.append(("clahe", cv2.cvtColor(enhanced, cv2.COLOR_GRAY2BGR)))

    return versions


def clean_text(text: str) -> str:
    """Remove garbage OCR noise: keep only words with enough real letters."""
    words = text.split()
    clean = []
    for w in words:
        letters = sum(c.isalpha() for c in w)
        # Keep word only if it has at least 2 letters and is mostly alphanumeric
        if letters >= 2 and letters / max(len(w), 1) >= 0.5:
            clean.append(w)
    return " ".join(clean)


def run_tesseract(pil_image: Image.Image, config: str) -> tuple:
    try:
        data = pytesseract.image_to_data(
            pil_image, config=config,
            output_type=pytesseract.Output.DICT
        )
        words, confs = [], []
        for i, word in enumerate(data["text"]):
            conf = int(data["conf"][i])
            word = word.strip()
            if conf > 50 and word:
                letters = sum(c.isalpha() for c in word)
                if letters >= 2 and letters / len(word) >= 0.5:
                    words.append(word)
                    confs.append(conf)
        text = " ".join(words)
        avg = float(np.mean(confs)) if confs else 0.0
        return text, avg
    except Exception:
        return "", 0.0


def to_base64(bgr_img: np.ndarray) -> str:
    ok, buf = cv2.imencode(".png", bgr_img)
    if not ok:
        return ""
    return base64.b64encode(buf.tobytes()).decode("utf-8")


def extract_text(pil_image: Image.Image) -> dict:
    versions = preprocess_image(pil_image)

    configs = [
        r"--oem 3 --psm 6",
        r"--oem 3 --psm 11",
        r"--oem 3 --psm 4",
        r"--oem 3 --psm 3",
        r"--oem 3 --psm 7",
    ]

    best_text = ""
    best_conf = 0.0
    best_img  = versions[0][1]

    for label, bgr in versions:
        pil = Image.fromarray(cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB))
        for cfg in configs:
            text, conf = run_tesseract(pil, cfg)
            if text.strip() and conf > best_conf:
                best_text = text
                best_conf = conf
                best_img  = bgr

    # Fallback with cleaning
    if not best_text.strip():
        try:
            raw = pytesseract.image_to_string(pil_image, config=r"--oem 3 --psm 6").strip()
            best_text = clean_text(raw)
        except Exception:
            best_text = ""

    return {
        "text": best_text,
        "confidence": round(best_conf, 2),
        "preprocessed_b64": to_base64(best_img)
    }
