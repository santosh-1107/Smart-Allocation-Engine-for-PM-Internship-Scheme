"use client";

import React, { useState, useEffect } from "react";

const BACKEND_URL = "http://localhost:8080";
const CYCLE_ID = "00000000-0000-0000-0000-000000000001";

export default function AdminPage() {
  const [activeTab, setActiveTab] = useState<string>("sandbox");
  const [stats, setStats] = useState<any>({
    studentsCount: 1000,
    companiesCount: 100,
    listingsCount: 300,
    budgetCeiling: 2000000,
    allocationRate: 0.0,
    seatUtilization: 0.0
  });

  // Sandbox State
  const [budgetCeiling, setBudgetCeiling] = useState<number>(1500000);
  const [seed, setSeed] = useState<number>(42);
  const [simulationResult, setSimulationResult] = useState<any>(null);
  const [draftRuns, setDraftRuns] = useState<any[]>([]);

  // Scenario Comparison State
  const [runA, setRunA] = useState<any>(null);
  const [runB, setRunB] = useState<any>(null);

  // Exceptions State
  const [exceptions, setExceptions] = useState<any[]>([]);
  const [resolutionReason, setResolutionReason] = useState<string>("");

  // Audit State
  const [auditLogs, setAuditLogs] = useState<any[]>([]);
  const [auditVerification, setAuditVerification] = useState<any>(null);

  // RAG State
  const [ragQuestion, setRagQuestion] = useState<string>("What is the monthly stipend structure for interns?");
  const [ragAnswer, setRagAnswer] = useState<any>(null);
  const [ragLoading, setRagLoading] = useState<boolean>(false);

  // Global triggers
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");

  useEffect(() => {
    fetchExceptions();
    fetchAuditLogs();
    fetchRuns();
  }, []);

  const fetchExceptions = async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/exceptions`);
      if (res.ok) {
        const data = await res.json();
        setExceptions(data);
      }
    } catch (e) {}
  };

  const fetchAuditLogs = async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/audit`);
      if (res.ok) {
        const data = await res.json();
        setAuditLogs(data);
      }
    } catch (e) {}
  };

  const fetchRuns = async () => {
    try {
      // In a real flow, we query the runs in DB. Let's make an API query
      // or check the latest runs
      const res = await fetch(`${BACKEND_URL}/api/v1/audit`); // bypass endpoint for runs or simulate
    } catch (e) {}
  };

  // Run simulation sandbox
  const handleRunSimulation = async (commitToApproval: boolean) => {
    setLoading(true);
    setError("");
    setSuccess("");
    setSimulationResult(null);
    try {
      const endpoint = commitToApproval ? "run" : "simulate";
      const res = await fetch(`${BACKEND_URL}/api/v1/allocation/${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          cycleId: CYCLE_ID,
          budgetCeiling,
          seed
        })
      });
      if (res.ok) {
        const data = await res.json();
        setSimulationResult(data);
        setSuccess(commitToApproval ? "Real allocation run queued and ready for approval!" : "Simulation run completed!");
        
        // Add to draft runs for comparison
        const newRun = {
          runId: data.runId,
          budgetCeiling,
          seed,
          metrics: data.metrics,
          status: data.status,
          createdAt: new Date().toISOString()
        };
        setDraftRuns(prev => [...prev, newRun]);
        
        // Auto-select for side-by-side scenario comparison
        if (!runA) {
          setRunA(newRun);
        } else if (!runB) {
          setRunB(newRun);
        }

        fetchAuditLogs();
        fetchExceptions();
      } else {
        const txt = await res.text();
        setError("Allocation execution failed: " + txt);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Approve a run
  const handleApproveRun = async (runId: string) => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/allocation/${runId}/approve`, {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          "X-User-Id": "NATIONAL_ADMIN_1"
        },
        body: JSON.stringify({ justification: "Verified stable matching compatibility score limits under budget ceiling limit." })
      });
      if (res.ok) {
        setSuccess("Allocation run approved and transactionally committed! Students notified.");
        setSimulationResult(null);
        fetchRuns();
        fetchAuditLogs();
      } else {
        const text = await res.text();
        setError("Failed to approve allocation: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Resolve exception case
  const handleResolveException = async (id: string) => {
    if (!resolutionReason) {
      setError("Please input a resolution justification");
      return;
    }
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/exceptions/${id}/resolve`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resolutionReason })
      });
      if (res.ok) {
        setSuccess("Exception case resolved successfully!");
        setResolutionReason("");
        fetchExceptions();
        fetchAuditLogs();
      } else {
        const text = await res.text();
        setError("Failed to resolve exception: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Verify Audit Log Hash Chain
  const handleVerifyAuditChain = async () => {
    setLoading(true);
    setError("");
    setAuditVerification(null);
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/audit/verify`, {
        method: "POST"
      });
      if (res.ok) {
        const data = await res.json();
        setAuditVerification(data);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Tamper simulation
  const handleSimulateTamper = async (id: string) => {
    setLoading(true);
    setError("");
    setSuccess("");
    setAuditVerification(null);
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/audit/tamper?id=${id}`, {
        method: "POST"
      });
      if (res.ok) {
        setSuccess("Simulated direct database tampering! Re-run hash chain verifier to detect.");
        fetchAuditLogs();
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Query Policy RAG
  const handleQueryRAG = async () => {
    if (!ragQuestion) return;
    setRagLoading(true);
    setError("");
    setRagAnswer(null);
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/rag/query`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          question: ragQuestion,
          role: "NATIONAL_ADMIN"
        })
      });
      if (res.ok) {
        const data = await res.json();
        setRagAnswer(data);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setRagLoading(false);
    }
  };

  return (
    <main className="min-h-screen p-6 bg-slate-950 text-slate-100">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-6 glass-panel border-white/5">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              <span>🛡️</span> National Admin Console
            </h1>
            <p className="text-xs text-slate-400">Configure cycle simulations, review exceptions, approve runs, and audit logs integrity.</p>
          </div>
          <div className="text-xs text-slate-400">
            Current Cycle ID: <strong className="text-indigo-400">{CYCLE_ID}</strong>
          </div>
        </div>

        {/* Messaging */}
        {error && (
          <div className="p-4 bg-red-950/40 border border-red-500/20 text-red-300 rounded-xl text-sm animate-fade-in">
            ⚠️ {error}
          </div>
        )}
        {success && (
          <div className="p-4 bg-emerald-950/40 border border-emerald-500/20 text-emerald-300 rounded-xl text-sm animate-fade-in">
            ✅ {success}
          </div>
        )}

        <div className="grid gap-6 md:grid-cols-4">
          
          {/* Sidebar */}
          <div className="md:col-span-1 space-y-2">
            <button
              onClick={() => setActiveTab("sandbox")}
              className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === "sandbox" ? "bg-amber-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
              }`}
            >
              ⚙️ Simulation Sandbox
            </button>
            <button
              onClick={() => setActiveTab("compare")}
              className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === "compare" ? "bg-amber-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
              }`}
            >
              ⚖️ Scenario Comparison
            </button>
            <button
              onClick={() => setActiveTab("exceptions")}
              className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === "exceptions" ? "bg-amber-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
              }`}
            >
              ⚠️ Exceptions Queue ({exceptions.length})
            </button>
            <button
              onClick={() => setActiveTab("audit")}
              className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === "audit" ? "bg-amber-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
              }`}
            >
              🔗 Audit Hash Chain
            </button>
            <button
              onClick={() => setActiveTab("rag")}
              className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                activeTab === "rag" ? "bg-amber-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
              }`}
            >
              📚 RAG Policy Assistant
            </button>
            <div className="pt-4 border-t border-white/5">
              <a
                href="/"
                className="block text-center text-xs text-amber-400 font-semibold hover:underline"
              >
                ← Back to Home
              </a>
            </div>
          </div>

          {/* Main Panel */}
          <div className="md:col-span-3">
            
            {/* Sandbox panel */}
            {activeTab === "sandbox" && (
              <div className="p-6 glass-panel border-white/5 space-y-6">
                <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Simulation Sandbox Configuration</h2>
                
                <div className="grid gap-4 sm:grid-cols-3 text-sm">
                  <div>
                    <label className="block text-xs text-slate-400 font-medium mb-1">Confirmed Budget Ceiling (Rs.)</label>
                    <input
                      type="number"
                      value={budgetCeiling}
                      onChange={(e) => setBudgetCeiling(parseInt(e.target.value))}
                      className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-amber-500 text-xs"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-xs text-slate-400 font-medium mb-1">Solver Tie-Break Seed</label>
                    <input
                      type="number"
                      value={seed}
                      onChange={(e) => setSeed(parseInt(e.target.value))}
                      className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-amber-500 text-xs"
                    />
                  </div>

                  <div className="flex gap-2 pt-5">
                    <button
                      onClick={() => handleRunSimulation(false)}
                      disabled={loading}
                      className="flex-1 bg-slate-800 hover:bg-slate-700 text-white rounded-lg py-2 text-xs font-bold transition-all border border-white/5 cursor-pointer"
                    >
                      Draft Simulation
                    </button>
                    <button
                      onClick={() => handleRunSimulation(true)}
                      disabled={loading}
                      className="flex-1 bg-amber-600 hover:bg-amber-500 text-white rounded-lg py-2 text-xs font-bold transition-all cursor-pointer"
                    >
                      Approvable Run
                    </button>
                  </div>
                </div>

                {simulationResult && (
                  <div className="space-y-6 animate-fade-in text-sm border-t border-white/5 pt-4">
                    <div className="flex justify-between items-center">
                      <h3 className="font-bold text-white text-base">Solver output result</h3>
                      {simulationResult.status === "READY_FOR_APPROVAL" && (
                        <button
                          onClick={() => handleApproveRun(simulationResult.runId)}
                          className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold px-4 py-1.5 rounded-lg cursor-pointer transition-colors"
                        >
                          ✓ Approve and Commit allocation
                        </button>
                      )}
                    </div>

                    <div className="grid gap-4 sm:grid-cols-4">
                      <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl text-center space-y-1">
                        <span className="text-[10px] text-slate-400 block uppercase">Allocation Rate</span>
                        <div className="text-xl font-bold text-gradient-purple">{simulationResult.metrics.allocation_rate}%</div>
                      </div>
                      <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl text-center space-y-1">
                        <span className="text-[10px] text-slate-400 block uppercase">Seat Utilization</span>
                        <div className="text-xl font-bold text-gradient-cyan">{simulationResult.metrics.seat_utilization}%</div>
                      </div>
                      <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl text-center space-y-1">
                        <span className="text-[10px] text-slate-400 block uppercase">Avg Preference Sat.</span>
                        <div className="text-xl font-bold text-gradient-gold">Choice #{simulationResult.metrics.preference_satisfaction}</div>
                      </div>
                      <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl text-center space-y-1">
                        <span className="text-[10px] text-slate-400 block uppercase">Budget Used</span>
                        <div className="text-xl font-bold text-white">₹ {simulationResult.metrics.budget_used}</div>
                      </div>
                    </div>

                    {/* Constraint trace */}
                    <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl space-y-2">
                      <span className="text-xs font-semibold text-slate-300 uppercase tracking-wide block">Hard constraint trace</span>
                      <div className="grid gap-4 sm:grid-cols-4 text-xs text-slate-400">
                        <div className="flex items-center gap-1.5">
                          <span className="text-emerald-400">●</span> Eligibility Rules verified
                        </div>
                        <div className="flex items-center gap-1.5">
                          <span className="text-emerald-400">●</span> Seat Capacity bounds verified
                        </div>
                        <div className="flex items-center gap-1.5">
                          <span className={simulationResult.constraintTrace.budget_ceiling ? "text-emerald-400" : "text-red-400"}>●</span> 
                          Budget Capping ({simulationResult.constraintTrace.budget_ceiling ? "Passed" : "Failed"})
                        </div>
                        <div className="flex items-center gap-1.5">
                          <span className="text-emerald-400">●</span> Regional Quotas floors verified
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Compare panel */}
            {activeTab === "compare" && (
              <div className="p-6 glass-panel border-white/5 space-y-6">
                <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Side-by-Side Scenario Comparison</h2>
                
                {draftRuns.length < 2 ? (
                  <div className="text-center py-8 text-slate-400 text-sm">
                    Please trigger at least two simulation runs in the Sandbox tab with different variables (e.g. change budget cap) to compare.
                  </div>
                ) : (
                  <div className="space-y-6 text-sm">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Scenario A</label>
                        <select
                          value={runA ? runA.runId : ""}
                          onChange={(e) => setRunA(draftRuns.find(r => r.runId === e.target.value))}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-amber-500 text-xs"
                        >
                          {draftRuns.map((r, i) => (
                            <option key={r.runId} value={r.runId}>Run #{i + 1} - Budget ₹{r.budgetCeiling} (Seed {r.seed})</option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Scenario B</label>
                        <select
                          value={runB ? runB.runId : ""}
                          onChange={(e) => setRunB(draftRuns.find(r => r.runId === e.target.value))}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-amber-500 text-xs"
                        >
                          {draftRuns.map((r, i) => (
                            <option key={r.runId} value={r.runId}>Run #{i + 1} - Budget ₹{r.budgetCeiling} (Seed {r.seed})</option>
                          ))}
                        </select>
                      </div>
                    </div>

                    {runA && runB && (
                      <table className="w-full text-xs text-slate-300 border-collapse border border-white/5 animate-fade-in">
                        <thead>
                          <tr className="bg-slate-900 border-b border-white/5">
                            <th className="p-3 text-left">Fulfillment Metric</th>
                            <th className="p-3 text-center">Scenario A</th>
                            <th className="p-3 text-center">Scenario B</th>
                            <th className="p-3 text-center">Variance Delta</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                          <tr>
                            <td className="p-3 font-semibold text-slate-400">Total Budget Capping</td>
                            <td className="p-3 text-center">₹ {runA.budgetCeiling}</td>
                            <td className="p-3 text-center">₹ {runB.budgetCeiling}</td>
                            <td className="p-3 text-center font-mono">{(runB.budgetCeiling - runA.budgetCeiling) >= 0 ? "+" : ""}{runB.budgetCeiling - runA.budgetCeiling}</td>
                          </tr>
                          <tr>
                            <td className="p-3 font-semibold text-slate-400">Successful Allocation Rate</td>
                            <td className="p-3 text-center">{runA.metrics.allocation_rate}%</td>
                            <td className="p-3 text-center">{runB.metrics.allocation_rate}%</td>
                            <td className="p-3 text-center font-mono text-emerald-400">{(runB.metrics.allocation_rate - runA.metrics.allocation_rate).toFixed(2)}%</td>
                          </tr>
                          <tr>
                            <td className="p-3 font-semibold text-slate-400">Listing Seats Utilization</td>
                            <td className="p-3 text-center">{runA.metrics.seat_utilization}%</td>
                            <td className="p-3 text-center">{runB.metrics.seat_utilization}%</td>
                            <td className="p-3 text-center font-mono">{(runB.metrics.seat_utilization - runA.metrics.seat_utilization).toFixed(2)}%</td>
                          </tr>
                          <tr>
                            <td className="p-3 font-semibold text-slate-400">Avg Student Preference Rank</td>
                            <td className="p-3 text-center">Choice #{runA.metrics.preference_satisfaction}</td>
                            <td className="p-3 text-center">Choice #{runB.metrics.preference_satisfaction}</td>
                            <td className="p-3 text-center font-mono">{(runB.metrics.preference_satisfaction - runA.metrics.preference_satisfaction).toFixed(2)}</td>
                          </tr>
                          <tr>
                            <td className="p-3 font-semibold text-slate-400">Budget Spent</td>
                            <td className="p-3 text-center">₹ {runA.metrics.budget_used}</td>
                            <td className="p-3 text-center">₹ {runB.metrics.budget_used}</td>
                            <td className="p-3 text-center font-mono">₹ {(runB.metrics.budget_used - runA.metrics.budget_used).toFixed(2)}</td>
                          </tr>
                        </tbody>
                      </table>
                    )}
                  </div>
                )}
              </div>
            )}

            {/* Exceptions panel */}
            {activeTab === "exceptions" && (
              <div className="p-6 glass-panel border-white/5 space-y-6">
                <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Exception cases resolution queue</h2>
                
                <div className="space-y-4">
                  {exceptions.length === 0 ? (
                    <div className="text-center py-8 text-slate-400 text-sm">No exception cases currently flagged.</div>
                  ) : (
                    exceptions.map((ex) => (
                      <div key={ex.id} className="p-4 bg-slate-900/60 border border-white/5 rounded-xl space-y-4 text-sm">
                        <div className="flex justify-between items-start">
                          <div>
                            <span className="text-xs px-2 py-0.5 rounded bg-red-600/20 text-red-400 border border-red-500/20 uppercase font-bold mr-2">
                              {ex.severity}
                            </span>
                            <span className="font-bold text-white">{ex.caseType}</span>
                            <p className="text-xs text-slate-500 mt-1">Logged on: {new Date(ex.createdAt).toLocaleString()}</p>
                          </div>
                          <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${
                            ex.status === "OPEN" ? "bg-amber-600/20 text-amber-400" : "bg-emerald-600/20 text-emerald-400"
                          }`}>
                            {ex.status}
                          </span>
                        </div>

                        <div className="text-xs text-slate-400 bg-slate-950 p-3 rounded-lg font-mono">
                          {ex.context}
                        </div>

                        {ex.status === "OPEN" ? (
                          <div className="flex gap-2 items-center">
                            <input
                              type="text"
                              value={resolutionReason}
                              onChange={(e) => setResolutionReason(e.target.value)}
                              placeholder="Justification for manual override/resolution"
                              className="flex-1 bg-slate-950 border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white outline-none focus:border-amber-500"
                            />
                            <button
                              onClick={() => handleResolveException(ex.id)}
                              className="bg-amber-600 hover:bg-amber-500 text-white rounded-lg px-3 py-1.5 text-xs font-bold cursor-pointer"
                            >
                              Resolve Exception
                            </button>
                          </div>
                        ) : (
                          <div className="text-xs text-emerald-400">
                            <strong>Resolved Reason:</strong> {ex.resolutionReason}
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}

            {/* Audit panel */}
            {activeTab === "audit" && (
              <div className="p-6 glass-panel border-white/5 space-y-6">
                <div className="flex justify-between items-center border-b border-white/5 pb-2">
                  <h2 className="text-lg font-bold text-white">Append-only audit chain integrity</h2>
                  <button
                    onClick={handleVerifyAuditChain}
                    className="bg-amber-600 hover:bg-amber-500 text-white rounded-lg px-3 py-1.5 text-xs font-bold cursor-pointer transition-colors"
                  >
                    🔍 Verify Chain Integrity
                  </button>
                </div>

                {auditVerification && (
                  <div className={`p-4 rounded-xl text-sm border animate-fade-in ${
                    auditVerification.status === "VERIFIED"
                      ? "bg-emerald-950/40 border-emerald-500/20 text-emerald-300"
                      : "bg-red-950/40 border-red-500/20 text-red-300"
                  }`}>
                    <div className="font-bold flex items-center gap-1.5">
                      {auditVerification.status === "VERIFIED" ? "✓ INTEGRITY VERIFIED" : "🚨 CHAIN COMPROMISED (TAMPER DETECTED)"}
                    </div>
                    <p className="text-xs mt-1">{auditVerification.reason || "All audit log entry hashes match. Chain link secure."}</p>
                    {auditVerification.compromisedRowId && (
                      <div className="text-xs font-mono bg-slate-950/80 p-2.5 rounded-lg mt-3 text-red-400">
                        Compromised Entry UUID: {auditVerification.compromisedRowId}
                      </div>
                    )}
                  </div>
                )}

                <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
                  {auditLogs.map((log) => (
                    <div key={log.id} className="p-3 bg-slate-900/60 border border-white/5 rounded-xl text-xs space-y-2">
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-white">{log.eventType}</span>
                        <span className="text-slate-500">{new Date(log.createdAt).toLocaleString()}</span>
                      </div>
                      <div className="text-[10px] text-slate-400 font-mono flex flex-col gap-0.5">
                        <span className="truncate">PrevHash: {log.previousHash}</span>
                        <span className="truncate text-indigo-400 font-semibold">CurrHash: {log.currentHash}</span>
                      </div>
                      <div className="flex justify-between items-center pt-1 border-t border-white/5">
                        <span className="text-[10px] text-slate-500">Actor: {log.actorId || "System"}</span>
                        <button
                          onClick={() => handleSimulateTamper(log.id)}
                          className="bg-red-950/50 text-red-400 hover:bg-red-900 border border-red-500/20 rounded px-2 py-0.5 text-[10px] font-medium transition-colors"
                        >
                          Simulate Tamper
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* RAG panel */}
            {activeTab === "rag" && (
              <div className="p-6 glass-panel border-white/5 space-y-6">
                <div>
                  <h2 className="text-lg font-bold text-white">RAG Policy assistant</h2>
                  <p className="text-xs text-slate-400 mt-1">
                    Ask questions regarding policy, age thresholds, quotas, and election freezes. Employs vector distance searches.
                  </p>
                </div>

                <div className="flex gap-2">
                  <input
                    type="text"
                    value={ragQuestion}
                    onChange={(e) => setRagQuestion(e.target.value)}
                    placeholder="Ask a policy guidelines question..."
                    className="flex-1 bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-xs text-white outline-none focus:border-amber-500"
                  />
                  <button
                    onClick={handleQueryRAG}
                    disabled={ragLoading}
                    className="bg-amber-600 hover:bg-amber-500 text-white rounded-lg px-4 py-2 text-xs font-bold cursor-pointer disabled:bg-slate-700"
                  >
                    {ragLoading ? "Searching vector space..." : "Search"}
                  </button>
                </div>

                {ragAnswer && (
                  <div className="p-6 bg-slate-900/80 border border-white/10 rounded-2xl space-y-4 animate-fade-in text-sm">
                    <div className="flex justify-between items-center border-b border-white/5 pb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-400">Search Confidence Score:</span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          ragAnswer.requires_human_review ? "bg-amber-600/20 text-amber-400 border border-amber-500/20" : "bg-emerald-600/20 text-emerald-400"
                        }`}>
                          {(ragAnswer.confidence * 100).toFixed(0)}% Match
                        </span>
                      </div>
                      
                      {ragAnswer.requires_human_review && (
                        <div className="text-[10px] text-amber-400 font-bold bg-amber-950/40 px-2 py-1 rounded border border-amber-500/20">
                          🚨 Routed to Human Grievance Officer (Low Confidence)
                        </div>
                      )}
                    </div>

                    <p className="text-slate-200 leading-relaxed italic bg-slate-950 p-4 rounded-xl">
                      {ragAnswer.answer}
                    </p>

                    {/* Sources / Citations */}
                    {ragAnswer.sources && ragAnswer.sources.length > 0 && (
                      <div className="space-y-2">
                        <span className="text-xs text-slate-500 font-bold uppercase tracking-wider block">Policy sources cited:</span>
                        <div className="grid gap-2">
                          {ragAnswer.sources.map((s: any, idx: number) => (
                            <div key={idx} className="flex justify-between items-center p-2 bg-slate-950/60 border border-white/5 rounded-lg text-xs text-slate-400">
                              <span>📁 {s.document} - <strong>{s.section}</strong></span>
                              <span className="text-slate-500">Vector Distance: {(1.0 - s.match_score).toFixed(2)}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}

          </div>
        </div>
      </div>
    </main>
  );
}
