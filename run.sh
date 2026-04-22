#!/bin/bash
echo "=========================================="
echo "  SignLens - Installing & Starting"
echo "=========================================="
echo ""
echo "Step 1: Installing Python packages..."
cd "$(dirname "$0")/backend"
pip install -r requirements.txt
echo ""
echo "Step 2: Starting server..."
echo ""
echo "  Open your browser at: http://localhost:8000"
echo "  Press Ctrl+C to stop"
echo ""
uvicorn main:app --reload --port 8000
