from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "rag-service"}

def test_query_fallback():
    payload = {
        "question": "What is the stipend rate?",
        "role": "STUDENT"
    }
    response = client.post("/v1/query", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
    assert data["confidence"] == 0.0
    assert data["requires_human_review"] is True
