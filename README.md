# SignLens — Street Sign Translator

Real-time street sign OCR and translation. Python FastAPI backend + light-themed frontend.

## Run on Linux/Mac

```bash
# Step 1: Install Tesseract
sudo apt install tesseract-ocr -y        # Ubuntu/Linux
# brew install tesseract                 # Mac

# Step 2: Run the app
bash run.sh

# Step 3: Open browser
# http://localhost:8000
```

## Run on Windows

```bash
cd backend
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```
Then open http://localhost:8000

## Languages
Kannada · Telugu · Tamil · Marathi · Malayalam · Hindi · English · French · Spanish · Arabic

## Stack
- Python 3.10+ · FastAPI · Tesseract OCR · OpenCV · deep-translator
