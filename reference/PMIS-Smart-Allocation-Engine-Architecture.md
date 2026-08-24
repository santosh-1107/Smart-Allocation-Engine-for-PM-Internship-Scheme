# Smart Allocation Engine for PM Internship Scheme (SIH25033)
### Production-Grade Solution Architecture

*Prepared for SIH 2026 — Department Level Screening prep*

---

> **⚠ Note on facts used below:** Web sources on PMIS conflict on some numbers (age band cited as both 18–30 and 21–24; stipend cited as both ₹5,000/month and ₹9,000/month across different rounds; internship duration cited as both 6–12 months). These are marked **[VERIFY]** wherever used. Everything else below (portal flow, preference-based matching, rounds, aspirational-district emphasis, scale ~1.18 lakh+ opportunities across 700+ districts) is drawn from public reporting and should still be spot-checked against `pminternship.mca.gov.in` before you present it as fact to judges — do not state unverified numbers with false confidence.

---

## 1. Problem Understanding

PMIS (Ministry of Corporate Affairs) places youth into paid internships with top companies nationwide. Confirmed facts from research:

- Portal: `pminternship.mca.gov.in`. Registration via mobile OTP → profile (eKYC, education, bank, skills, languages) → browse/apply to internships → preference submission (historically up to 3, later rounds up to 5) → selection.
- Scale: **1.18 lakh+ opportunities** across **36 states/UTs** and **735 districts** (Round 2 figures) — genuinely national scale, not a toy dataset problem.
- **The matching model has evolved across rounds** — this is the single most important fact for your pitch:
  - **Round 1:** students applied "blind" — the portal's algorithm alone decided company, city, and role. This caused a **role-mismatch/attrition problem** (students got roles they didn't expect).
  - **Round 3 (current, as of reporting):** companies post detailed listings (location, role, duration, qualifications, incentives); students browse and apply themselves; the portal still shortlists per company, but the student now makes the first move.
- Stipend: **[VERIFY]** ₹5,000/month total (₹4,500 govt + ₹500 company) cited in early-2025 sources; a later source (mid-2026) cites ₹9,000/month (₹8,100 govt DBT + ₹900 company) — this may reflect a scheme revision. State the current figure only after checking the portal directly before your presentation.
- Duration: **[VERIFY]** 12 months cited most consistently, "at least 6 months hands-on" in one source.
- No application fee. Completion certificate issued.

**What this tells you architecturally:** the government has already *tried* a pure algorithmic black-box (Round 1) and moved away from it because it produced bad matches and low trust. **This is your single strongest pitch angle** — you're not proposing something untested; you're fixing a documented, publicly-reported failure mode of the exact system you're targeting.

### 1.1 Existing functionality (do not rebuild)
- Registration, OTP auth, eKYC, profile completion
- Internship browsing and listing (Round 3 style)
- Preference submission
- Company-side listing creation
- Stipend disbursement via DBT
- Certificate issuance

