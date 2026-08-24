"use client";

import React from "react";

export default function Home() {
  return (
    <main className="min-h-screen flex items-center justify-center p-6 bg-slate-950 relative overflow-hidden">
      {/* Decorative ambient glowing background circles */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-900/30 rounded-full blur-[120px] pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple-900/30 rounded-full blur-[120px] pointer-events-none"></div>

      <section className="relative z-10 w-full max-w-5xl mx-auto text-center space-y-8 animate-fade-in">
        <div className="space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-950/60 border border-indigo-500/20 text-indigo-400 text-xs font-semibold tracking-wider uppercase backdrop-blur-md">
            SIH 2026 Production Prototype
          </div>
          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white">
            Smart Allocation <span className="text-gradient-purple">Engine</span>
          </h1>
          <p className="max-w-2xl mx-auto text-slate-400 text-base sm:text-lg">
            High-integrity stable matching, deterministic policy constraint reconciliation, explainability traces, and audit logs verification for the PM Internship Scheme.
          </p>
        </div>

        <div className="grid gap-6 sm:grid-cols-3 max-w-4xl mx-auto pt-6">
          <a
            href="/student"
            className="glass-panel group flex flex-col justify-between p-6 text-left border border-white/5 hover:border-indigo-500/30 transition-all duration-300"
          >
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-lg bg-indigo-600/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400 group-hover:scale-110 transition-transform">
                🎓
              </div>
              <h3 className="text-xl font-bold text-white group-hover:text-indigo-400 transition-colors">Student Portal</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Submit preferences, engage in pairwise sorting, inspect matching explanations, and test counterfactual outcomes.
              </p>
            </div>
            <div className="mt-6 text-xs text-indigo-400 font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Enter portal <span>→</span>
            </div>
          </a>

          <a
            href="/company"
            className="glass-panel group flex flex-col justify-between p-6 text-left border border-white/5 hover:border-purple-500/30 transition-all duration-300"
          >
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-lg bg-purple-600/20 border border-purple-500/30 flex items-center justify-center text-purple-400 group-hover:scale-110 transition-transform">
                🏢
              </div>
              <h3 className="text-xl font-bold text-white group-hover:text-purple-400 transition-colors">Company Recruiter</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Post internships, adjust capacity limits, confirm joining, and track onboarding reliability scores.
              </p>
            </div>
            <div className="mt-6 text-xs text-purple-400 font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Enter portal <span>→</span>
            </div>
          </a>

          <a
            href="/admin"
            className="glass-panel group flex flex-col justify-between p-6 text-left border border-white/5 hover:border-amber-500/30 transition-all duration-300"
          >
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-lg bg-amber-600/20 border border-amber-500/30 flex items-center justify-center text-amber-400 group-hover:scale-110 transition-transform">
                🛡️
              </div>
              <h3 className="text-xl font-bold text-white group-hover:text-amber-400 transition-colors">National Admin</h3>
              <p className="text-sm text-slate-400 leading-relaxed">
                Trigger simulations, compare side-by-side scenarios, review exception queues, and verify audit log hash chains.
              </p>
            </div>
            <div className="mt-6 text-xs text-amber-400 font-semibold flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Enter console <span>→</span>
            </div>
          </a>
        </div>
      </section>
    </main>
  );
}
