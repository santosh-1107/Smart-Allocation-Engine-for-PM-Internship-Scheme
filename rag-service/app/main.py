import os
import psycopg2
from fastapi import FastAPI
from pydantic import BaseModel
from contextlib import asynccontextmanager

# Load sentence transformer model for query embeddings
try:
    from sentence_transformers import SentenceTransformer
    embed_model = SentenceTransformer('all-MiniLM-L6-v2')
    print("RAG SentenceTransformer loaded successfully.")
except Exception as e:
    print(f"RAG SentenceTransformer failed to load: {e}")
    embed_model = None

# Database connection parameters
DB_HOST = os.getenv("POSTGRES_HOST", "localhost")
DB_PORT = os.getenv("POSTGRES_PORT", "5432")
DB_NAME = os.getenv("POSTGRES_DB", "pmis")
DB_USER = os.getenv("POSTGRES_USER", "pmis")
DB_PASS = os.getenv("POSTGRES_PASSWORD", "pmis_dev")

def get_db_connection():
    return psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USER,
        password=DB_PASS
    )

app = FastAPI(title="PMIS Policy RAG Service", version="0.1.0")

class QueryRequest(BaseModel):
    question: str
    role: str

class QueryResponse(BaseModel):
    answer: str
    confidence: float
    sources: list[dict]
    requires_human_review: bool

@app.get("/health")
def health():
    return {"status": "ok", "service": "rag-service"}

@app.post("/v1/query", response_model=QueryResponse)
def query(q: QueryRequest):
    question_lower = q.question.strip().lower()
    
    # 1. Generate query embedding
    query_emb = None
    if embed_model is not None:
        try:
            query_emb = embed_model.encode(q.question).tolist()
        except Exception as e:
            print(f"Failed to generate query embedding: {e}")

    vector_results = []
    keyword_results = []

    # 2. Query DB
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        # Vector search (using cosine distance)
        if query_emb is not None:
            try:
                emb_str = "[" + ",".join(map(str, query_emb)) + "]"
                cur.execute(
                    """
                    SELECT pc.id, pd.title, pd.url, pc.section_title, pc.content, 
                           (pc.embedding <=> %s::vector) AS distance
                    FROM policy_chunks pc
                    JOIN policy_documents pd ON pc.document_id = pd.id
                    ORDER BY distance ASC
                    LIMIT 5;
                    """,
                    (emb_str,)
                )
                for row in cur.fetchall():
                    # Distance is between 0 (identical) and 2 (orthogonal/opposite). Convert to similarity: 1 - distance
                    sim = 1.0 - float(row[5])
                    vector_results.append({
                        "id": row[0],
                        "doc_title": row[1],
                        "doc_url": row[2],
                        "section": row[3],
                        "content": row[4],
                        "score": round(max(0.0, sim), 4)
                    })
            except Exception as ev:
                print(f"Vector search failed (pgvector might not be initialized or populated yet): {ev}")
                conn.rollback()

        # Keyword search
        try:
            search_pattern = f"%{q.question.strip()}%"
            cur.execute(
                """
                SELECT pc.id, pd.title, pd.url, pc.section_title, pc.content
                FROM policy_chunks pc
                JOIN policy_documents pd ON pc.document_id = pd.id
                WHERE pc.content ILIKE %s OR pc.section_title ILIKE %s
                LIMIT 5;
                """,
                (search_pattern, search_pattern)
            )
            for row in cur.fetchall():
                keyword_results.append({
                    "id": row[0],
                    "doc_title": row[1],
                    "doc_url": row[2],
                    "section": row[3],
                    "content": row[4],
                    "score": 0.5 # Default score for keyword matches
                })
        except Exception as ek:
            print(f"Keyword search failed: {ek}")
            conn.rollback()

        cur.close()
        conn.close()
    except Exception as e:
        print(f"Database connection failed in RAG query: {e}")

    # 3. Hybrid Merge (Reciprocal Rank Fusion or Simple Score Addition)
    combined = {}
    for vr in vector_results:
        combined[vr["id"]] = vr

    for kr in keyword_results:
        if kr["id"] in combined:
            # Boost score if found in both
            combined[kr["id"]]["score"] = min(1.0, combined[kr["id"]]["score"] + 0.2)
        else:
            combined[kr["id"]] = kr

    # Sort results by score descending
    sorted_chunks = sorted(combined.values(), key=lambda x: x["score"], reverse=True)

    # 4. Generate Answer and Citation
    if not sorted_chunks:
        return {
            "answer": "No relevant policy documents or guidelines were found answering this question.",
            "confidence": 0.0,
            "sources": [],
            "requires_human_review": True
        }

    top_match = sorted_chunks[0]
    confidence = top_match["score"]
    
    # Check if confidence meets safety boundary (e.g. 0.6)
    requires_review = confidence < 0.6

    # Grounded response using retrieved clause
    answer_text = (
        f"According to the PM Internship Scheme policy guidelines, specifically the section "
        f"'{top_match['section']}' in '{top_match['doc_title']}':\n\n"
        f"\"{top_match['content']}\""
    )

    sources = []
    for chunk in sorted_chunks[:3]:
        sources.append({
            "document": chunk["doc_title"],
            "section": chunk["section"],
            "url": chunk["doc_url"],
            "match_score": chunk["score"]
        })

    return {
        "answer": answer_text,
        "confidence": confidence,
        "sources": sources,
        "requires_human_review": requires_review
    }
