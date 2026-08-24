"use client";

import React, { useState, useEffect } from "react";

const BACKEND_URL = "http://localhost:8080";
const SECTORS = ["IT Services", "Finance", "Healthcare", "Manufacturing", "Education", "Retail"];

export default function CompanyPage() {
  const [companies, setCompanies] = useState<any[]>([]);
  const [selectedCompanyId, setSelectedCompanyId] = useState<string>("");
  const [activeTab, setActiveTab] = useState<string>("dashboard");

  // Recruiter context data
  const [companyDetails, setCompanyDetails] = useState<any>(null);
  const [listings, setListings] = useState<any[]>([]);
  const [allocations, setAllocations] = useState<any[]>([]);

  // Post form state
  const [title, setTitle] = useState<string>("");
  const [location, setLocation] = useState<string>("");
  const [sector, setSector] = useState<string>("IT Services");
  const [capacity, setCapacity] = useState<number>(1);
  const [stipend, setStipend] = useState<number>(2000);
  const [requiredSkills, setRequiredSkills] = useState<string>("");

  // UI state
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedCompanyId) {
      fetchCompanyData();
    } else {
      setCompanyDetails(null);
      setListings([]);
      setAllocations([]);
    }
  }, [selectedCompanyId]);

  const fetchInitialData = async () => {
    try {
      // In a real database, we fetch all companies. Let's make an API call to get listings
      // and compile the list of companies dynamically from them!
      const res = await fetch(`${BACKEND_URL}/api/v1/internships?all=true`);
      if (res.ok) {
        const listingsData = await res.json();
        
        // Extract unique companies
        const compMap = new Map();
        listingsData.forEach((l: any) => {
          if (l.company) {
            compMap.set(l.company.id, l.company);
          }
        });
        const compList = Array.from(compMap.values());
        setCompanies(compList);
        if (compList.length > 0) {
          setSelectedCompanyId(compList[0].id);
        }
      }
    } catch (err: any) {
      console.error("Failed to load companies", err);
      // Mock fallback companies
      const mockComps = [
        { id: "c100f4e4-7d5a-4467-bc5b-4328b975e510", legalName: "Tata Consultancy Services" },
        { id: "c200c5a2-9d3b-4171-8b4e-4f38e671b420", legalName: "Reliance Industries Group" },
        { id: "c300f5e1-8d4a-4271-9b4e-5f38e671c430", legalName: "Infosys Technologies" }
      ];
      setCompanies(mockComps);
      setSelectedCompanyId(mockComps[0].id);
    }
  };

  const fetchCompanyData = async () => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      // 1. Fetch listings for this company
      // Filter list from API or fetch directly
      const res = await fetch(`${BACKEND_URL}/api/v1/internships?all=true`);
      if (res.ok) {
        const data = await res.json();
        const companyListings = data.filter((l: any) => l.company?.id === selectedCompanyId);
        setListings(companyListings);

        // Extract company details from the first matching listing
        if (companyListings.length > 0) {
          setCompanyDetails(companyListings[0].company);
        } else {
          const comp = companies.find(c => c.id === selectedCompanyId);
          setCompanyDetails(comp);
        }

        // 2. Fetch allocations for each listing
        const allocList: any[] = [];
        for (const l of companyListings) {
          const aRes = await fetch(`${BACKEND_URL}/api/v1/internships/${l.id}/applications`);
          if (aRes.ok) {
            const aData = await aRes.json();
            allocList.push(...aData);
          }
        }
        setAllocations(allocList);
      }
    } catch (err: any) {
      setError("Failed to load recruiter data: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Submit new internship posting
  const handlePostInternship = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title || !location) {
      setError("Title and Location are required.");
      return;
    }
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const skillsList = requiredSkills.split(",").map(s => s.trim()).filter(Boolean);
      const res = await fetch(`${BACKEND_URL}/api/v1/internships`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Role": "COMPANY_RECRUITER",
          "X-User-Id": selectedCompanyId
        },
        body: JSON.stringify({
          title,
          location,
          sector,
          capacity,
          stipendCompanyShare: stipend,
          requiredSkills: skillsList
        })
      });
      if (res.ok) {
        setSuccess("Internship listing posted successfully!");
        setTitle("");
        setLocation("");
        setRequiredSkills("");
        fetchCompanyData();
      } else {
        const text = await res.text();
        setError("Failed to post listing: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Update listing capacity
  const handleUpdateCapacity = async (listingId: string, newCap: number) => {
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/internships/${listingId}/capacity`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ capacity: newCap })
      });
      if (res.ok) {
        setSuccess("Listing capacity updated successfully!");
        fetchCompanyData();
      } else {
        const text = await res.text();
        setError("Failed to update capacity: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  // Confirm onboarding joining
  const handleConfirmJoining = async (listingId: string, studentId: string, confirmed: boolean) => {
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/internships/${listingId}/joining-confirmation`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          studentId,
          confirmed,
          comments: confirmed ? "Student reported on schedule." : "Student non-fulfillment recorded."
        })
      });
      if (res.ok) {
        setSuccess(confirmed ? "Joining confirmed successfully!" : "Non-fulfillment reported. Seat released.");
        fetchCompanyData();
      } else {
        const text = await res.text();
        setError("Failed to confirm joining: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <main className="min-h-screen p-6 bg-slate-950 text-slate-100">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-6 glass-panel border-white/5">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              <span>🏢</span> Company Recruiter Portal
            </h1>
            <p className="text-xs text-slate-400">Post internships, manage seat capacities, and confirm onboarding.</p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-medium">Recruiter Context:</span>
            <select
              value={selectedCompanyId}
              onChange={(e) => setSelectedCompanyId(e.target.value)}
              className="bg-slate-900/80 border border-white/10 text-white rounded-lg px-3 py-1.5 text-xs focus:ring-1 focus:ring-indigo-500 outline-none"
            >
              {companies.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.legalName}
                </option>
              ))}
            </select>
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

        {loading && (
          <div className="text-center py-12 text-slate-400 text-sm">Loading recruiter portal...</div>
        )}

        {!loading && companyDetails && (
          <div className="grid gap-6 md:grid-cols-4">
            
            {/* Sidebar navigation */}
            <div className="md:col-span-1 space-y-2">
              <button
                onClick={() => setActiveTab("dashboard")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "dashboard" ? "bg-purple-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                📊 Recruiter Dashboard
              </button>
              <button
                onClick={() => setActiveTab("post")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "post" ? "bg-purple-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                ➕ Post Internship Listing
              </button>
              <button
                onClick={() => setActiveTab("capacity")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "capacity" ? "bg-purple-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                👥 Capacity Management
              </button>
              <button
                onClick={() => setActiveTab("joining")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "joining" ? "bg-purple-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                🤝 Joining Confirmations
              </button>
              <div className="pt-4 border-t border-white/5">
                <a
                  href="/"
                  className="block text-center text-xs text-purple-400 font-semibold hover:underline"
                >
                  ← Back to Home
                </a>
              </div>
            </div>

            {/* Content panels */}
            <div className="md:col-span-3">
              
              {/* Dashboard Tab */}
              {activeTab === "dashboard" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <div className="border-b border-white/5 pb-2">
                    <h2 className="text-lg font-bold text-white">Recruiter overview</h2>
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl space-y-2">
                      <div className="text-xs text-slate-400">Legal Company Name</div>
                      <div className="text-lg font-bold text-white">{companyDetails.legalName}</div>
                      <div className="text-xs text-slate-500">CIN: {companyDetails.cin || "Pending Verification"}</div>
                    </div>

                    <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl space-y-2">
                      <div className="text-xs text-slate-400">Recruiter onboarding reliability</div>
                      <div className="text-lg font-bold text-white flex items-center gap-2">
                        <span>100%</span>
                        <span className="text-xs px-2 py-0.5 rounded bg-emerald-600/20 text-emerald-400 border border-emerald-500/20">Excellent</span>
                      </div>
                      <div className="text-xs text-slate-500">Based on zero non-fulfillment incidents.</div>
                    </div>
                  </div>

                  <div className="p-4 bg-purple-950/40 border border-purple-500/20 text-purple-300 rounded-xl text-xs space-y-1">
                    <span className="font-bold">Recruiter Information Checklist:</span>
                    <p>All postings are co-funded under national guidelines: Company contributes Rs. 500 CSR portion per intern monthly.</p>
                  </div>
                </div>
              )}

              {/* Post Tab */}
              {activeTab === "post" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Create new internship listing</h2>
                  <form onSubmit={handlePostInternship} className="space-y-4 text-sm">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Internship Title</label>
                        <input
                          type="text"
                          value={title}
                          onChange={(e) => setTitle(e.target.value)}
                          placeholder="e.g. Software Dev Intern"
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Location District</label>
                        <input
                          type="text"
                          value={location}
                          onChange={(e) => setLocation(e.target.value)}
                          placeholder="e.g. Pune"
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Industrial Sector</label>
                        <select
                          value={sector}
                          onChange={(e) => setSector(e.target.value)}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        >
                          {SECTORS.map((s) => (
                            <option key={s} value={s}>{s}</option>
                          ))}
                        </select>
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Seat Capacity</label>
                        <input
                          type="number"
                          min="1"
                          value={capacity}
                          onChange={(e) => setCapacity(parseInt(e.target.value))}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Stipend Share (Company CSR) Rs.</label>
                        <input
                          type="number"
                          min="500"
                          value={stipend}
                          onChange={(e) => setStipend(parseInt(e.target.value))}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Required Skills (Comma separated)</label>
                        <input
                          type="text"
                          value={requiredSkills}
                          onChange={(e) => setRequiredSkills(e.target.value)}
                          placeholder="e.g. Excel, SQL, Python"
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-purple-500"
                        />
                      </div>
                    </div>

                    <div className="flex justify-end pt-2">
                      <button
                        type="submit"
                        className="bg-purple-600 hover:bg-purple-500 text-white rounded-lg px-4 py-2 text-xs font-bold cursor-pointer"
                      >
                        Publish listing
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {/* Capacity Tab */}
              {activeTab === "capacity" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Active listings seat capacity management</h2>
                  <div className="space-y-4">
                    {listings.length === 0 ? (
                      <div className="text-center py-8 text-slate-400 text-sm">No postings published.</div>
                    ) : (
                      listings.map((l) => (
                        <div key={l.id} className="p-4 bg-slate-900/60 border border-white/5 rounded-xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-sm">
                          <div>
                            <h3 className="font-bold text-white text-base">{l.title}</h3>
                            <div className="flex gap-4 text-xs text-slate-500 mt-1">
                              <span>📍 {l.location}</span>
                              <span>💼 {l.sector}</span>
                              <span>💰 Rs. {l.stipendCompanyShare} Co-share</span>
                            </div>
                          </div>
                          <div className="flex items-center gap-3">
                            <span className="text-xs text-slate-400 font-semibold">Seat Cap:</span>
                            <input
                              type="number"
                              min="0"
                              value={l.capacity}
                              onChange={(e) => handleUpdateCapacity(l.id, parseInt(e.target.value))}
                              className="w-16 bg-slate-950 border border-white/10 text-white text-center rounded-lg py-1 text-xs"
                            />
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}

              {/* Joining Tab */}
              {activeTab === "joining" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Proposed interns onboarding verification</h2>
                  <div className="space-y-4">
                    {allocations.length === 0 ? (
                      <div className="text-center py-8 text-slate-400 text-sm">
                        No proposed allocations matched to your listings yet. Run simulation in the Admin Console.
                      </div>
                    ) : (
                      allocations.map((a) => (
                        <div key={a.id} className="p-4 bg-slate-900/60 border border-white/5 rounded-xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 text-sm">
                          <div className="space-y-1">
                            <div className="font-bold text-white text-base">{a.student?.fullName}</div>
                            <div className="text-xs text-purple-400 font-semibold">{a.listing?.title}</div>
                            <div className="text-xs text-slate-500">Proposed Rank Satisfaction: #{a.assignedRank}</div>
                          </div>
                          
                          <div className="flex gap-2">
                            {a.status === "PROPOSED" || a.status === "ACCEPTED" ? (
                              <>
                                <button
                                  onClick={() => handleConfirmJoining(a.listing.id, a.student.id, true)}
                                  className="bg-emerald-600/20 text-emerald-400 border border-emerald-500/20 hover:bg-emerald-600 hover:text-white rounded-lg px-3 py-1.5 text-xs font-semibold cursor-pointer transition-colors"
                                >
                                  ✓ Confirm Onboard
                                </button>
                                <button
                                  onClick={() => handleConfirmJoining(a.listing.id, a.student.id, false)}
                                  className="bg-red-600/20 text-red-400 border border-red-500/20 hover:bg-red-600 hover:text-white rounded-lg px-3 py-1.5 text-xs font-semibold cursor-pointer transition-colors"
                                >
                                  ✗ Report No-Show
                                </button>
                              </>
                            ) : (
                              <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase ${
                                a.status === "JOINED" 
                                  ? "bg-emerald-600/20 text-emerald-400 border border-emerald-500/20" 
                                  : "bg-slate-700 text-slate-400"
                              }`}>
                                {a.status}
                              </span>
                            )}
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}

            </div>
          </div>
        )}
      </div>
    </main>
  );
}
