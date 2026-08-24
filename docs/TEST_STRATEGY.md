# Test Strategy

## Allocation engine

- eligibility hard constraint
- capacity constraint
- budget ceiling
- quota floor
- one allocation per student
- stable matching property
- deterministic tie-breaking
- reproducibility from seed
- timeout behavior
- incremental reallocation

## Backend

- validation tests
- authorization tests
- transaction rollback tests
- optimistic locking
- allocation approval state machine
- audit hash verification
- exception resolution

## ML

- skill normalization examples
- score bounds
- confidence bounds
- low-confidence behavior
- model drift monitoring contract

## RAG

- retrieval relevance
- citation presence
- unsupported-answer refusal
- role access

## Frontend

- accessibility
- loading states
- empty states
- API error states
- role separation
- simulation comparison
- counterfactual replay
