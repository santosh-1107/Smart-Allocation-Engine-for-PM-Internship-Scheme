from app.solver.solver import solve_matching

def test_solver_constraints_and_reproducibility():
    students = [
        {
            "id": "s1", 
            "fullName": "Student 1", 
            "skills": ["Python", "SQL"], 
            "district": "Mumbai", 
            "aspirationalDistrict": False, 
            "category": "GENERAL", 
            "eligibleListings": ["l1", "l2"]
        },
        {
            "id": "s2", 
            "fullName": "Student 2", 
            "skills": ["Java"], 
            "district": "Gadchiroli", 
            "aspirationalDistrict": True, 
            "category": "GENERAL", 
            "eligibleListings": ["l1", "l2"]
        }
    ]
    
    listings = [
        {
            "id": "l1", 
            "companyId": "c1", 
            "companyName": "Company A", 
            "title": "Developer", 
            "capacity": 1, 
            "stipendCompanyShare": 1000.0, 
            "location": "Mumbai", 
            "sector": "IT", 
            "requiredSkills": ["Python"]
        },
        {
            "id": "l2", 
            "companyId": "c2", 
            "companyName": "Company B", 
            "title": "Analyst", 
            "capacity": 1, 
            "stipendCompanyShare": 1500.0, 
            "location": "Gadchiroli", 
            "sector": "IT", 
            "requiredSkills": ["Java"]
        }
    ]
    
    preferences = [
        {"studentId": "s1", "preferenceOrder": ["l1", "l2"]},
        {"studentId": "s2", "preferenceOrder": ["l2", "l1"]}
    ]
    
    # Test budget ceiling that allows both matches (1000 + 1500 = 2500 <= 3000)
    result1 = solve_matching(students, listings, preferences, budget_ceiling=3000.0, seed=42)
    assert result1["metrics"]["allocation_rate"] == 100.0
    assert result1["metrics"]["budget_used"] == 2500.0
    
    alloc_map1 = {a["student_id"]: a["listing_id"] for a in result1["allocations"]}
    assert alloc_map1["s1"] == "l1"
    assert alloc_map1["s2"] == "l2"
    
    # Test budget ceiling that only allows ONE (budget = 1200, so only l1 can match at 1000)
    result2 = solve_matching(students, listings, preferences, budget_ceiling=1200.0, seed=42)
    assert result2["metrics"]["budget_used"] <= 1200.0
    allocs2 = [a for a in result2["allocations"] if a["listing_id"] is not None]
    assert len(allocs2) == 1
    
    # Test seed reproducibility
    result1_retry = solve_matching(students, listings, preferences, budget_ceiling=3000.0, seed=42)
    assert result1["allocations"] == result1_retry["allocations"]
