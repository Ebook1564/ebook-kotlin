#!/bin/bash
# Piper TTS startup script
# Place this in /opt/ebook-pipertts/run_piper.sh

# Activate virtual environment
source /opt/ebook-pipertts/venv/bin/activate

# Select a voice model (adjust based on available models)
MODEL_PATH="/opt/ebook-pipertts/voices/en_US-lessac-medium.onnx"
LOG_FILE="/opt/ebook-pipertts/piper.log"

echo "Starting piper-tts server with model: $MODEL_PATH" >> ${LOG_FILE}
echo "Timestamp: $(date)" >> ${LOG_FILE}

# Run piper HTTP server
python -m piper.http_server -m ${MODEL_PATH} >> ${LOG_FILE} 2>&1
