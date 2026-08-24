from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "service": "ml-service"}

def test_compatibility():
    payload = {
        "student_skills": ["Python", "SQL"],
        "required_skills": ["Python", "Java"]
    }
    response = client.post("/v1/compatibility", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "score" in data
    assert data["matched_skills"] == ["python"]

def test_dropout_risk():
    payload = {
        "student_id": "test-student-1",
        "academic_percentage": 75.0,
        "stipend_company_share": 1500.0,
        "skill_match_ratio": 0.5,
        "is_out_of_district": False
    }
    response = client.post("/v1/dropout-risk", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "dropout_risk" in data
    assert len(data["factors"]) > 0
