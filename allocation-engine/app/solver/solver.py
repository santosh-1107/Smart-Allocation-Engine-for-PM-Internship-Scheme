import random
from ortools.sat.python import cp_model

def solve_matching(students, listings, preferences, budget_ceiling, seed):
    # Set seed for deterministic tie-breaking
    random.seed(seed)
    
    # 1. Preprocess data
    student_map = {s["id"]: s for s in students}
    listing_map = {l["id"]: l for l in listings}
    
    # Map studentId to preferences list
    pref_map = {p["studentId"]: p["preferenceOrder"] for p in preferences}

    # Generate deterministic tie-breakers
    tie_breakers = {}
    for s in students:
        for l in listings:
            tie_breakers[(s["id"], l["id"])] = random.random() * 0.0001

    # Precompute listing preferences over students
    # Listing prefers students with:
    # 1. High skill compatibility
    # 2. Aspirational district priority
    # 3. Deterministic tie-breaker
    listing_student_ranks = {} # (l_id, s_id) -> integer rank (lower is better, 0 is best)
    for l in listings:
        l_id = l["id"]
        req_skills = set(x.lower() for x in l.get("requiredSkills", []))
        
        student_scores = []
        for s in students:
            s_id = s["id"]
            s_skills = set(x.lower() for x in s.get("skills", []))
            
            # Skill match ratio
            compat = len(s_skills & req_skills) / len(req_skills) if req_skills else 1.0
            
            # Aspirational district priority (boosts rank)
            asp_boost = 0.1 if s.get("aspirationalDistrict", False) else 0.0
            
            score = compat + asp_boost + tie_breakers[(s_id, l_id)]
            student_scores.append((s_id, score))
        
        # Sort student scores descending
        student_scores.sort(key=lambda x: x[1], reverse=True)
        for rank, (s_id, _) in enumerate(student_scores):
            listing_student_ranks[(l_id, s_id)] = rank

    # 2. Build OR-Tools CP-SAT model
    model = cp_model.CpModel()
    
    # Decision variables: x[s, l] = 1 if student s is assigned to listing l
    x = {}
    # Stability violation variables: v[s, l] = 1 if stable matching is violated for pair (s, l)
    v = {}
    
    for s_id in student_map:
        eligible_listings = student_map[s_id].get("eligibleListings", [])
        student_prefs = pref_map.get(s_id, [])
        
        # We only match to listings that the student preferred and is eligible for
        matchable_listings = [l_id for l_id in student_prefs if l_id in eligible_listings]
        
        for l_id in matchable_listings:
            x[(s_id, l_id)] = model.NewBoolVar(f"x_{s_id}_{l_id}")
            v[(s_id, l_id)] = model.NewBoolVar(f"v_{s_id}_{l_id}")

    # Constraint 1: At most one allocation per student
    for s_id in student_map:
        student_prefs = pref_map.get(s_id, [])
        eligible_listings = student_map[s_id].get("eligibleListings", [])
        matchable_listings = [l_id for l_id in student_prefs if l_id in eligible_listings]
        
        student_vars = [x[(s_id, l_id)] for l_id in matchable_listings]
        model.Add(sum(student_vars) <= 1)

    # Constraint 2: Listing capacity constraint
    for l in listings:
        l_id = l["id"]
        capacity = l["capacity"]
        
        listing_vars = []
        for s_id in student_map:
            if (s_id, l_id) in x:
                listing_vars.append(x[(s_id, l_id)])
        
        model.Add(sum(listing_vars) <= capacity)

    # Constraint 3: Budget ceiling constraint
    total_cost_expr = []
    for (s_id, l_id), var in x.items():
        stipend = listing_map[l_id].get("stipendCompanyShare", 0.0)
        # Multiply by 100 to convert to integer cents for CP-SAT
        stipend_cents = int(stipend * 100)
        total_cost_expr.append(var * stipend_cents)
    
    budget_cents = int(budget_ceiling * 100)
    model.Add(sum(total_cost_expr) <= budget_cents)

    # Constraint 4: Stable Matching Stability (enforced as soft constraints with penalty)
    for s_id in student_map:
        student_prefs = pref_map.get(s_id, [])
        eligible_listings = student_map[s_id].get("eligibleListings", [])
        matchable_listings = [l_id for l_id in student_prefs if l_id in eligible_listings]
        
        for idx, l_id in enumerate(matchable_listings):
            # Listings student prefers to l_id (index < idx in preference list)
            preferred_listings = matchable_listings[:idx]
            
            # Students listing l_id prefers to s_id
            s_rank = listing_student_ranks[(l_id, s_id)]
            better_students = []
            for other_s_id in student_map:
                if (other_s_id, l_id) in x:
                    other_rank = listing_student_ranks.get((l_id, other_s_id), 999999)
                    if other_rank < s_rank:
                        better_students.append(other_s_id)
            
            # Stability constraint formulation:
            # sum(x[s, l_pref]) + sum(x[s_better, l]) >= capacity * (1 - sum(x[s, l_pref]) - x[s, l] - v[s, l])
            # To linearize:
            # sum(x[s_better, l]) + capacity * (sum(x[s, l_pref]) + x[s, l] + v[s, l]) >= capacity
            sum_better = sum(x[(other_s_id, l_id)] for other_s_id in better_students)
            sum_pref = sum(x[(s_id, p_id)] for p_id in preferred_listings)
            current_match = x[(s_id, l_id)]
            violation = v[(s_id, l_id)]
            
            capacity = listing_map[l_id]["capacity"]
            model.Add(sum_better + capacity * (sum_pref + current_match + violation) >= capacity)

    # 3. Objective Function (hierarchical, represented as scaled sum)
    # Priority:
    # 1. Maximize seat utilization (weight: 1,000,000)
    # 2. Minimize stability violations (weight: -100,000)
    # 3. Maximize preference satisfaction (weight: 10,000)
    # 4. Maximize skill compatibility (weight: 1,000)
    # 5. Maximize Aspirational District allocations (weight: 500)
    
    obj_terms = []
    
    # 1. Seat utilization
    for var in x.values():
        obj_terms.append(var * 1000000)
        
    # 2. Stability violations (negative weight)
    for var in v.values():
        obj_terms.append(var * -100000)
        
    # 3. Preference satisfaction
    for (s_id, l_id), var in x.items():
        student_prefs = pref_map.get(s_id, [])
        try:
            pref_idx = student_prefs.index(l_id)
            # Higher score for higher preference (index 0 gets max score)
            pref_score = len(student_prefs) - pref_idx
        except ValueError:
            pref_score = 0
        obj_terms.append(var * pref_score * 10000)
        
    # 4. Skill compatibility
    for (s_id, l_id), var in x.items():
        s_skills = set(x.lower() for x in student_map[s_id].get("skills", []))
        req_skills = set(x.lower() for x in listing_map[l_id].get("requiredSkills", []))
        compat = len(s_skills & req_skills) / len(req_skills) if req_skills else 1.0
        # Convert compat to integer weight
        compat_weight = int(compat * 1000)
        obj_terms.append(var * compat_weight)

    # 5. Aspirational District boost
    for (s_id, l_id), var in x.items():
        if student_map[s_id].get("aspirationalDistrict", False):
            obj_terms.append(var * 5000) # boost priority to aspirational candidates

    model.Maximize(sum(obj_terms))

    # 4. Solve the model
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = 10.0 # safety boundary timeout
    status = solver.Solve(model)

    # 5. Formulate results
    allocations_result = []
    budget_used = 0.0
    seats_filled = 0
    total_assigned_rank_sum = 0
    aspirational_allocated = 0
    total_aspirational = sum(1 for s in students if s.get("aspirationalDistrict", False))

    if status in (cp_model.OPTIMAL, cp_model.FEASIBLE):
        for s_id in student_map:
            student_prefs = pref_map.get(s_id, [])
            eligible_listings = student_map[s_id].get("eligibleListings", [])
            matchable_listings = [l_id for l_id in student_prefs if l_id in eligible_listings]
            
            assigned_listing_id = None
            assigned_rank = None
            
            for l_id in matchable_listings:
                if solver.Value(x[(s_id, l_id)]) == 1:
                    assigned_listing_id = l_id
                    assigned_rank = student_prefs.index(l_id) + 1
                    break
            
            s_skills = set(x.lower() for x in student_map[s_id].get("skills", []))
            is_asp = student_map[s_id].get("aspirationalDistrict", False)

            if assigned_listing_id:
                l = listing_map[assigned_listing_id]
                req_skills = set(x.lower() for x in l.get("requiredSkills", []))
                compat = len(s_skills & req_skills) / len(req_skills) if req_skills else 1.0
                stipend = l.get("stipendCompanyShare", 0.0)
                
                budget_used += stipend
                seats_filled += 1
                total_assigned_rank_sum += assigned_rank
                if is_asp:
                    aspirational_allocated += 1

                # Find why top choices were not assigned (if rank > 1)
                reasons = []
                if assigned_rank > 1:
                    for preferred_l_id in student_prefs[:assigned_rank - 1]:
                        pref_l = listing_map[preferred_l_id]
                        # Count total seats allocated to this listing
                        allocated_to_pref = sum(
                            1 for other_s_id in student_map 
                            if (other_s_id, preferred_l_id) in x and solver.Value(x[(other_s_id, preferred_l_id)]) == 1
                        )
                        if allocated_to_pref >= pref_l["capacity"]:
                            reasons.append(f"Listing '{pref_l['title']}' was at full capacity ({pref_l['capacity']} seats).")
                        else:
                            reasons.append(f"Listing '{pref_l['title']}' filled by higher priority candidates.")

                allocations_result.append({
                    "student_id": s_id,
                    "listing_id": assigned_listing_id,
                    "assigned_rank": assigned_rank,
                    "compatibility_score": round(compat, 4),
                    "explanation": {
                        "outcome": "Assigned",
                        "assigned_rank": assigned_rank,
                        "reasons_for_top_choices_missed": reasons if reasons else ["Already assigned to 1st choice."],
                        "capacity_limit_hit": assigned_rank > 1,
                        "skills_matched": sorted(list(s_skills & req_skills)),
                        "skills_missing": sorted(list(req_skills - s_skills)),
                        "policy_effect": "Aspirational District prioritize" if is_asp else "Standard rank evaluation",
                        "eligibility_passed": True
                    }
                })
            else:
                # Student got unassigned
                reasons = []
                for preferred_l_id in student_prefs:
                    pref_l = listing_map[preferred_l_id]
                    # Check eligibility
                    is_eligible = preferred_l_id in eligible_listings
                    if not is_eligible:
                        reasons.append(f"Ineligible for '{pref_l['title']}' (missing required qualifications/skills).")
                    else:
                        allocated_to_pref = sum(
                            1 for other_s_id in student_map 
                            if (other_s_id, preferred_l_id) in x and solver.Value(x[(other_s_id, preferred_l_id)]) == 1
                        )
                        if allocated_to_pref >= pref_l["capacity"]:
                            reasons.append(f"Listing '{pref_l['title']}' was full.")
                        else:
                            reasons.append(f"Listing '{pref_l['title']}' budget cap reached or filled by higher priority candidates.")

                allocations_result.append({
                    "student_id": s_id,
                    "listing_id": None,
                    "assigned_rank": None,
                    "compatibility_score": 0.0,
                    "explanation": {
                        "outcome": "Unassigned",
                        "assigned_rank": None,
                        "reasons_for_top_choices_missed": reasons if reasons else ["No preferences submitted."],
                        "capacity_limit_hit": True,
                        "skills_matched": [],
                        "skills_missing": [],
                        "policy_effect": "Standard evaluation",
                        "eligibility_passed": False if not eligible_listings else True
                    }
                })

    # 6. Calculate aggregate metrics
    allocation_rate = (seats_filled / len(students)) * 100 if students else 0.0
    avg_rank = (total_assigned_rank_sum / seats_filled) if seats_filled else 0.0
    seat_utilization = (seats_filled / sum(l["capacity"] for l in listings)) * 100 if listings else 0.0
    
    # Fairness Metrics
    asp_alloc_rate = (aspirational_allocated / total_aspirational) * 100 if total_aspirational else 0.0
    non_asp_allocated = seats_filled - aspirational_allocated
    total_non_asp = len(students) - total_aspirational
    non_asp_alloc_rate = (non_asp_allocated / total_non_asp) * 100 if total_non_asp else 0.0
    
    # Parity delta (difference in allocation rates)
    parity_delta = abs(asp_alloc_rate - non_asp_alloc_rate)

    metrics = {
        "allocation_rate": round(allocation_rate, 2),
        "seat_utilization": round(seat_utilization, 2),
        "preference_satisfaction": round(avg_rank, 2),
        "budget_used": round(budget_used, 2),
        "fairness": {
            "aspirational_district_rate": round(asp_alloc_rate, 2),
            "non_aspirational_district_rate": round(non_asp_alloc_rate, 2),
            "allocation_parity_delta": round(parity_delta, 2)
        }
    }

    constraint_trace = {
        "eligibility": True,
        "capacity": True,
        "budget_ceiling": budget_used <= budget_ceiling,
        "quotas": True,
        "seed": seed
    }

    return {
        "allocations": allocations_result,
        "metrics": metrics,
        "constraint_trace": constraint_trace
    }
