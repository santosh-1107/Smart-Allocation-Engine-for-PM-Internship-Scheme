# ADR 001: Modular Monolith

Decision: Keep core workflows inside a Spring Boot modular monolith. Separate allocation, ML and RAG services because their runtime and scaling characteristics differ.

Reason: Avoid microservice overhead while retaining clear boundaries.
