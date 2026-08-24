# ADR 004: ML Boundary

Decision: ML generates bounded signals only. It never directly allocates a student.

Signals:
- skill compatibility
- dropout risk
- demand forecast

Reason: Keep public allocation explainable, testable and policy-controlled.
