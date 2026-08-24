# Security Model

## Roles

- STUDENT
- COMPANY_RECRUITER
- CSC_OPERATOR
- REGIONAL_ADMIN
- NATIONAL_ADMIN
- AUDITOR
- GRIEVANCE_OFFICER

## Controls

- short-lived access tokens
- refresh-token rotation
- MFA for privileged roles
- RBAC and least privilege
- field-level encryption for sensitive identifiers
- PII masking
- rate limiting
- upload validation and malware scanning adapter
- idempotency keys for allocation jobs
- optimistic locking for preferences
- single active allocation run per cycle
- mandatory justification for overrides
- four-eyes approval for high-severity overrides

## CSC delegated access

CSC operators act on behalf of students using their own authenticated identity.

Every delegated action stores:
- operator id
- student id
- action
- timestamp
- source
- acting_on_behalf_of flag
- request id

## Audit integrity

Audit entries form a hash chain:

`current_hash = SHA256(canonical_entry + previous_hash)`

A scheduled verifier detects broken chains. Store and expose the latest chain head for auditor verification.

Do not describe this as blockchain.
