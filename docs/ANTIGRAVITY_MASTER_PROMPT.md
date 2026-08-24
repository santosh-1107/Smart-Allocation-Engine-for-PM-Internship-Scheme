# Antigravity Master Build Prompt

You are the lead engineer for an SIH 2026 production-grade prototype named "Smart Allocation Engine for PM Internship Scheme".

Read every file in this repository before changing code, especially:
- `docs/PROJECT_SCOPE.md`
- `docs/ARCHITECTURE.md`
- `docs/SECURITY.md`
- `docs/EDGE_CASES.md`
- `docs/API_CONTRACTS.md`
- `docs/DATABASE.md`
- `docs/DEMO_FLOW.md`

## Goal

Build a working end-to-end prototype, not a collection of mock screens.

The system must solve a real allocation problem:
students have preferences and eligibility, companies have requirements and seat capacities, government policy has fairness and budget constraints, and administrators need to simulate, review, approve, explain and audit allocation decisions.

## Non-negotiable architecture

Frontend:
- Next.js
- TypeScript
- TailwindCSS
- accessible responsive UI

Core backend:
- Java 21
- Spring Boot
- PostgreSQL
- Spring Security
- modular monolith

Allocation:
- Python
- FastAPI
- OR-Tools
- student-proposing deferred acceptance as the base matching algorithm
- policy and budget reconciliation as a constraint layer

ML:
- FastAPI
- sentence-transformers for skill compatibility
- XGBoost for dropout risk
- bounded confidence
- ML never decides allocation

RAG:
- FastAPI
- pgvector
- hybrid keyword + vector retrieval
- section-aware policy chunks
- mandatory citations
- low-confidence human escalation

Infrastructure:
- Redis
- RabbitMQ
- Docker Compose
- OpenTelemetry-ready logging

## Build order

1. Inspect repository and preserve the intended architecture.
2. Create database schema and migrations.
3. Implement Spring Boot domain modules and REST APIs.
4. Implement seed data.
5. Implement allocation engine with deterministic tests.
6. Implement ML endpoints with deterministic fallback behavior.
7. Implement simulation and approval workflow.
8. Implement explanation and counterfactual services.
9. Implement waitlist and incremental reallocation.
10. Implement fairness metrics.
11. Implement audit hash chain and verification.
12. Implement frontend role-based portals.
13. Connect all flows end to end.
14. Add tests.
15. Add Docker support.
16. Add demo seed scenario.
17. Run the application and fix integration issues.

## Allocation requirements

Implement these hard constraints:
- eligibility
- listing capacity
- confirmed budget ceiling
- policy quota minimums
- one accepted allocation per student

Implement priority order:
1. hard constraints
2. stable matching
3. preference satisfaction
4. fairness
5. compatibility
6. seat utilization

Use deterministic tie-breaking and store the tie-break seed.

Do not use a single weighted score as the allocator.

## Explainability requirements

For every allocation result store enough information to answer:
- what was assigned
- preference rank
- why the top choice was unavailable
- capacity effect
- skill matches
- skill gaps
- policy/quota effect
- eligibility checks
- run id
- constraint snapshot

Student view must use plain language.

Admin view must expose the constraint trace.

## Counterfactual requirement

Implement:
`GET /api/v1/allocation/counterfactual`

Given a synthetic student and one changed input, determine whether the outcome changes.

Examples:
- add one missing skill
- change preference rank
- change location preference

The response must explain the causal change using actual solver inputs, not an LLM guess.

## Fairness

Dashboard:
- allocation rate parity
- preference satisfaction parity
- repeat-unallocated count
- company concentration
- regional distribution

Allow simulation comparison before commit.

Add an automated fairness red-team scenario suite using synthetic adversarial profiles.

## Inclusion

Implement:
- CSC operator role
- delegated student actions
- `acting_on_behalf_of` audit field
- pairwise preference elicitation
- vernacular-ready notification templates
- SMS adapter interface
- WCAG 2.1 AA-friendly UI patterns

## Operational resilience

Implement interfaces and demo mocks for:
- Aadhaar/eKYC degraded mode
- budget tranche availability
- MCoC calendar freeze
- deadline waiting room
- notification retry and dead-letter state
- company non-fulfillment
- cross-scheme duplicate-benefit flag

Do not fake live government integrations.

## Audit

Use an append-only audit table plus hash chaining:
`SHA256(canonical_json(entry) + previous_hash)`

Implement:
- chain verification endpoint
- tamper simulation test
- immutable allocation input snapshot
- override justification
- high-risk override second approval

Do not add blockchain.

## UI

Create separate experiences:

Student:
- dashboard
- profile
- internship catalogue
- pairwise preference flow
- allocation result
- explanation
- counterfactual replay

Company:
- dashboard
- create listing
- applications
- capacity management
- joining confirmation
- reliability metrics

Admin:
- national dashboard
- allocation cycle
- simulation sandbox
- side-by-side scenario comparison
- fairness dashboard
- exception queue
- allocation approval
- audit viewer
- RAG policy assistant
- shadow-mode comparison

## Demo data

Seed:
- 1000 students
- 100 companies
- 300 listings
- multiple districts
- aspirational-district flags
- multiple categories
- realistic skills
- varied preference patterns
- some high dropout-risk profiles
- enough capacity conflicts to make the simulation interesting

Also provide a smaller deterministic dataset for unit tests.

## Engineering rules

- No hardcoded fake allocation results in UI.
- No fake metrics.
- No random results without a stored seed.
- No silent fallback.
- No hidden admin override.
- No direct database writes from frontend.
- Use DTOs.
- Validate every request.
- Use migrations.
- Add indexes for allocation lookups.
- Add idempotency for allocation jobs.
- Add optimistic locking for preference versions.
- Use structured errors.
- Add health endpoints.
- Keep secrets in environment variables.
- Add unit tests for solver behavior.
- Add integration tests for allocation approval and reallocation.
- Add frontend loading, empty, error and success states.

## Definition of done

The following flow must work locally:

student seed data
→ preferences
→ company listings
→ eligibility
→ ML compatibility
→ simulation
→ fairness metrics
→ budget validation
→ admin approval
→ allocation commit
→ student explanation
→ counterfactual replay
→ company withdrawal
→ incremental reallocation
→ notification event
→ audit verification.

Before finishing, run tests and fix failures. Update README with exact run commands.
