CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS student_profiles (
    student_id UUID PRIMARY KEY REFERENCES students(id) ON DELETE CASCADE,
    category VARCHAR(50) DEFAULT 'GENERAL',
    gender VARCHAR(30),
    dob DATE,
    ekyc_verified BOOLEAN DEFAULT FALSE,
    ekyc_failed_reason VARCHAR(255),
    acting_on_behalf_of VARCHAR(120),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS education (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    degree VARCHAR(100) NOT NULL,
    field_of_study VARCHAR(100),
    institution VARCHAR(200),
    percentage_or_cgpa NUMERIC(5,2),
    graduation_year INTEGER
);

CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS student_skills (
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, skill_id)
);

CREATE TABLE IF NOT EXISTS company_verifications (
    id UUID PRIMARY KEY,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    auditor_id VARCHAR(100),
    status VARCHAR(40) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS listing_requirements (
    listing_id UUID NOT NULL REFERENCES internship_listings(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (listing_id, skill_id)
);

CREATE TABLE IF NOT EXISTS preference_versions (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    preference_order JSONB NOT NULL,
    updated_by VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS eligibility_records (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES internship_listings(id) ON DELETE CASCADE,
    eligible BOOLEAN NOT NULL DEFAULT TRUE,
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS reallocation_events (
    id UUID PRIMARY KEY,
    cycle_id UUID NOT NULL,
    run_id UUID REFERENCES allocation_runs(id),
    student_id UUID NOT NULL REFERENCES students(id),
    previous_listing_id UUID REFERENCES internship_listings(id),
    new_listing_id UUID REFERENCES internship_listings(id),
    event_type VARCHAR(50) NOT NULL, -- WITHDRAWAL, REJECTION, NON_FULFILLMENT
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS joining_confirmations (
    id UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    listing_id UUID NOT NULL REFERENCES internship_listings(id) ON DELETE CASCADE,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    comments TEXT,
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS company_reliability_scores (
    company_id UUID PRIMARY KEY REFERENCES companies(id) ON DELETE CASCADE,
    onboarding_count INTEGER DEFAULT 0,
    withdrawal_count INTEGER DEFAULT 0,
    reliability_score NUMERIC(5,2) DEFAULT 100.0
);

CREATE TABLE IF NOT EXISTS feedback (
    id UUID PRIMARY KEY,
    student_id UUID REFERENCES students(id),
    company_id UUID REFERENCES companies(id),
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    comments TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    recipient_id VARCHAR(100) NOT NULL, -- phone number or student id
    channel VARCHAR(30) NOT NULL, -- SMS, EMAIL, IN_APP
    template_name VARCHAR(100),
    language VARCHAR(10) DEFAULT 'en',
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS admin_actions (
    id UUID PRIMARY KEY,
    action_type VARCHAR(100) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    target_id VARCHAR(100),
    justification TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS policy_documents (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS policy_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES policy_documents(id) ON DELETE CASCADE,
    section_title VARCHAR(200),
    content TEXT NOT NULL,
    embedding vector(384),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
