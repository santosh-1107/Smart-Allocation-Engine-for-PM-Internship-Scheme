# API Contracts

## Student

POST `/api/v1/students/profile`
GET `/api/v1/internships`
POST `/api/v1/preferences`
POST `/api/v1/preferences/pairwise`
GET `/api/v1/allocation/status`
GET `/api/v1/allocation/explanation`
GET `/api/v1/allocation/counterfactual`
POST `/api/v1/allocation/accept`
POST `/api/v1/allocation/reject`

## Company

POST `/api/v1/internships`
PUT `/api/v1/internships/{id}`
GET `/api/v1/internships/{id}/applications`
PUT `/api/v1/internships/{id}/capacity`
POST `/api/v1/internships/{id}/joining-confirmation`

## Admin

POST `/api/v1/allocation/simulate`
GET `/api/v1/allocation/simulate/{runId}`
POST `/api/v1/allocation/run`
POST `/api/v1/allocation/{runId}/approve`
POST `/api/v1/allocation/reallocate`
GET `/api/v1/fairness/dashboard`
GET `/api/v1/exceptions`
POST `/api/v1/exceptions/{id}/resolve`
GET `/api/v1/audit`
POST `/api/v1/audit/verify`

## RAG

POST `/api/v1/rag/query`

RAG answers must contain source document and clause references. Low-confidence retrieval routes to a human officer.
