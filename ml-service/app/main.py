import os
import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field
from contextlib import asynccontextmanager

# Try loading sentence-transformers
try:
    from sentence_transformers import SentenceTransformer, util
    # Load a lightweight, popular embedding model
    embed_model = SentenceTransformer('all-MiniLM-L6-v2')
    print("SentenceTransformer loaded successfully.")
except Exception as e:
    print(f"SentenceTransformer failed to load (offline fallback will be used): {e}")
    embed_model = None

# Try loading XGBoost
xgb_model = None
try:
    import xgboost as xgb
    from sklearn.datasets import make_classification
    print("XGBoost loaded successfully.")
except Exception as e:
    print(f"XGBoost or scikit-learn failed to load: {e}")

@asynccontextmanager
async def lifespan(app: FastAPI):
    global xgb_model
    # Train a small synthetic model on startup using XGBoost
    try:
        X, y = make_classification(
            n_samples=200, 
            n_features=4, 
            n_informative=3, 
            n_redundant=1, 
            random_state=42
        )
        # academic_percentage, stipend_company_share, skill_match_ratio, is_out_of_district
        xgb_model = xgb.XGBClassifier(n_estimators=10, max_depth=3, random_state=42)
        xgb_model.fit(X, y)
        print("XGBoost dropout-risk model trained and initialized.")
    except Exception as e:
        print(f"Could not train startup XGBoost model: {e}")
    yield

app = FastAPI(title="PMIS ML Service", version="0.1.0", lifespan=lifespan)

class CompatibilityRequest(BaseModel):
    student_skills: list[str]
    required_skills: list[str]

class CompatibilityResponse(BaseModel):
    score: float
    confidence: float
    matched_skills: list[str]
    missing_skills: list[str]

class DropoutRiskRequest(BaseModel):
    student_id: str
    academic_percentage: float = Field(default=75.0)
    stipend_company_share: float = Field(default=1500.0)
    skill_match_ratio: float = Field(default=0.5)
    is_out_of_district: bool = Field(default=false)

class DropoutRiskResponse(BaseModel):
    dropout_risk: float
    confidence: float
    factors: list[str]

@app.get("/health")
def health():
    return {"status": "ok", "service": "ml-service"}

@app.post("/v1/compatibility", response_model=CompatibilityResponse)
def compatibility(req: CompatibilityRequest):
    student = {x.strip().lower() for x in req.student_skills}
    required = {x.strip().lower() for x in req.required_skills}
    
    matched = sorted(student & required)
    missing = sorted(required - student)

    # Deterministic vector embedding match if model is available
    if embed_model is not None and req.student_skills and req.required_skills:
        try:
            # Join skills as sentences/queries
            student_text = ", ".join(req.student_skills)
            required_text = ", ".join(req.required_skills)
            emb1 = embed_model.encode(student_text, convert_to_tensor=True)
            emb2 = embed_model.encode(required_text, convert_to_tensor=True)
            cos_sim = util.cos_sim(emb1, emb2).item()
            score = max(0.0, min(1.0, cos_sim))
            confidence = 0.9
        except Exception as e:
            # Fallback to Jaccard
            score = len(matched) / len(required) if required else 0.0
            confidence = 0.5
    else:
        # Fallback to simple Jaccard match
        score = len(matched) / len(required) if required else 0.0
        confidence = 0.7 if required else 0.4

    return {
        "score": round(score, 4),
        "confidence": confidence,
        "matched_skills": matched,
        "missing_skills": missing,
    }

@app.post("/v1/dropout-risk", response_model=DropoutRiskResponse)
def dropout_risk(req: DropoutRiskRequest):
    # Features order: academic_percentage, stipend_company_share, skill_match_ratio, is_out_of_district
    features = np.array([[
        req.academic_percentage / 100.0,
        min(1.0, req.stipend_company_share / 10000.0),
        req.skill_match_ratio,
        1.0 if req.is_out_of_district else 0.0
    ]])

    factors = []
    if req.academic_percentage < 60.0:
        factors.append("Low academic percentage history")
    if req.stipend_company_share < 1000.0:
        factors.append("Below average stipend offering")
    if req.skill_match_ratio < 0.3:
        factors.append("High skill discrepancy")
    if req.is_out_of_district:
        factors.append("Relocation/out-of-district assignment stress")

    if xgb_model is not None:
        try:
            prob = float(xgb_model.predict_proba(features)[0][1])
            confidence = 0.85
        except Exception as e:
            # Heuristic fallback if model scoring fails
            prob = 0.5
            if req.is_out_of_district:
                prob += 0.15
            if req.skill_match_ratio < 0.3:
                prob += 0.2
            prob = min(0.9, max(0.1, prob))
            confidence = 0.5
    else:
        # Heuristic fallback
        prob = 0.3
        if req.is_out_of_district:
            prob += 0.2
        if req.academic_percentage < 60.0:
            prob += 0.15
        if req.skill_match_ratio < 0.3:
            prob += 0.15
        prob = min(0.95, prob)
        confidence = 0.6

    return {
        "dropout_risk": round(prob, 4),
        "confidence": confidence,
        "factors": factors if factors else ["No major risk factors detected"]
    }
