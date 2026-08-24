# PMIS Smart Allocation Engine

SIH 2026 project scaffold for a production-oriented Smart Allocation Engine for the PM Internship Scheme.

## Architecture

- `frontend/`: Next.js + TypeScript + Tailwind student, company and admin portals
- `backend/`: Spring Boot modular monolith for core transactional workflows
- `allocation-engine/`: Python + FastAPI + OR-Tools allocation solver
- `ml-service/`: Python + FastAPI for skill compatibility and dropout-risk scoring
- `rag-service/`: Python + FastAPI + pgvector-oriented policy assistant
- `infra/`: Docker Compose, PostgreSQL, Redis, RabbitMQ
- `docs/`: architecture, API contracts, data model, demo flow, implementation plan
- `scripts/`: local development and seed helpers

## Core product flow

Registration → eligibility → internship discovery → preference elicitation → scoring → constraint validation → simulation → human approval → allocation → explanation → acceptance → waitlist/reallocation → joining → completion → feedback → analytics.

## Important design decisions

1. Stable student-proposing deferred acceptance is the matching base.
2. OR-Tools handles policy, quota, budget and capacity reconciliation.
3. ML supplies bounded signals. ML never directly allocates seats.
4. Every allocation run is reproducible from its input snapshot.
5. Admin approval is mandatory before publishing an allocation.
6. Fairness is measured with explicit metrics.
7. Shadow mode is the proposed production rollout path.
8. Budget ceiling is a hard constraint.
9. CSC assisted submission is a first-class role.
10. Audit records use append-only storage plus a hash chain for tamper evidence.
11. Counterfactual explanation and live replay are the flagship demo feature.
12. SMS is treated as a primary critical-notification channel.

## SIH scope

Build the MVP and demo features first. Do not claim real Aadhaar, DBT, DigiLocker, UMANG or government integrations. Use mock adapters with clear interfaces until formal access exists.

## Run

See `docs/LOCAL_SETUP.md`.
