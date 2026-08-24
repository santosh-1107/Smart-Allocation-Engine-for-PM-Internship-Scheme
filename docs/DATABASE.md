# Database Model

Core tables:

- students
- student_profiles
- education
- skills
- student_skills
- companies
- company_verifications
- internship_listings
- listing_requirements
- preferences
- preference_versions
- applications
- eligibility_records
- allocation_runs
- allocation_inputs
- allocation_results
- waitlist_entries
- reallocation_events
- joining_confirmations
- company_reliability_scores
- feedback
- notifications
- exception_cases
- admin_actions
- audit_logs
- policy_documents
- policy_chunks

Important constraints:

- preference versions are append-only
- allocation result references allocation run and input snapshot
- only one accepted allocation per student
- allocation run has status: DRAFT, SIMULATING, READY_FOR_APPROVAL, APPROVED, COMMITTED, FAILED
- every audit row stores previous_hash and current_hash
