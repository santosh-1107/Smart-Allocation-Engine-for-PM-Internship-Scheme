from fastapi import FastAPI
from pydantic import BaseModel, Field
from app.solver.solver import solve_matching

app = FastAPI(title="PMIS Allocation Engine", version="0.1.0")

class SimulationRequest(BaseModel):
    cycle_id: str
    budget_ceiling: float = Field(gt=0)
    seed: int = 42
    students: list[dict]
    listings: list[dict]
    preferences: list[dict]

class SimulationResponse(BaseModel):
    run_id: str
    status: str
    allocations: list[dict]
    metrics: dict
    constraint_trace: dict

@app.get("/health")
def health():
    return {"status": "ok", "service": "allocation-engine"}

@app.post("/v1/simulate", response_model=SimulationResponse)
def simulate(request: SimulationRequest):
    # Run the CP-SAT stable matching solver
    result = solve_matching(
        students=request.students,
        listings=request.listings,
        preferences=request.preferences,
        budget_ceiling=request.budget_ceiling,
        seed=request.seed
    )

    return {
        "run_id": f"sim-{request.seed}",
        "status": "SIMULATED",
        "allocations": result["allocations"],
        "metrics": result["metrics"],
        "constraint_trace": result["constraint_trace"]
    }
