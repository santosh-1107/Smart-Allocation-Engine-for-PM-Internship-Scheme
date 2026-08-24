# Production Architecture

## Deployment shape

Use a modular monolith for core transactional workflows. Keep compute-heavy or runtime-specific workloads separate.

### Core application

Spring Boot modules:

- auth
- students
- companies
- internships
- preferences
- eligibility
- allocations
- explanations
- waitlist
- notifications
- exceptions
- audit
- analytics
- policy

### Specialized services

Allocation engine:
- Python
- FastAPI
- OR-Tools
- deferred acceptance
- constraint reconciliation
- simulation and reallocation

ML service:
- Python
- FastAPI
- sentence-transformers
- XGBoost
- bounded confidence outputs

RAG service:
- FastAPI
- hybrid retrieval
- pgvector
- policy-document citations
- low-confidence human routing

## Infrastructure

PostgreSQL, Redis, RabbitMQ, Docker.

Production direction:
- Kubernetes
- government-empanelled cloud
- Prometheus/Grafana
- OpenTelemetry
- OpenSearch

## Data flow

1. Student submits or updates profile.
2. Backend validates and versions data.
3. Listing requirements are normalized.
4. ML service calculates compatibility signals.
5. Allocation engine creates a stable proposal.
6. Policy layer enforces hard constraints.
7. Simulation stores an immutable input snapshot.
8. Admin reviews metrics and exceptions.
9. Approved run is committed transactionally.
10. Explanation engine creates student and admin explanations.
11. Notifications are dispatched.
12. Rejection/dropout/withdrawal events trigger incremental reallocation.

## Hard constraints

- eligibility
- listing capacity
- confirmed budget ceiling
- legal/policy quota minimums
- one accepted allocation per student
- verified data requirements

## Optimization priority

1. Hard constraints
2. Stable matching
3. Preference satisfaction
4. Fairness
5. Skill-role compatibility
6. Seat utilization

## Safety boundary

The LLM never decides eligibility or allocation. The rule engine and allocation engine own those decisions.
