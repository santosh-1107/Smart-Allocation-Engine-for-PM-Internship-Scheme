CREATE TABLE IF NOT EXISTS students (
  id UUID PRIMARY KEY,
  full_name VARCHAR(160) NOT NULL,
  phone VARCHAR(30),
  preferred_language VARCHAR(30) DEFAULT 'en',
  district VARCHAR(120),
  aspirational_district BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS companies (
  id UUID PRIMARY KEY,
  legal_name VARCHAR(200) NOT NULL,
  cin VARCHAR(50),
  verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS internship_listings (
  id UUID PRIMARY KEY,
  company_id UUID NOT NULL REFERENCES companies(id),
  title VARCHAR(200) NOT NULL,
  location VARCHAR(160) NOT NULL,
  sector VARCHAR(120),
  capacity INTEGER NOT NULL CHECK (capacity >= 0),
  stipend_company_share NUMERIC(12,2) DEFAULT 0,
  status VARCHAR(40) NOT NULL DEFAULT 'DRAFT',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS preferences (
  id UUID PRIMARY KEY,
  student_id UUID NOT NULL REFERENCES students(id),
  cycle_id UUID NOT NULL,
  version INTEGER NOT NULL,
  preference_order JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(student_id, cycle_id, version)
);

CREATE TABLE IF NOT EXISTS allocation_runs (
  id UUID PRIMARY KEY,
  cycle_id UUID NOT NULL,
  status VARCHAR(40) NOT NULL,
  seed BIGINT NOT NULL,
  budget_ceiling NUMERIC(14,2) NOT NULL,
  input_snapshot JSONB NOT NULL,
  metrics JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS allocation_results (
  id UUID PRIMARY KEY,
  allocation_run_id UUID NOT NULL REFERENCES allocation_runs(id),
  student_id UUID NOT NULL REFERENCES students(id),
  listing_id UUID REFERENCES internship_listings(id),
  assigned_rank INTEGER,
  compatibility_score NUMERIC(8,5),
  explanation JSONB,
  status VARCHAR(40) NOT NULL DEFAULT 'PROPOSED',
  UNIQUE(allocation_run_id, student_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_student_accepted_allocation
ON allocation_results(student_id)
WHERE status = 'ACCEPTED';

CREATE TABLE IF NOT EXISTS waitlist_entries (
  id UUID PRIMARY KEY,
  student_id UUID NOT NULL REFERENCES students(id),
  listing_id UUID NOT NULL REFERENCES internship_listings(id),
  rank_position INTEGER NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'WAITING',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS exception_cases (
  id UUID PRIMARY KEY,
  case_type VARCHAR(80) NOT NULL,
  severity VARCHAR(30) NOT NULL,
  entity_id UUID,
  context JSONB NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
  resolution_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id UUID PRIMARY KEY,
  event_type VARCHAR(100) NOT NULL,
  actor_id VARCHAR(120),
  acting_on_behalf_of UUID,
  payload JSONB NOT NULL,
  previous_hash VARCHAR(128),
  current_hash VARCHAR(128) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_preferences_student_cycle ON preferences(student_id, cycle_id);
CREATE INDEX IF NOT EXISTS idx_allocation_results_run_student ON allocation_results(allocation_run_id, student_id);
CREATE INDEX IF NOT EXISTS idx_listings_company_status ON internship_listings(company_id, status);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
