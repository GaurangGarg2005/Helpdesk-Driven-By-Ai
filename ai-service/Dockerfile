# HelpDeskAI — FastAPI AI Service Dockerfile
FROM python:3.11-slim

WORKDIR /app

# System deps needed by sentence-transformers / faiss
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    g++ \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies first for better layer caching
COPY requirements.txt .
RUN pip install --no-cache-dir --timeout 120 -r requirements.txt

# Copy application source
COPY . .

# Pre-download the sentence-transformer model so first request isn't slow
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('all-MiniLM-L6-v2')" 2>/dev/null || true

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
