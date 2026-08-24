# Edge Cases

| Case | Expected behavior |
|---|---|
| zero eligible listings | explain skill gaps and retry next cycle |
| many eligible listings | paginate and cap display |
| demand exceeds supply | waitlist |
| supply exceeds demand | unfilled seat report |
| tie | deterministic documented tiebreak |
| student changes preference | version preference and incremental re-solve |
| company changes requirements | apply to unresolved passes only |
| company withdraws | affected students enter priority reallocation |
| student rejects | release seat and promote waitlist |
| no response | decline after published SLA |
| multiple accepted allocations | freeze records and create exception |
| duplicate account | flag and route to review |
| invalid certificate | exclude from allocation until verified |
| solver timeout | no auto-publish, require human review |
| concurrent preference update | optimistic locking |
| concurrent allocation run | cycle lock |
| admin override | reason required and audited |
| ML low confidence | bounded signal and review flag |
| Aadhaar dependency unavailable | degraded verification queue |
| budget insufficient | do not allocate unfunded seats |
| MCoC active | freeze publishing for affected region |
| deadline traffic surge | waiting-room/request-shaping adapter |
| company never confirms joining | company non-fulfillment event and reallocation |
| cross-scheme duplicate benefit | flag for manual review |
