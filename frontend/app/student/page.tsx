"use client";

import React, { useState, useEffect } from "react";

const BACKEND_URL = "http://localhost:8080";
const CYCLE_ID = "00000000-0000-0000-0000-000000000001";
const SKILL_POOL = ["Java", "Python", "SQL", "Excel", "Marketing", "Finance", "Accounting", "React", "Node.js", "Tally", "Communication", "Machine Learning"];

export default function StudentPage() {
  const [students, setStudents] = useState<any[]>([]);
  const [selectedStudentId, setSelectedStudentId] = useState<string>("");
  const [profile, setProfile] = useState<any>(null);
  const [activeTab, setActiveTab] = useState<string>("profile");
  
  // Data lists
  const [listings, setListings] = useState<any[]>([]);
  const [preferences, setPreferences] = useState<any>(null);
  const [outcome, setOutcome] = useState<any>(null);

  // Pairwise Elicitation state
  const [pairwiseState, setPairwiseState] = useState<any>(null);

  // Counterfactual state
  const [cfField, setCfField] = useState<string>("skill");
  const [cfValue, setCfValue] = useState<string>("");
  const [cfResult, setCfResult] = useState<any>(null);
  const [cfLoading, setCfLoading] = useState<boolean>(false);

  // UI status
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string>("");
  const [success, setSuccess] = useState<string>("");

  // Load seed students list on startup
  useEffect(() => {
    fetchStudents();
    fetchListings();
  }, []);

  // Fetch student profile, preferences, outcome whenever student is changed
  useEffect(() => {
    if (selectedStudentId) {
      fetchStudentData();
    } else {
      setProfile(null);
      setPreferences(null);
      setOutcome(null);
    }
  }, [selectedStudentId]);

  const fetchStudents = async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/policy/documents`); // documents endpoint to check API connection, or just get students from a mock
      // Since we don't have a direct "list all students" endpoint, let's fetch internships and get student seed via a fallback,
      // or we can invoke our Spring Boot API. Since we seeded 1000 students, we can also query the API or hardcode some seed IDs to toggle.
      // Let's call the API to fetch listings, which is public.
      // To get student list, we can write a simple endpoint or fetch a list. Since we seeded 1000 students starting with name "Amit Sharma 1",
      // we can fetch some profiles or simulate a lookup. Let's see: we can fetch listings to verify API connection.
      // Let's fetch some student IDs. Since we seeded, we can use a lookup or fetch a simulated list from local profiles.
      // Wait, let's look at the database seeder: it creates 1000 students. We can fetch listings first.
      // To get student details, let's query the backend or fallback to seeded UUIDs.
      // Let's seed 3 specific UUIDs for the frontend demo:
      // Student 1 (Regular, Satara): "00000000-0000-0000-0000-000000000001" (Wait, they are random in seeder, but we can write a simple custom endpoint or search by name. Let's make an API request to GET /api/v1/exceptions or policy to get student ID, or write a quick endpoint to search or fetch seed IDs).
      // Actually, since we can't search, let's query listings. Let's look up applications or write a fetch to a new endpoint `/api/v1/students` if we want.
      // Wait! We can add a simple GET /api/v1/students endpoint in StudentController.java to list the first 50 seeded students!
      // This is an excellent idea and makes the dropdown dynamically populated from the database seeder!
      // Let's modify StudentController.java to add a GET /api/v1/students endpoint later or do it now.
      // For now, let's write the code assuming there is a GET /api/v1/students endpoint.
      const listingsRes = await fetch(`${BACKEND_URL}/api/v1/internships?all=true`);
      const listingsData = await listingsRes.json();
      setListings(listingsData);

      // Fetch students list (we will implement this endpoint in StudentController)
      const studentsRes = await fetch(`${BACKEND_URL}/api/v1/students`);
      if (studentsRes.ok) {
        const studentsData = await studentsRes.json();
        setStudents(studentsData);
        if (studentsData.length > 0) {
          setSelectedStudentId(studentsData[0].id);
        }
      }
    } catch (err) {
      console.error("Failed to fetch startup data", err);
      // Fallback seed list if backend is offline
      const mockSeeds = [
        { id: "e102f4e4-7d5a-4467-bc5b-4328b975e510", fullName: "Amit Sharma (Satara - General)" },
        { id: "f205c5a2-9d3b-4171-8b4e-4f38e671b420", fullName: "Priya Patil (Gadchiroli - Aspirational SC - eKYC Verified)" },
        { id: "00000000-0000-0000-0000-000000000042", fullName: "Rahul Deshmukh (Washim - Aspirational ST - eKYC Degraded Case)" }
      ];
      setStudents(mockSeeds);
      setSelectedStudentId(mockSeeds[0].id);
    }
  };

  const fetchListings = async () => {
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/internships`);
      if (res.ok) {
        const data = await res.json();
        setListings(data);
      }
    } catch (e) {}
  };

  const fetchStudentData = async () => {
    setLoading(true);
    setError("");
    setSuccess("");
    setCfResult(null);
    setPairwiseState(null);
    try {
      // 1. Fetch profile
      // In our design, profile is fetched using the X-User-Id header
      const pRes = await fetch(`${BACKEND_URL}/api/v1/students/profile`, {
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      // If profile doesn't exist, we can create/save a dummy one
      if (pRes.ok) {
        const pData = await pRes.json();
        setProfile(pData);
      }

      // 2. Fetch preferences
      const prefRes = await fetch(`${BACKEND_URL}/api/v1/preferences/status?cycleId=${CYCLE_ID}`, {
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      if (prefRes.ok) {
        const prefData = await prefRes.json();
        setPreferences(prefData);
      } else {
        setPreferences(null);
      }

      // 3. Fetch outcome
      const outcomeRes = await fetch(`${BACKEND_URL}/api/v1/allocation/status`, {
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      if (outcomeRes.ok) {
        const outData = await outcomeRes.json();
        setOutcome(outData);
      } else {
        setOutcome(null);
      }

    } catch (err: any) {
      setError("Failed to load student dashboard: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  // Submit profile updates (eKYC simulation)
  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/students/profile`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        },
        body: JSON.stringify({
          fullName: profile.student.fullName,
          phone: profile.student.phone,
          preferredLanguage: profile.student.preferredLanguage,
          district: profile.student.district,
          aspirationalDistrict: profile.student.aspirationalDistrict,
          category: profile.category,
          gender: profile.gender,
          dob: profile.dob,
          failEkyc: !profile.ekycVerified // toggle
        })
      });
      if (res.ok) {
        const data = await res.json();
        setProfile(data);
        setSuccess("Profile updated successfully!");
        fetchStudentData();
      } else {
        const msg = await res.text();
        setError("Update failed: " + msg);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Pairwise elicitation flow control
  const handlePairwiseAction = async (action: string, chosenId?: string, otherId?: string) => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/preferences/pairwise`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        },
        body: JSON.stringify({
          cycleId: CYCLE_ID,
          action: action,
          chosenId: chosenId,
          otherId: otherId
        })
      });
      if (res.ok) {
        const data = await res.json();
        if (data.status === "COMPLETED") {
          setPairwiseState(null);
          setSuccess("Pairwise ranking completed! Preferences saved.");
          fetchStudentData();
        } else {
          setPairwiseState(data);
        }
      } else {
        const text = await res.text();
        setError("Pairwise step failed: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Counterfactual evaluation trigger
  const handleRunCounterfactual = async () => {
    if (!cfValue) {
      setError("Please specify a change value (e.g. skill name like Java)");
      return;
    }
    setCfLoading(true);
    setError("");
    setCfResult(null);
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/allocation/counterfactual?field=${cfField}&value=${cfValue}`, {
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      if (res.ok) {
        const data = await res.json();
        setCfResult(data);
      } else {
        const text = await res.text();
        setError("Counterfactual replay failed: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setCfLoading(false);
    }
  };

  // Accept allocation offer
  const handleAcceptOffer = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/allocation/accept`, {
        method: "POST",
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      if (res.ok) {
        setSuccess("Offer accepted successfully!");
        fetchStudentData();
      } else {
        const text = await res.text();
        setError("Failed to accept offer: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // Reject allocation offer
  const handleRejectOffer = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await fetch(`${BACKEND_URL}/api/v1/allocation/reject`, {
        method: "POST",
        headers: {
          "X-User-Role": "STUDENT",
          "X-User-Id": selectedStudentId
        }
      });
      if (res.ok) {
        setSuccess("Offer rejected. Waitlist promotions triggered.");
        fetchStudentData();
      } else {
        const text = await res.text();
        setError("Failed to reject offer: " + text);
      }
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="min-h-screen p-6 bg-slate-950 text-slate-100">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-6 glass-panel border-white/5">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              <span>🎓</span> Student Portal
            </h1>
            <p className="text-xs text-slate-400">Manage preferences, verify eligibility, and view allocations.</p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-xs text-slate-400 font-medium">Profile Context:</span>
            <select
              value={selectedStudentId}
              onChange={(e) => setSelectedStudentId(e.target.value)}
              className="bg-slate-900/80 border border-white/10 text-white rounded-lg px-3 py-1.5 text-xs focus:ring-1 focus:ring-indigo-500 outline-none"
            >
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.fullName || s.student?.fullName}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* System messages */}
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

        {/* Main layout */}
        {loading && !pairwiseState && (
          <div className="text-center py-12 text-slate-400 text-sm">Loading student dashboard...</div>
        )}

        {!loading && profile && (
          <div className="grid gap-6 md:grid-cols-4">
            
            {/* Sidebar navigation */}
            <div className="md:col-span-1 space-y-2">
              <button
                onClick={() => setActiveTab("profile")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "profile" ? "bg-indigo-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                👤 Profile & Aadhaar eKYC
              </button>
              <button
                onClick={() => setActiveTab("catalog")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "catalog" ? "bg-indigo-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                🔎 Internship Catalogue
              </button>
              <button
                onClick={() => setActiveTab("preferences")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "preferences" ? "bg-indigo-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                📊 Preference Elicitation
              </button>
              <button
                onClick={() => setActiveTab("outcome")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "outcome" ? "bg-indigo-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                🎉 Match Results & Explanation
              </button>
              <button
                onClick={() => setActiveTab("counterfactual")}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-xs font-semibold transition-colors ${
                  activeTab === "counterfactual" ? "bg-indigo-600 text-white" : "hover:bg-slate-900/60 text-slate-400"
                }`}
              >
                🔄 Counterfactual Replay
              </button>
              <div className="pt-4 border-t border-white/5">
                <a
                  href="/"
                  className="block text-center text-xs text-indigo-400 font-semibold hover:underline"
                >
                  ← Back to Home
                </a>
              </div>
            </div>

            {/* Tab content panels */}
            <div className="md:col-span-3">
              
              {/* Profile panel */}
              {activeTab === "profile" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Profile & eKYC verification</h2>
                  <form onSubmit={handleUpdateProfile} className="space-y-4 text-sm">
                    <div className="grid gap-4 sm:grid-cols-2">
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Full Name</label>
                        <input
                          type="text"
                          value={profile.student.fullName}
                          onChange={(e) => setProfile({
                            ...profile,
                            student: { ...profile.student, fullName: e.target.value }
                          })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Phone Number</label>
                        <input
                          type="text"
                          value={profile.student.phone}
                          onChange={(e) => setProfile({
                            ...profile,
                            student: { ...profile.student, phone: e.target.value }
                          })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">District</label>
                        <input
                          type="text"
                          value={profile.student.district}
                          onChange={(e) => setProfile({
                            ...profile,
                            student: { ...profile.student, district: e.target.value }
                          })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Gender</label>
                        <input
                          type="text"
                          value={profile.gender}
                          onChange={(e) => setProfile({ ...profile, gender: e.target.value })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Social Category</label>
                        <select
                          value={profile.category}
                          onChange={(e) => setProfile({ ...profile, category: e.target.value })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        >
                          <option value="GENERAL">GENERAL</option>
                          <option value="OBC">OBC</option>
                          <option value="SC">SC</option>
                          <option value="ST">ST</option>
                        </select>
                      </div>
                      <div>
                        <label className="block text-xs text-slate-400 font-medium mb-1">Preferred language</label>
                        <select
                          value={profile.student.preferredLanguage}
                          onChange={(e) => setProfile({
                            ...profile,
                            student: { ...profile.student, preferredLanguage: e.target.value }
                          })}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500"
                        >
                          <option value="en">English (en)</option>
                          <option value="hi">Hindi (hi)</option>
                          <option value="mr">Marathi (mr)</option>
                        </select>
                      </div>
                    </div>

                    <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl flex items-center justify-between">
                      <div className="space-y-1">
                        <div className="text-xs font-semibold text-white">Aadhaar eKYC status</div>
                        <p className="text-slate-400 text-xs">
                          {profile.ekycVerified 
                            ? "Verified. Your profile is eligible for automatic matching." 
                            : "Degraded. Re-routed to the exceptions queue for manual validation."}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => setProfile({ ...profile, ekycVerified: !profile.ekycVerified })}
                        className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
                          profile.ekycVerified 
                            ? "bg-emerald-600/20 text-emerald-400 border border-emerald-500/30" 
                            : "bg-amber-600/20 text-amber-400 border border-amber-500/30"
                        }`}
                      >
                        {profile.ekycVerified ? "Verified" : "Fail eKYC (Degrade)"}
                      </button>
                    </div>

                    {profile.student.aspirationalDistrict && (
                      <div className="p-3 bg-indigo-950/40 border border-indigo-500/20 text-indigo-300 rounded-lg text-xs font-semibold">
                        📍 Aspirational District Prioritization Active (Quota Floors apply)
                      </div>
                    )}

                    <div className="flex justify-end pt-2">
                      <button
                        type="submit"
                        className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg px-4 py-2 text-xs font-bold shadow-md cursor-pointer"
                      >
                        Save & Verify Profile
                      </button>
                    </div>
                  </form>
                </div>
              )}

              {/* Catalogue panel */}
              {activeTab === "catalog" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Active Internships catalogue</h2>
                  <div className="space-y-4">
                    {listings.length === 0 ? (
                      <div className="text-center py-8 text-slate-400 text-sm">No active listings published yet.</div>
                    ) : (
                      listings.map((l) => (
                        <div key={l.id} className="p-4 bg-slate-900/60 border border-white/5 rounded-xl hover:border-white/10 transition-colors flex flex-col sm:flex-row justify-between gap-4 text-sm">
                          <div className="space-y-2">
                            <div>
                              <h3 className="font-bold text-white text-base">{l.title}</h3>
                              <p className="text-xs text-indigo-400 font-semibold">{l.company?.legalName}</p>
                            </div>
                            <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-400">
                              <span>📍 {l.location}</span>
                              <span>💼 {l.sector}</span>
                              <span>👥 Seats: {l.capacity}</span>
                            </div>
                            <div className="flex flex-wrap gap-1">
                              {l.requiredSkills?.map((s: any) => (
                                <span key={s.id} className="bg-slate-800 text-slate-300 text-[10px] px-2 py-0.5 rounded font-mono">
                                  {s.name}
                                </span>
                              ))}
                            </div>
                          </div>
                          <div className="sm:text-right flex sm:flex-col justify-between items-end gap-2">
                            <span className="text-xs font-bold text-emerald-400">₹ {l.stipendCompanyShare} /mo (Co-share)</span>
                            <span className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase ${
                              l.status === "PUBLISHED" ? "bg-indigo-600/20 text-indigo-400 border border-indigo-500/20" : "bg-slate-700 text-slate-400"
                            }`}>
                              {l.status}
                            </span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}

              {/* Preferences / Pairwise panel */}
              {activeTab === "preferences" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <div className="flex items-center justify-between border-b border-white/5 pb-2">
                    <h2 className="text-lg font-bold text-white">Preference satisfaction</h2>
                    <button
                      onClick={() => handlePairwiseAction("START")}
                      className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg px-3 py-1.5 text-xs font-bold cursor-pointer transition-colors"
                    >
                      ⚡ Start Pairwise Elicitation
                    </button>
                  </div>

                  {pairwiseState ? (
                    <div className="p-6 bg-slate-900/80 border border-white/10 rounded-2xl space-y-6 animate-fade-in">
                      <div className="text-center space-y-1">
                        <div className="text-xs text-indigo-400 font-bold uppercase tracking-wider">Pairwise Comparison Flow</div>
                        <h3 className="text-sm font-medium text-slate-200">Which of these internships do you prefer?</h3>
                        <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden mt-3 max-w-xs mx-auto">
                          <div 
                            className="bg-indigo-500 h-full transition-all duration-300" 
                            style={{ width: `${pairwiseState.progress}%` }}
                          ></div>
                        </div>
                        <div className="text-[10px] text-slate-500 mt-1">Elicitation progress: {pairwiseState.progress}%</div>
                      </div>

                      <div className="grid gap-4 sm:grid-cols-2">
                        <button
                          onClick={() => handlePairwiseAction("COMPARE", pairwiseState.choiceA.id, pairwiseState.choiceB.id)}
                          className="glass-card hover:border-indigo-500/30 text-left p-5 transition-transform hover:scale-[1.02] cursor-pointer"
                        >
                          <div className="text-xs font-semibold text-indigo-400 mb-1">Option A</div>
                          <h4 className="font-bold text-white text-base leading-tight">{pairwiseState.choiceA.title}</h4>
                          <p className="text-xs text-slate-400 mt-1">{pairwiseState.choiceA.company?.legalName}</p>
                          <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-400 border-t border-white/5 pt-2">
                            <span>📍 {pairwiseState.choiceA.location}</span>
                            <span>💼 {pairwiseState.choiceA.sector}</span>
                          </div>
                        </button>

                        <button
                          onClick={() => handlePairwiseAction("COMPARE", pairwiseState.choiceB.id, pairwiseState.choiceA.id)}
                          className="glass-card hover:border-indigo-500/30 text-left p-5 transition-transform hover:scale-[1.02] cursor-pointer"
                        >
                          <div className="text-xs font-semibold text-indigo-400 mb-1">Option B</div>
                          <h4 className="font-bold text-white text-base leading-tight">{pairwiseState.choiceB.title}</h4>
                          <p className="text-xs text-slate-400 mt-1">{pairwiseState.choiceB.company?.legalName}</p>
                          <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-400 border-t border-white/5 pt-2">
                            <span>📍 {pairwiseState.choiceB.location}</span>
                            <span>💼 {pairwiseState.choiceB.sector}</span>
                          </div>
                        </button>
                      </div>

                      <div className="text-center pt-2">
                        <button
                          onClick={() => setPairwiseState(null)}
                          className="text-xs text-slate-500 hover:text-slate-400 font-semibold hover:underline"
                        >
                          Cancel elicitation
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {preferences ? (
                        <div className="space-y-3">
                          <div className="flex items-center justify-between text-xs text-slate-400">
                            <span>Current preference version: <strong>V{preferences.version}</strong></span>
                            <span>Last updated: {new Date(preferences.createdAt).toLocaleString()}</span>
                          </div>
                          <div className="space-y-2">
                            {preferences.preferenceOrder.map((listingId: string, index: number) => {
                              const match = listings.find((l) => l.id === listingId);
                              return (
                                <div key={listingId} className="flex items-center gap-4 p-3 bg-slate-900/60 border border-white/5 rounded-lg text-sm">
                                  <div className="w-6 h-6 rounded-full bg-indigo-950 border border-indigo-500/20 flex items-center justify-center text-xs font-bold text-indigo-400">
                                    {index + 1}
                                  </div>
                                  <div>
                                    {match ? (
                                      <div>
                                        <span className="font-bold text-white">{match.title}</span>
                                        <span className="text-xs text-slate-400 ml-2">({match.company?.legalName} - {match.location})</span>
                                      </div>
                                    ) : (
                                      <span className="text-slate-500">Loading Listing {listingId}...</span>
                                    )}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      ) : (
                        <div className="p-8 text-center bg-slate-900/60 border border-white/5 rounded-xl space-y-4">
                          <p className="text-slate-400 text-sm">No preferences submitted for this cycle yet.</p>
                          <button
                            onClick={() => handlePairwiseAction("START")}
                            className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg px-4 py-2 text-xs font-bold cursor-pointer"
                          >
                            Set preferences via Pairwise sorting
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}

              {/* Outcome panel */}
              {activeTab === "outcome" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <h2 className="text-lg font-bold text-white border-b border-white/5 pb-2">Match result & explanation</h2>
                  {outcome ? (
                    <div className="space-y-6 text-sm">
                      <div className="p-6 bg-slate-900/80 border border-white/10 rounded-2xl flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                        <div className="space-y-1">
                          <div className="text-xs text-indigo-400 font-bold uppercase">Proposed assignment</div>
                          <h3 className="text-xl font-bold text-white">{outcome.assignedListing || "Unassigned"}</h3>
                          <p className="text-xs text-slate-400">{outcome.assignedCompany}</p>
                        </div>
                        <div className="flex flex-col items-end gap-2">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold uppercase ${
                            outcome.status === "ACCEPTED" || outcome.status === "JOINED"
                              ? "bg-emerald-600/20 text-emerald-400 border border-emerald-500/20"
                              : "bg-indigo-600/20 text-indigo-400 border border-indigo-500/20"
                          }`}>
                            {outcome.status}
                          </span>
                          {outcome.assignedRank && (
                            <span className="text-xs text-slate-400">Preference Satisfaction Rank: #{outcome.assignedRank}</span>
                          )}
                        </div>
                      </div>

                      {/* Accept/Reject buttons */}
                      {outcome.status === "PROPOSED" && (
                        <div className="flex gap-4">
                          <button
                            onClick={handleAcceptOffer}
                            className="flex-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg py-2.5 text-xs font-bold shadow-md cursor-pointer transition-colors"
                          >
                            ✓ Accept Offer
                          </button>
                          <button
                            onClick={handleRejectOffer}
                            className="flex-1 bg-red-600 hover:bg-red-500 text-white rounded-lg py-2.5 text-xs font-bold shadow-md cursor-pointer transition-colors"
                          >
                            ✗ Decline Offer
                          </button>
                        </div>
                      )}

                      {/* Plain language explanation */}
                      <div className="space-y-3">
                        <h4 className="font-bold text-white text-base">Why was I assigned here?</h4>
                        <div className="p-4 bg-slate-900/60 border border-white/5 rounded-xl space-y-4">
                          <p className="text-slate-300 leading-relaxed">
                            {outcome.status === "UNASSIGNED" 
                              ? "You were unassigned during this pass because of budget ceilings or capacity saturations. Your eligibility status was verified, and your records will automatically join the priority reallocation waitlist queue."
                              : `You were matched to '${outcome.assignedListing}' (${outcome.assignedCompany}), which was your choice #${outcome.assignedRank} in your preference submission list.`
                            }
                          </p>
                          
                          {outcome.trace && (
                            <div className="space-y-3 border-t border-white/5 pt-3">
                              {outcome.trace.reasons_for_top_choices_missed && outcome.trace.reasons_for_top_choices_missed.length > 0 && (
                                <div className="space-y-1">
                                  <span className="text-xs text-slate-500 font-semibold block uppercase">Cutoff details for higher choices:</span>
                                  <ul className="list-disc pl-4 text-xs text-slate-400 space-y-1">
                                    {outcome.trace.reasons_for_top_choices_missed.map((reason: string, idx: number) => (
                                      <li key={idx}>{reason}</li>
                                    ))}
                                  </ul>
                                </div>
                              )}

                              <div className="grid gap-3 sm:grid-cols-2 text-xs text-slate-400">
                                <div>
                                  <span className="text-slate-500 font-semibold block">Skill compatibility match:</span>
                                  <span>{outcome.compatibilityScore ? (outcome.compatibilityScore * 100).toFixed(0) : 0}% overlap</span>
                                </div>
                                <div>
                                  <span className="text-slate-500 font-semibold block">Policy priority factors:</span>
                                  <span>{outcome.trace.policy_effect || "None applied"}</span>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-8 text-slate-400 text-sm">
                      Matching calculations have not been committed yet. Trigger simulation in the Admin Console.
                    </div>
                  )}
                </div>
              )}

              {/* Counterfactual panel */}
              {activeTab === "counterfactual" && (
                <div className="p-6 glass-panel border-white/5 space-y-6">
                  <div>
                    <h2 className="text-lg font-bold text-white">Counterfactual replay solver</h2>
                    <p className="text-xs text-slate-400 mt-1">
                      Change a parameter (e.g. add a skill or shift a preference rank) and see how the matching engine recalculates outcomes.
                    </p>
                  </div>

                  <div className="grid gap-4 sm:grid-cols-3 items-end text-sm">
                    <div>
                      <label className="block text-xs text-slate-400 font-medium mb-1">What to change?</label>
                      <select
                        value={cfField}
                        onChange={(e) => setCfField(e.target.value)}
                        className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500 text-xs"
                      >
                        <option value="skill">Add missing skill</option>
                        <option value="preference">Move listing to 1st preference</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-xs text-slate-400 font-medium mb-1">Change value</label>
                      {cfField === "skill" ? (
                        <select
                          value={cfValue}
                          onChange={(e) => setCfValue(e.target.value)}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500 text-xs"
                        >
                          <option value="">-- Choose Skill --</option>
                          {SKILL_POOL.map((s) => (
                            <option key={s} value={s}>{s}</option>
                          ))}
                        </select>
                      ) : (
                        <select
                          value={cfValue}
                          onChange={(e) => setCfValue(e.target.value)}
                          className="w-full bg-slate-900 border border-white/10 rounded-lg px-3 py-2 text-white outline-none focus:border-indigo-500 text-xs"
                        >
                          <option value="">-- Choose Listing --</option>
                          {listings.map((l) => (
                            <option key={l.id} value={l.id}>{l.title} ({l.company?.legalName})</option>
                          ))}
                        </select>
                      )}
                    </div>

                    <button
                      onClick={handleRunCounterfactual}
                      disabled={cfLoading}
                      className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg py-2.5 text-xs font-bold cursor-pointer disabled:bg-slate-700 disabled:text-slate-400"
                    >
                      {cfLoading ? "Re-running Solver..." : "Run Live Replay"}
                    </button>
                  </div>

                  {cfResult && (
                    <div className="p-6 bg-slate-900/80 border border-white/10 rounded-2xl space-y-4 animate-fade-in text-sm">
                      <div className="flex items-center gap-2">
                        <span className={`w-2 h-2 rounded-full ${cfResult.outcomeChanged ? "bg-amber-400 animate-pulse" : "bg-slate-500"}`}></span>
                        <span className="text-xs font-bold uppercase text-slate-400">Simulation result</span>
                      </div>
                      
                      <div className="grid gap-4 sm:grid-cols-2">
                        <div className="p-4 bg-slate-950/60 border border-white/5 rounded-xl space-y-1">
                          <span className="text-[10px] text-slate-500 block uppercase font-semibold">Original match</span>
                          <div className="font-bold text-slate-300">{cfResult.originalAllocation.title}</div>
                          <div className="text-xs text-slate-500">{cfResult.originalAllocation.company}</div>
                        </div>

                        <div className="p-4 bg-slate-950/60 border border-indigo-500/20 rounded-xl space-y-1">
                          <span className="text-[10px] text-indigo-400 block uppercase font-semibold">Counterfactual match</span>
                          <div className="font-bold text-white">{cfResult.counterfactualAllocation.title}</div>
                          <div className="text-xs text-slate-400">{cfResult.counterfactualAllocation.company}</div>
                        </div>
                      </div>

                      <div className="p-4 bg-indigo-950/30 border border-indigo-500/10 text-indigo-300 rounded-xl leading-relaxed">
                        {cfResult.explanation}
                      </div>
                    </div>
                  )}
                </div>
              )}
              
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
