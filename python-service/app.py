from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import pymupdf4llm
import base64, tempfile, os, re
from sentence_transformers import SentenceTransformer

app = FastAPI(title="Legal RAG Ingestion Service")

model = SentenceTransformer('all-MiniLM-L6-v2')


# ─── Request Models ────────────────────────────────────────────────────────────

class IngestRequest(BaseModel):
    pdf_base64: str
    document_id: str
    contract_type: str

class LawIngestRequest(BaseModel):
    text: str
    law_document_id: str
    law_type: str

class EmbedRequest(BaseModel):
    text: str


# ─── Health Check ──────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


# ─── Single Text Embedding ─────────────────────────────────────────────────────

@app.post("/embed")
def embed(req: EmbedRequest):
    if not req.text or not req.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")
    embedding = model.encode(req.text).tolist()
    return {"embedding": embedding}


# ─── Contract PDF Ingestion ────────────────────────────────────────────────────

@app.post("/ingest")
def ingest(req: IngestRequest):
    try:
        pdf_bytes = base64.b64decode(req.pdf_base64)
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid base64 PDF data")

    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(suffix='.pdf', delete=False) as f:
            f.write(pdf_bytes)
            tmp_path = f.name

        pages = pymupdf4llm.to_markdown(tmp_path, page_chunks=True)

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"PDF extraction failed: {str(e)}")
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)

    chunks = []
    chunk_index = 0
    chunk_size = 300
    overlap = 50

    for i, page in enumerate(pages):
        page_content = page['text'].strip() if isinstance(page, dict) else str(page).strip()
        if len(page_content) < 50:
            continue

        words = page_content.split()

        if len(words) <= chunk_size:
            # Page is small enough — keep as single chunk
            embedding = model.encode(page_content).tolist()
            chunks.append({
                "content": page_content,
                "embedding": embedding,
                "page_number": i + 1,
                "chunk_index": chunk_index
            })
            chunk_index += 1
        else:
            # Split into overlapping sub-chunks
            j = 0
            while j < len(words):
                sub_words = words[j:j + chunk_size]
                sub_content = ' '.join(sub_words)
                if len(sub_content) >= 50:
                    embedding = model.encode(sub_content).tolist()
                    chunks.append({
                        "content": sub_content,
                        "embedding": embedding,
                        "page_number": i + 1,
                        "chunk_index": chunk_index
                    })
                    chunk_index += 1
                j += chunk_size - overlap

    return {
        "document_id": req.document_id,
        "total_chunks": len(chunks),
        "chunks": chunks
    }


# ─── Indian Law Text Ingestion ─────────────────────────────────────────────────

@app.post("/ingest-law")
def ingest_law(req: LawIngestRequest):
    if not req.text or not req.text.strip():
        raise HTTPException(status_code=400, detail="Text cannot be empty")

    words = req.text.split()
    chunk_size = 500
    overlap = 50
    chunks = []
    i = 0
    chunk_index = 0

    while i < len(words):
        chunk_words = words[i : i + chunk_size]
        content = ' '.join(chunk_words)

        embedding = model.encode(content).tolist()

        section_match = re.search(r'[Ss]ection\s+\d+[A-Za-z]?', content)
        section_ref = section_match.group(0) if section_match else f"Part {chunk_index + 1}"

        chunks.append({
            "content": content,
            "embedding": embedding,
            "section_reference": section_ref,
            "chunk_index": chunk_index
        })

        i += chunk_size - overlap
        chunk_index += 1

    return {
        "law_document_id": req.law_document_id,
        "total_chunks": len(chunks),
        "chunks": chunks
    }