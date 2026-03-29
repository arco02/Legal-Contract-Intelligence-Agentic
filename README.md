# ⚖️ Legal Contract Intelligence Assistant (Agentic RAG)

> An advanced, full-stack Agentic RAG (Retrieval-Augmented Generation) system designed to analyze private legal contracts and cross-reference them with Indian Contract Law.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=flat&logo=spring)
![React](https://img.shields.io/badge/React-18-61DAFB?style=flat&logo=react)
![FastAPI](https://img.shields.io/badge/FastAPI-0.110-009688?style=flat&logo=fastapi)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_pgvector-316192?style=flat&logo=postgresql)

## 📖 Overview

Legal contracts are long, dense, and scattered. This project acts as an AI-powered legal assistant. Users can upload business contracts, ask natural language questions, and receive cited answers grounded in both their specific uploaded documents and an embedded corpus of Indian legal statutes (e.g., Indian Contract Act 1872, Arbitration Act).

**Disclaimer:** *This tool is for informational and research purposes only and does not constitute legal advice.*

## 📸 Application Interface

**1. Internal Knowledge Base Retrieval**
*The AI agent successfully retrieving and citing exact clauses directly from an uploaded private contract.

<img width="1365" height="632" alt="Screenshot 2026-03-29 212629" src="https://github.com/user-attachments/assets/f12231bf-6fb2-4429-8730-392632f21510" />

<br>
<br>

**2. Autonomous Web Search Fallback (CRAG)**
*The Corrective RAG system autonomously routing to a live web search when internal context is insufficient.*

<img width="1155" height="339" alt="Screenshot 2026-03-29 213251" src="https://github.com/user-attachments/assets/d3d08c88-6507-4046-be20-38bea49359d1" />

## ✨ Core Architecture & Features

* **🧠 Agentic Query Routing:** Utilizes a lightweight LLM (`RouterNode.java`) to classify query intent and dynamically route searches to specific vector namespaces (Commercial, Corporate IP, Operational) or the public Law Corpus, drastically reducing irrelevant context retrieval.
* **🛡️ Corrective RAG (CRAG) Gate:** Implements a two-stage relevance check (Cosine Similarity + LLM Sufficiency Grader). If the retrieved context is deemed insufficient to answer the query accurately, the system autonomously falls back to a Web Search agent.
* **📄 Layout-Aware Ingestion:** Bypasses standard flat-text PDF extraction by utilizing `pymupdf4llm` in a dedicated Python microservice to convert legal PDFs to Markdown, preserving critical table structures and formatting before generating embeddings.
* **⚡ Real-Time SSE Streaming:** Features ultra-low latency token streaming from the Groq-powered LLM directly to the React frontend via Server-Sent Events (`SseEmitter` and custom React hooks).
* **🔒 Secure & Stateless:** Protected by Spring Security with stateless JWT authentication and proper CORS configuration for decoupled frontend/backend deployments.

## 🛠️ Tech Stack

* **Backend Orchestrator:** Java, Spring Boot, Spring Security, LangChain4j
* **Frontend Client:** React, Vite, Tailwind CSS (or standard CSS), Zustand
* **Ingestion Microservice:** Python, FastAPI, `sentence-transformers`, PyMuPDF
* **Database & Vector Store:** PostgreSQL with `pgvector`
* **LLM Provider:** Groq (LLaMA 3.3 70B for generation, LLaMA 3.1 8B for routing/grading)

## 📂 Monorepo Structure

```text
Legal-Contract-Intelligence-Agentic/
├── legal-rag-assistant/    # Spring Boot backend (RAG orchestration, Auth, API)
├── frontend/               # React Vite SPA (Chat UI, Document Management)
└── python-service/         # FastAPI service (PDF extraction, Embeddings)
```

## 🚀 Local Development Setup

### 1. Database Configuration
Ensure PostgreSQL is running locally (or via a cloud provider like Neon). Execute the following to enable the vector extension:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```
*(Run the provided schema SQL to generate the `users`, `documents`, `contract_chunks`, and `messages` tables).*

### 2. Python Ingestion Service
Handles document parsing and generating `all-MiniLM-L6-v2` embeddings.
```bash
cd python-service
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
uvicorn app:app --reload --port 8000
```

### 3. Spring Boot Backend
Configure your environment variables (or place them in `application-dev.properties`):
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/legalrag
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
JWT_SECRET=your_secure_256_bit_secret_key
GROQ_API_KEY=your_groq_api_key
PYTHON_SERVICE_URL=http://localhost:8000
```
Run the application:
```bash
cd legal-rag-assistant
./mvnw spring-boot:run
```

### 4. React Frontend
Configure your local environment variables in `frontend/.env.local`:
```env
VITE_API_BASE_URL=http://localhost:8080
```
Start the development server:
```bash
cd frontend
npm install
npm run dev
```