### 1.2 Existing limitations (confirmed or strongly implied by sources)
- Round 1's algorithmic allocation caused **expectation mismatch and attrition** — no visibility into *why* an allocation happened
- No evidence of a **simulation/dry-run environment** for administrators before committing an allocation
- No evidence of a **formal fairness model** beyond stated affirmative-action intent (aspirational districts, social categories) — no measurable fairness metric surfaced publicly
- No evidence of an **explanation layer** for students on why they got (or didn't get) a match
- No evidence of a **waitlist/reallocation engine** for dropouts, company capacity changes, or rejections
- Manual/undocumented process for what happens when a company withdraws a listing or reduces capacity mid-cycle

### 1.3 Our proposed improvements (built on top of existing functionality)
- A proper multi-objective optimization allocation engine replacing "blind algorithmic decision" (Round 1's failure mode) *and* enhancing "self-selected browsing" (Round 3) with a matching layer underneath it
- Explainability engine for every allocation decision
- Measurable, enforced fairness constraints (not just stated intent)
- Simulation/dry-run environment for admins
- Automated waitlist + reallocation workflow for dropouts/capacity changes
- Full audit trail and human-override capability at every automated decision point

### 1.4 Completely new functionality (not implied to exist today)
- Allocation Explanation Engine (per-student, per-admin views)
- Fairness dashboard with measurable metrics
- Admin simulation/what-if sandbox
- Exception queue + escalation workflow
- RAG-based scheme-guideline assistant (justified separately in §12 — not a demo chatbot)

---

## 2. Stakeholder-wise Problem Analysis

### 2.1 Students
| Pain point | Real consequence |
|---|---|
| Skill/role mismatch (Round 1 documented failure) | Attrition, wasted internship slot, wasted stipend |
| No explanation for allocation outcome | Loss of trust, repeated grievance filings |
| No accessibility-aware matching | Students with mobility/sensory needs placed in unsuitable locations/roles |
| Rural/aspirational-district students lack visibility into which companies value their profile | Under-application to real opportunities, self-selection bias |
| No mechanism to update preferences after a life event (e.g. family relocation) before allocation runs | Locked into bad-fit preference set |
| Multiple offers / no offer with no guided next step | Confusion, drop-off from the scheme entirely |
| Duplicate accounts (accidental or fraudulent) | Skews demand data, unfair seat consumption |

### 2.2 Companies
| Pain point | Real consequence |
|---|---|
| Candidate quality variance across regions | Companies concentrate postings in Tier-1 cities, defeating the scheme's inclusion goal |
| Requirement changes mid-cycle (skills, seat count) | Stale postings, wasted admin cycles reconciling |
| Candidate dropout after allocation | Unfilled seats late in the cycle with no fast backfill |
| Fraudulent/low-quality listings (any open platform risk) | Wasted student applications, reputational risk to scheme |
| No visibility into *why* they got certain candidates | Reduced trust in scheme, lower repeat participation |

### 2.3 Government / Admin
| Pain point | Real consequence |
|---|---|
| Lakhs of applicants vs. limited admin bandwidth | Manual review doesn't scale |
| No measurable fairness proof | Political/audit risk — affirmative-action intent (aspirational districts, categories) with no evidence it's being met |
| No dry-run before committing an allocation | Bad allocation runs are expensive to unwind (stipend already triggered, students already notified) |
| No structured exception handling | Ad hoc manual overrides with no audit trail |
| Regional imbalance monitoring | Hard to prove/disprove urban bias in outcomes without dedicated tooling |

### 2.4 Other stakeholders
- **Nodal training partners / skilling agencies** (for skill-gap-flagged candidates)
- **State-level implementation cells** (regional quota administration)
- **Grievance redressal officers**
- **Auditors / CAG-type oversight** (public money via DBT — needs strong audit trail)

---

## 3. Complete End-to-End Workflow

```
Registration → eKYC/Profile Verification → Eligibility Validation →
Internship Discovery (browse, Round-3 style) → Preference Submission
(ranked, up to N) → Company Requirement Ingestion → Candidate Scoring
(skill/eligibility compatibility) → Constraint Validation (capacity,
quotas, policy) → Allocation Optimization (batch, scheduled) →
Conflict Resolution → Simulation Review (admin) → Approval →
Allocation Result Published → Explanation Generated →
Acceptance/Rejection Window → Waitlist Activation (if rejected/no seat)
→ Reallocation Pass → Internship Joining → Periodic Check-ins →
Completion → Feedback → Analytics Feed-back into next cycle
```

### Automated decision points — detail

**A. Eligibility Validation**
- Input: profile data, education docs, age, income declaration
- Processing: rule engine against scheme criteria
- Decision: eligible / ineligible / needs-manual-review
- Output: eligibility flag on profile
- Failure condition: missing/conflicting data → routed to manual review queue, never silently rejected
- Human override: admin can approve with documented reason (audit-logged)

**B. Candidate Scoring**
- Input: skills, education, preferences, location, accessibility flags
- Processing: skill-compatibility model + rule-based eligibility filters
- Decision: compatibility score per (student, internship) pair — **used as one input to optimization, never the sole allocator**
- Output: scored candidate-internship matrix
- Failure condition: sparse/missing skill data → fallback to education-based compatibility, flagged low-confidence
- Human override: admin can manually adjust weight profile before a run (via simulation)

**C. Allocation Optimization**
- Input: scored matrix + capacity + fairness constraints + policy quotas
- Processing: constrained optimization solver (see §4)
- Decision: proposed allocation set
- Output: allocation result set (not committed until approved)
- Failure condition: solver infeasible/timeout on huge batch → fallback to relaxed-constraint pass with flagged relaxations shown to admin
- Human override: admin must explicitly approve before publish (see §14, §15)

**D. Reallocation (post-dropout/capacity change)**
- Input: updated capacity/dropout event
- Processing: incremental re-solve on affected subset only (not full re-run — see §13 scalability)
- Decision: waitlist promotion or re-match
- Output: updated allocation for affected students only
- Failure condition: no eligible waitlist candidate → seat marked unfilled, surfaced to admin dashboard
- Human override: admin can manually assign from exception queue

---

## 4. Allocation Engine Design

### 4.1 Why not a simple weighted score
A pure weighted-sum score ("skill×0.4 + location×0.3 + preference×0.3, pick highest") is what most student teams present and call "AI." It fails because:
- It doesn't guarantee **stability** (no incentive for anyone to game preferences)
- It doesn't handle **capacity constraints properly** — greedy assignment by score creates lopsided outcomes where popular companies get flooded and good-but-not-top candidates get nothing
- It has **no mechanism to enforce fairness quotas** without hacky post-processing
- It's **not explainable** in a principled way — "your score was 0.71" means nothing to a student

### 4.2 Approach comparison

| Approach | Fit for this problem | Verdict |
|---|---|---|
| Rule-based allocation | Too rigid for multi-attribute tradeoffs at scale | Reject as sole method — useful only for hard eligibility gates |
| Weighted scoring | Simple, but no stability/fairness guarantees, easily gamed | Reject as sole method — usable as a *feature*, not the *algorithm* |
| Simple bipartite matching (max cardinality) | Doesn't respect preference ranking or fairness | Reject alone |
| **Stable matching (Gale–Shapley, hospital/residents variant — many-to-one)** | Directly models "many students, many-seat companies, ranked preferences on both sides" — this is *literally* the Hospital/Residents problem | **Strong fit** |
| Constraint Satisfaction (CSP) | Good for hard constraints (eligibility, quotas) but weak at optimizing soft preferences simultaneously | Use as a **layer**, not standalone |
| Integer Linear Programming (ILP) / Mixed-Integer Programming (MIP) | Can encode capacity, quotas, and a weighted multi-objective function exactly; solvable at this scale with modern solvers (CBC, OR-Tools, Gurobi/academic license) for batches in the tens/hundreds of thousands with proper decomposition | **Strong fit — this is your core engine** |
| Min-cost max-flow | Elegant for pure capacity+cost problems, but doesn't naturally encode ranked stable-matching semantics or multiple simultaneous soft objectives as cleanly as MIP | Consider as a **faster approximate pre-solve** before MIP refinement |
| Multi-objective optimization | Necessary — we have competing objectives (preference satisfaction vs. fairness vs. utilization) | **Required as the framing**, implemented via MIP with weighted/lexicographic objectives |
| Optimization + ML hybrid | ML doesn't allocate seats — it **feeds features into the optimizer** (skill compatibility scores, dropout-risk scores) | **Correct use of ML** — see §10 |

### 4.3 Recommended engine: **Deferred-Acceptance Stable Matching (many-to-one) as the base algorithm, refined by a Mixed-Integer Programming layer for fairness/policy constraints.**

**Why this combination, precisely:**
- Deferred acceptance (Gale-Shapley, hospital-residents variant) guarantees a **stable outcome** — no student-company pair would both prefer to defect from their assigned match, which is the actual game-theoretic property that prevents gaming and complaints ("but I preferred X and X had room" complaints become provably impossible under a stable matching w.r.t. the stated preference lists).
- Student-proposing deferred acceptance is **strategy-proof for students** — students have no incentive to misrepresent preferences, which matters enormously for a public system where trust is the whole point.
- Pure deferred acceptance doesn't natively encode fairness quotas (aspirational-district reservation, category quotas) — so we add these as a **quota-constrained variant** (reserve-based deferred acceptance, as used in real-world school-choice systems like Boston/NYC) OR post-process via MIP to enforce quota satisfaction while minimizing deviation from the stable outcome.
- MIP is used specifically for the **quota/fairness reconciliation pass** and for **capacity-constrained tie-breaking**, not to replace the stable-matching core — this keeps the system explainable (each layer has one clear job) instead of one opaque objective function nobody can reason about.

### 4.4 Objective function (conceptual, multi-objective, lexicographic priority — not one blended score)

**Priority order (lexicographic — each level optimized only after the previous is satisfied, to avoid one factor silently dominating):**

1. **Hard constraints (must never be violated):** eligibility, capacity, legal quota minimums
2. **Stability:** no blocking pairs (guaranteed by deferred acceptance)
3. **Preference satisfaction:** maximize sum of (inverse rank of assigned choice) across students
4. **Fairness:** minimize variance in allocation-rate across regions/categories, subject to not violating merit within quota groups
5. **Skill-role compatibility:** maximize aggregate compatibility score, as tiebreaker within already-stable, already-fair outcomes
6. **Seat utilization:** minimize unfilled seats, as final tiebreaker

*Rejected factors:* we deliberately do **not** independently optimize "minimize unnecessary relocation" as a top-level objective — location preference already enters via the student's own ranked preference list, so a separate relocation-penalty term would double-count it and distort the stable-matching guarantees. Only add it if post-launch data shows students ranking location inconsistently with actual relocation tolerance.

---

## 5. Fairness Model

### 5.1 Concrete unfair-outcome risks (from stakeholder analysis)
- Rural/aspirational-district students under-allocated despite quota intent
- Weak-profile students perpetually unallocated across cycles (no "priority boost" for repeat non-selection)
- Popular companies flooded with top-decile scorers, starving mid-tier companies of good matches
- Accessibility-flagged students silently deprioritized by a naive score function
- Ranking bias: students who don't understand the system may rank conservatively/poorly, systematically disadvantaging low-digital-literacy applicants — **this is a real, documented-adjacent risk given PMIS explicitly targets youth "outside Tier-1 colleges"**

### 5.2 Measurable fairness metrics (must be dashboarded, not just claimed)
- **Allocation rate parity**: allocation rate by district-tier, gender, category — flag any group >X percentage points below the national average
- **Preference satisfaction parity**: average rank-of-assigned-choice by group — flag systemic gaps
- **Repeat-unallocated tracking**: students unallocated for 2+ consecutive cycles get an automatic priority boost (bounded, so it doesn't override merit indefinitely — capped boost)
- **Company concentration index**: Gini-style measure of how concentrated top-scorers are in a handful of companies — feeds a soft rebalancing constraint in the MIP layer

### 5.3 How fairness is enforced without destroying merit
- Quotas are implemented as **reserved-seat minimums within the deferred-acceptance/MIP layer**, not as post-hoc reshuffling of an already-computed "merit list" — this is the same mechanism used in real reservation-compliant matching systems, and it preserves within-quota merit ordering while guaranteeing the quota floor.
- The repeat-unallocated boost is **capped and logged** — never silently overrides a genuinely ineligible or clearly incompatible match.

---

## 6. Explainability Model — Allocation Explanation Engine

For every student, generate a structured explanation (not a black-box score):

```
Assigned: [Company X, Role Y, Location Z]
Your preference rank for this: #2 of 5
Why not your #1 choice (Company A)?
 → Company A had 40 seats; 312 eligible students ranked it #1;
   your skill-compatibility score placed you below the cutoff
   for Company A's available seats after quota reservations
   were applied.
Skills matched: [Excel, Communication] (2 of 3 required)
Skill gap: [Basic SQL] — flagged for pre-joining skilling recommendation
Eligibility conditions satisfied: [Age, Education, Category]
Was capacity a factor? Yes — Company A's seats were fully subscribed
   by higher-ranked-preference students before quota adjustment.
Was a policy constraint a factor? Yes — aspirational-district
   quota reserved 15% of Company A's seats; you were not in
   that reserved pool for this listing.
```

- **Student view:** plain language, no raw scores or internal weights exposed
- **Admin view:** same explanation plus raw compatibility scores, constraint trace, and which MIP constraint bound the outcome (for audit/appeal handling)
- **What is deliberately hidden:** exact internal weight coefficients (prevents gaming via preference-list manipulation), other students' individual data

---

## 7. AI/ML Components — only where they add real value

| Component | Problem solved | Data needed | Model | Why this model | Evaluation | Low-confidence behavior | If wrong |
|---|---|---|---|---|---|---|---|
| Skill normalization / extraction | Students self-report skills inconsistently ("MS Excel" vs "Excel" vs "spreadsheets") | Free-text skill entries, resume text if uploaded | Lightweight NER + embedding-based synonym clustering (not a huge LLM) | Fast, deterministic-enough, cheap to run at scale, doesn't need GPU inference at request time | Precision/recall against a curated skill taxonomy | Falls back to raw string match, flagged for admin skill-taxonomy review | Mismatch surfaces as lower compatibility score, not a hard rejection — self-correcting |
| Skill–role compatibility scoring | Feeds one input into the optimizer's objective | Normalized skills + role requirements | Embedding similarity (sentence-transformers) + calibrated logistic layer | Interpretable, cheap, doesn't need per-student fine-tuning | Correlation with post-hoc supervisor feedback scores over time | Score returned with confidence interval; low-confidence pairs get manual-review flag before final approval | Bounded impact — it's one signal among several in the MIP, not the sole allocator |
| Dropout-risk prediction | Predict which allocated students are likely to not join/drop out early, to pre-emptively size the waitlist buffer | Historical join/drop data (distance from home, stipend timeliness, sector, prior cycle behavior) | Gradient-boosted trees (XGBoost) — tabular, interpretable via SHAP | Best fit for structured tabular data at this scale; explainable | AUC against held-out historical cohort | High-risk flag does NOT block allocation — only sizes the waitlist buffer more generously | Worst case: slightly oversized waitlist, no harm to any real student |
| Seat-demand forecasting | Helps admins/companies anticipate demand imbalance before a cycle opens | Historical applications by sector/region | Simple time-series (Prophet/ARIMA) or even a moving-average baseline | Don't over-engineer this — a strong baseline beats an LSTM at this data volume | MAPE against holdout cycles | Forecast shown with confidence band, never auto-acts | Purely advisory dashboard — no downstream automated action depends on it |

**Explicitly rejected ML components:**
- **Internship recommendation via deep personalization models** — the browsing/preference system (Round 3 style) already lets students self-select; a heavy recommender adds complexity without solving a documented problem. A simple filter/sort by compatibility score is sufficient — reject the urge to add a "for you" feed.
- **Fraud detection via deep learning** — at this stage, rule-based + anomaly-flagging (duplicate Aadhaar/bank hash matches, impossible application velocity) is sufficient and far more explainable/auditable for a government context than an opaque model. Revisit only if fraud volume in production data justifies it.

---

## 8. GenAI / RAG — where it's legitimate, and where we reject it

**We explicitly reject** a general-purpose "ask me anything" chatbot bolted on for demo appeal. That is exactly the kind of feature the prompt told us to cut.

**Legitimate use case: Policy/Guideline Query Assistant for Admins and Grievance Officers** (not students by default — see below)

Why admins, not primarily students: the highest-value, lowest-hallucination-risk use is an internal tool where officials need fast, cited answers against scheme guideline PDFs, eligibility circulars, and quota policy documents during grievance handling and audit prep — a real, bounded, high-stakes-but-verifiable use.

If extended to students, it must be scoped **only** to already-published, static scheme FAQ/guideline content — never to give personalized eligibility rulings (that stays a hard rule-engine decision, not a generative one).

**RAG design:**
- **Data sources:** official scheme guideline PDFs, eligibility circulars, quota policy documents, FAQ — all static, version-controlled government documents
- **Ingestion:** scheduled re-ingestion on document version change only (not continuous crawling)
- **Chunking:** section-aware chunking (by clause/heading), not naive fixed-token windows — policy documents have legal structure that matters
- **Embeddings:** a standard sentence-embedding model, self-hosted for data-sovereignty reasons (government data should not leave approved infrastructure)
- **Vector DB:** pgvector (co-located with existing PostgreSQL — avoids introducing a whole new system for a bounded-scale document set; see §19)
- **Retrieval:** hybrid (BM25 keyword + vector similarity) — policy text has exact-term significance (e.g. "aspirational district") that pure semantic search can blur
- **Reranking:** lightweight cross-encoder rerank on top-20 candidates before generation
- **Generation:** answer restricted to retrieved context only, with **mandatory inline citation** to the source clause/document
- **Hallucination prevention:** if retrieval confidence is below threshold, the system returns "I don't have a confident answer — routing to a human officer" rather than generating an unsupported answer. This is non-negotiable for a government-facing tool.
- **Evaluation:** a curated set of Q/A pairs against real guideline text, graded for citation accuracy, run before every document-set update
- **Access control:** role-scoped — admin/grievance-officer tier sees full guideline corpus; student-facing tier (if ever enabled) sees only the public FAQ subset

---

## 9. Edge Cases — system behavior defined

| Edge case | Defined behavior |
|---|---|
| Student eligible for zero internships | Routed to a "no current match" state with skill-gap explanation + recommended skilling resources; re-evaluated automatically next cycle |
| Student eligible for hundreds | Ranked by compatibility, capped list shown, no different from normal flow — no special handling needed, just pagination |
| No company has enough seats for demand | Surfaced on admin dashboard pre-run (simulation stage) as a supply-gap alert before commit |
| More students than seats overall | Deferred-acceptance naturally leaves excess students unmatched → routed to waitlist, not silently dropped |
| More seats than students | Unfilled-seat report to admin; may trigger company outreach workflow (out of allocation-engine scope, but logged) |
| Two students tied for one seat | Deterministic tiebreak: documented secondary criteria (e.g. earlier application timestamp, then random seed logged for audit) — never silent/undocumented tiebreaking |
| Student/company preference conflict | Resolved by stable-matching property itself — no blocking pair can exist in the output by construction |
| Student changes preference post-allocation | Allowed only before acceptance window closes; treated as a new preference set, triggers incremental re-solve for that student only |
| Company changes requirements mid-cycle | New requirements apply to *next* unresolved matching pass; already-committed allocations are not silently retro-invalidated — requires explicit admin action if a rollback is needed |
| Company withdraws listing | Affected already-allocated students automatically enter priority reallocation queue |
| Company reduces/increases capacity | Triggers incremental re-solve on affected subset (see §13) |
| Student rejects allocation | Seat released to waitlist immediately; student's remaining preferences are reconsidered in next incremental pass |
| Student doesn't respond within window | Defaults to "no response = decline" after a defined SLA, documented and communicated in advance — never silently held open indefinitely |
| Student accepts multiple offers (shouldn't be possible by design, but data errors happen) | Hard uniqueness constraint at the database layer; if detected, both are frozen and routed to exception queue for manual resolution |
| Duplicate student accounts | Deduplication check at registration (Aadhaar-hash based) blocks a second account; existing duplicates flagged by nightly batch job |
| Duplicate/fraudulent company listings | Company verification gate before listing goes live (see §12 security); anomaly flags for suspiciously high seat counts or mismatched CIN |
| Invalid/missing certificates | Profile marked "pending verification," excluded from allocation runs until resolved — never allocated on unverified data |
| API/DB/allocation-engine failure mid-run | Allocation runs are transactional and idempotent (see §13) — a failed run rolls back cleanly, nothing partially commits |
| Algorithm timeout on huge batch | Batch decomposition by region/sector (see §13) prevents single-run timeout; partial results never auto-publish without admin approval |
| Concurrent preference updates | Optimistic locking on preference records; last-write-wins with versioning, old version audit-logged |
| Concurrent allocation runs | Single active-run lock per allocation cycle — a second run request is queued, not run in parallel against the same dataset |
| Administrator override / incorrect override | Every override requires a documented reason field; fully audit-logged and reversible by a senior admin role, never silently applied |
| Data privacy issue | PII fields are access-scoped and masked in all non-admin views (see §12) |
| Model drift (ML components) | Scheduled model performance monitoring per cycle; automatic alert if compatibility-score correlation with outcomes drops below threshold |

---

## 10. Production Architecture

### 10.1 Modular monolith, not microservices — and here's why
Given SIH team size, timeline, and even a realistic first-year production rollout, **microservices are the wrong default**. The system doesn't have wildly different scaling profiles per component at launch, and microservices add operational overhead (service mesh, distributed tracing, more failure modes) that a small government-vendor team will struggle to run reliably. 

**Recommendation: a well-modularized monolith with clear internal service boundaries**, split into **separate deployable services only where scaling or failure isolation genuinely differs**:
- **Core application (modular monolith):** auth, profile, internship listing, preference, eligibility, notification, audit — these share transactional data and don't need independent scaling
- **Separate service #1 — Allocation Engine:** long-running, CPU/solver-heavy batch job, fundamentally different resource profile (needs to scale compute independently, run async, not block the request path)
- **Separate service #2 — ML/scoring services:** Python-based (skill scoring, dropout risk), different runtime from the main backend, benefits from independent deployment/versioning
- **Separate service #3 — RAG/GenAI service:** different infra needs (vector DB, embedding inference), optional component, should not be coupled to core uptime

This is **event-driven between the monolith and the two/three specialized services** (via message queue), but **not** a full microservices mesh internally. Justify this explicitly to judges — it shows architectural maturity, not just "we used microservices because it sounds advanced."

### 10.2 Components
- **API Gateway:** single entry point, rate limiting, auth token validation
- **Auth service:** OTP + password, MFA for admin roles
- **Core modular monolith:** as above
- **Allocation Engine service:** async, queue-triggered, MIP solver (OR-Tools)
- **ML/scoring service:** Python/FastAPI, XGBoost + embedding models
- **RAG service:** optional, pgvector-backed
- **Notification service:** SMS/email/push, retry-capable, queue-backed
- **Audit service:** append-only log store, separate from operational DB for tamper-resistance
- **Analytics service:** read replica + aggregation jobs, isolated from transactional load

### 10.3 Cross-cutting
- **Message queue:** for allocation triggers, notification dispatch, reallocation events
- **Cache:** hot-read data (listings, student profile summaries) — reduces DB load during peak preference-submission windows
- **Object storage:** documents, certificates
- **Search:** internship listing search/filter (Round-3 browsing needs fast filtered search at scale)
- **Monitoring/Logging:** see §16
- **CI/CD, containerization, orchestration:** see §19
- **Disaster recovery:** daily encrypted backups, documented RTO/RPO, allocation-run state is always reconstructable from the audit log (never only in-memory)

---

## 11. Database Architecture

### 11.1 Major entities (relational core)
`Student`, `StudentProfile`, `Education`, `Skill`, `StudentSkill (M:N)`, `Company`, `CompanyVerification`, `InternshipListing`, `ListingRequirement`, `Preference (ranked, versioned)`, `Application`, `EligibilityRecord`, `AllocationRun`, `AllocationResult`, `Waitlist`, `ReallocationEvent`, `Feedback`, `Notification`, `AuditLog`, `AdminAction`, `DocumentVerification`

Key constraints:
- `Preference` is versioned (append new row on change, never overwrite — needed for explainability and audit)
- `AllocationResult` references the exact `AllocationRun` id and the constraint-set snapshot used — **every allocation is reproducible from stored state**, which is essential for explainability and appeals
- Unique constraint preventing a student holding two simultaneously "accepted" allocations

### 11.2 SQL vs NoSQL
**Primary store: PostgreSQL.** This is a transactional, relationship-heavy, integrity-critical domain (government money movement via DBT, legal eligibility, audit requirements) — this is exactly what an ACID relational database is for. NoSQL is not justified here; don't add MongoDB "for flexibility" when the actual need is strong consistency.

**Exception:** the RAG vector store uses `pgvector` **inside the same PostgreSQL instance** where feasible (avoids an unjustified extra system), escalating to a dedicated vector DB (Qdrant) only if corpus size/query load genuinely outgrows pgvector — don't provision for that on day one.

### 11.3 Consistency, concurrency, auditability
- Allocation commit is a single DB transaction — either the full result set commits or none does
- Optimistic locking on preference and profile edits during open windows
- **Historical allocation snapshots:** every completed `AllocationRun` and its full input snapshot (scores, constraints, capacity at time of run) is retained — required both for explainability and for any future audit/RTI-style request
- Indexing: composite indexes on (student_id, cycle_id), (listing_id, region), (allocation_run_id) for fast explanation lookups at scale

---

## 12. Security Architecture

Sensitive data in this system: Aadhaar-linked identity, bank account details (for DBT), income declarations, category/reservation status, disability/accessibility status. This is government-scale PII — treat it accordingly.

- **Authentication:** OTP + password for students; MFA (TOTP or hardware-key) mandatory for all admin/company roles
- **Authorization:** strict RBAC — student, company-recruiter, regional-admin, national-admin, auditor, grievance-officer as distinct roles with least-privilege scopes
- **Encryption:** TLS in transit; field-level encryption at rest for Aadhaar/bank data specifically (not just whole-disk encryption)
- **Data masking:** category/accessibility/income fields masked in all views except where a role's function explicitly requires them (e.g. quota-verification admin only)
- **Secure APIs:** input validation, strict schema enforcement, rate limiting per token, anti-scraping protections on the listing-browse endpoints
- **Audit logs:** every admin action, override, and allocation-run commit is logged immutably (append-only), separate storage from operational DB
- **Secrets management:** vault-based secrets store, no credentials in code/config
- **Session security:** short-lived tokens, refresh rotation, forced re-auth for sensitive admin actions (e.g. approving an allocation run)
- **File upload security:** virus scanning, strict type/size validation for certificate/document uploads
- **Fraud/abuse prevention:** velocity checks on registration, duplicate-Aadhaar-hash detection, anomaly flags on company listings (see §9)
- **Privacy/retention:** documented data retention policy per government data-handling norms; PII purge process for withdrawn applications past retention window
- **Admin security:** every override action requires a typed justification, is logged, and above a severity threshold requires second-admin approval (four-eyes principle)

---

## 13. Scalability Architecture

- **Peak traffic patterns:** registration surges around cycle-open dates, preference-submission surges near deadline — plan for bursty load, not steady load
- **Allocation must not block the rest of the app:** the allocation engine runs as an **async, queued, separately-scaled service** — students can keep browsing/updating profile while a batch run executes elsewhere; this is a hard architectural requirement, not a nice-to-have
- **Batch decomposition:** allocation runs are decomposed by region/sector into parallelizable sub-problems where the MIP structure allows (with a final cross-region reconciliation pass for shared-capacity companies) — this avoids a single monolithic solve timing out on a lakhs-scale dataset
- **Idempotency:** allocation-run triggers are idempotent keyed by cycle+run-id — a retried trigger doesn't double-run
- **Retry mechanisms:** notification dispatch and reallocation triggers use queue-based retry with backoff, dead-letter queue for persistent failures surfaced to admin
- **Horizontal scaling:** stateless API/backend layers scale horizontally behind the gateway; DB scales via read replicas for analytics/browse-heavy read load, with the transactional writer staying single-primary for consistency
- **Caching:** listing browse/search results cached with short TTL during high-traffic windows

---

## 14. Allocation Simulation (Admin Sandbox)

Before any allocation run affects real students, admins can:
- Run a **dry run** against the current live dataset without committing
- Compare outcomes across constraint-weight variants (e.g. raise aspirational-district quota, see the effect on utilization/preference-satisfaction before committing)
- See projected: seat utilization %, preference satisfaction distribution, unallocated-student count, fairness-metric deltas, flagged unfair-looking clusters
- Compare **two full scenario runs side-by-side**
- Only after a simulation is reviewed and explicitly approved does the **same run configuration** get committed as the real allocation — the committed run must reuse the exact approved simulation's parameters (prevents "approve run A, silently commit run B" risk)

This single feature is one of the strongest differentiators in your pitch — it directly answers "how do you prevent a bad Round-1-style outcome from happening again," with a concrete mechanism.

---

## 15. Human-in-the-Loop

**Automated (no human needed for routine cases):**
- Eligibility rule-checks with clean data
- Scoring/compatibility computation
- Deferred-acceptance + MIP solve itself
- Waitlist promotion on standard dropout events
- Notification dispatch

**Requires human review/approval:**
- Final commit of any allocation run (mandatory approval step, no auto-publish)
- Any override of an automated eligibility rejection
- Any manual reassignment outside the optimizer's output
- Any relaxed-constraint fallback run (see §9 timeout handling)
- Fraud/anomaly flags above a severity threshold

**Exception queue + escalation:** any case the automated pipeline can't confidently resolve (missing data, tie beyond deterministic tiebreak, conflicting records) lands in a queued exception list with full context, assignable to an admin, with a mandatory resolution-reason field feeding the audit trail.

An administrator can always see, override, and roll back an allocation outcome before it is finally locked-in — the system never removes human control, only removes *repetitive manual labor*.

---

## 16. Observability

**Dashboards, stakeholder-scoped:**
- **National admin dashboard:** allocation success rate, seat utilization, preference-satisfaction distribution, fairness-metric trend, regional distribution heatmap, unallocated-student count trend across cycles
- **Regional admin dashboard:** same, scoped to region
- **Company dashboard:** their own listing fill rate, candidate quality distribution, dropout rate
- **Engineering/ops dashboard:** API latency, queue depth, failed-job count, allocation-run duration, solver convergence stats, system error rate
- **Grievance/fraud dashboard:** fraud-anomaly alert queue, exception-queue backlog

**Core metrics tracked:** allocation success rate, allocation run duration, seat utilization, preference satisfaction, unallocated-student count, unfilled-seat count, skill-mismatch rate, regional distribution parity, fairness-metric values, API latency (p50/p95/p99), queue depth, failed job count, fraud alert count.

---

## 17. Features We Should NOT Build (explicitly rejected, with reasons)

| Rejected feature | Why |
|---|---|
| General-purpose AI chatbot for students | Duplicates FAQ content, high hallucination risk if not tightly scoped, adds demo flash without solving a real documented problem |
| Deep-learning recommendation "feed" | Round 3 already gives students browsing agency; a heavy recommender adds opacity without a proven need |
| Blockchain for allocation records | PostgreSQL with an append-only audit log gives the same tamper-evidence and auditability guarantees a government auditor actually needs, at a fraction of the operational complexity — blockchain here is technology-for-its-own-sake |
| Full microservices mesh | Unjustified operational overhead for this team size and initial scale — see §10 |
| Deep NLP/LLM for skill extraction | Overkill for structured skill-tag data; a lightweight embedding-similarity approach is cheaper, faster, and just as effective at this stage |
| Reinforcement learning for allocation | RL needs a reward signal and simulation environment mature enough to be trustworthy for real people's internships — far too risky/opaque for a government allocation system; classical constrained optimization is the right tool here, not RL |
| Federated learning | No genuine cross-institution model-training need exists here; this is a centralized government dataset problem, not a privacy-distributed-training problem — don't add it because it sounds advanced |

*(These match near-verbatim to what several public "SIH25033" prototypes online proposed — Digital Twins, RL, blockchain audit, federated learning, all in one pitch. Explicitly rejecting them, with reasons, is itself a strong differentiator: it signals engineering judgment rather than buzzword-stacking, which is rare and will stand out to evaluators who've seen the same list of overused terms in every other team's slide.)*

---

## 18. Features Typical Projects Miss (worth including)

- Allocation simulation/dry-run sandbox (§14) — genuinely rare in student projects
- Explainable per-student allocation reasoning (§6) — most projects show a score, not a reason
- Waitlist + incremental reallocation as a first-class workflow, not an afterthought
- Fairness as measurable dashboarded metrics, not a claimed feature
- Exception queue with mandatory reason-logging for overrides
- Historical allocation-run snapshotting for reproducibility/audit
- Skill-gap-based pre-joining recommendation (turns a "no match" outcome into something actionable instead of a dead end)
- Deterministic, documented tiebreaking (most projects wave this away)

---

## 19. Technology Stack (with reasoning)

| Layer | Choice | Why |
|---|---|---|
| Frontend | **React** (Next.js if SSR/SEO matters for public listing pages) | Team familiarity, strong ecosystem, fast to build a clean, accessible multi-role UI |
| Backend (core) | **Node.js (NestJS)** or **Java Spring Boot** | Spring Boot is the safer choice for **government deployment suitability** — widely used in Indian gov-tech stacks (eGov, DigiLocker-adjacent systems), strong typing, mature security tooling, easier for a government team to maintain long-term. Recommend Spring Boot if team has the skill; Node/NestJS as a faster-to-build SIH-prototype fallback |
| Allocation engine | **Python + Google OR-Tools (CP-SAT/MIP solver)** | Purpose-built constraint/MIP solving, free, well-documented, proven at this problem scale |
| ML/scoring service | **Python + FastAPI + XGBoost + sentence-transformers** | Right-sized tools for tabular + embedding tasks — no need for PyTorch-scale infra here |
| Database | **PostgreSQL** | ACID guarantees for transactional integrity + government audit needs; pgvector extension covers RAG needs without a second system (see §11) |
| Cache | **Redis** | Standard, low-latency, well-understood ops burden |
| Messaging | **RabbitMQ** for SIH-scale prototype; **Kafka** only if production volume genuinely needs stream-processing/replay semantics — don't default to Kafka for prototype simplicity |
| Search | **OpenSearch** | Open-source, avoids licensing questions for a government deployment (vs. Elasticsearch's licensing history) |
| Vector DB | **pgvector** (default), escalate to **Qdrant** only if scale demands it | Avoids an unjustified extra system at launch (§11) |
| Cloud | **Government-empanelled cloud (MeitY-empanelled providers)** for production; standard AWS/GCP free tier for SIH prototype | Real government deployments must use empanelled infrastructure — say this explicitly to judges, it shows deployment awareness |
| Containerization | **Docker** | Standard, low-friction |
| Orchestration | **Kubernetes** for production; plain Docker Compose acceptable for SIH prototype | Don't over-engineer the prototype — K8s adds no demo value at hackathon scale |
| CI/CD | **GitHub Actions** | Free, sufficient, well-understood |
| Monitoring | **Prometheus + Grafana** | Standard, open-source, matches the dashboards described in §16 |
| Logging | **OpenTelemetry + ELK/OpenSearch** | Consistent with search-layer choice, avoids introducing a third search-adjacent system |

---

## 20. API Design (representative, not exhaustive)

**Student**
```
POST   /students/profile
GET    /internships?filters=...
POST   /preferences
GET    /allocation/status
GET    /allocation/explanation
POST   /allocation/accept
POST   /allocation/reject
```

**Company**
```
POST   /internships
PUT    /internships/{id}
GET    /internships/{id}/applications
PUT    /internships/{id}/capacity
```

**Admin**
```
POST   /allocation/simulate
GET    /allocation/simulate/{run_id}/results
POST   /allocation/run
POST   /allocation/approve
POST   /allocation/reallocate
GET    /allocation/metrics
GET    /fairness/dashboard
GET    /exceptions/queue
POST   /exceptions/{id}/resolve
```

**Cross-cutting**
```
POST   /notifications/dispatch        (internal, queue-triggered)
POST   /documents/verify
GET    /audit/logs?filters=...
GET    /analytics/cycle/{id}
POST   /rag/query                     (admin/grievance-officer scoped)
```

---

## 21. MVP vs SIH Demo vs Production

**MVP (build-first, proves the core claim):**
- Student/company/admin core flows (registration → listing → preference → allocation)
- Deferred-acceptance + MIP allocation engine on a realistic synthetic dataset (thousands, not lakhs)
- Explanation engine (§6) — this is your differentiator, build it early, not last
- Basic simulation sandbox (§14)
- Basic fairness metrics dashboard

**SIH Demo (add on top of MVP for the pitch):**
- Polished UI for all three roles
- Live simulation → approve → allocate → explain walkthrough as the core demo narrative
- A pre-seeded scenario showing a fairness-quota effect visibly changing the outcome (your "wow" moment)
- One live edge case triggered live (e.g. a dropout → automatic reallocation)

**Production version (roadmap slide, not built for SIH):**
- Real integration with actual PMIS data sources / eKYC / Aadhaar-linked DBT (requires formal government data-sharing agreement — say this explicitly, don't fake it)
- Full RAG guideline assistant for admin/grievance officers
- Full dropout-risk and demand-forecasting ML services
- Kubernetes-based production deployment on empanelled cloud
- Formal security audit and load testing at full national scale

---

## 22. Innovation Points (for your pitch, in order of what will actually land with judges)

1. **You're fixing a documented, publicly-reported failure** (Round 1's blind-algorithm mismatch problem) — not inventing a hypothetical problem. This is your strongest opening line.
2. **Stable matching + MIP hybrid** is a real operations-research-grounded choice, not "we called it AI" — you can defend this under technical questioning, which most teams can't.
3. **Explainability is a first-class engine**, not a UI afterthought — every allocation has a traceable, reproducible reason.
4. **Fairness is measured, not claimed** — dashboarded metrics instead of a stated intention.
5. **Simulation-before-commit** directly prevents a repeat of the Round-1 failure mode — this is a concrete causal link between your feature and the government's actual documented pain point.
6. **You explicitly rejected buzzword features** (blockchain, RL, federated learning, generic chatbot) with stated reasons — this reads as engineering maturity to any judge who's sat through a dozen other pitches with all of those bolted on for no reason.

---

## 23. Evaluation Metrics (for your own testing, and to show judges you have them)

- Preference satisfaction rate (% assigned to top-2 choice)
- Fairness-metric parity gap (max deviation across regions/categories)
- Allocation run time at target scale
- Stability violations (should be zero, by construction — testable)
- Explanation generation latency
- Simulation-to-commit consistency (100% required — no drift allowed)

---

## 24. Presentation Framing for Tomorrow's Screening

Given screening is imminent, prioritize saying these things clearly, in this order, even if the build isn't fully done:

1. **Open with the Round 1 failure fact** — it grounds everything and shows real research, not guesswork.
2. **Name your algorithm precisely** — "deferred-acceptance stable matching with a MIP fairness/quota layer" — don't say "AI-based smart matching," say the real technique. Judges who know the space will immediately respect the specificity; judges who don't will still register that you know something concrete.
3. **Show the explanation output** as your visual centerpiece if you can mock even one example screen — it's the single most differentiating artifact.
4. **State clearly what you rejected and why** (§17) — this is a fast way to signal maturity in a 5-minute slot.
5. **Have the MVP/Demo/Production split ready** (§21) if asked "what will you actually have working" — answer honestly about what's prototype vs. roadmap.

---

*Document prepared for internal team prep — verify all PMIS factual figures marked [VERIFY] against the official portal before stating them as fact to evaluators.*
