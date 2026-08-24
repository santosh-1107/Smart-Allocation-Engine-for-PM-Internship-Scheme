package in.pmis.modules.allocations;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.pmis.modules.students.Student;
import in.pmis.modules.students.StudentRepository;
import in.pmis.modules.students.StudentProfile;
import in.pmis.modules.students.StudentProfileRepository;
import in.pmis.modules.internships.InternshipListing;
import in.pmis.modules.internships.InternshipListingRepository;
import in.pmis.modules.preferences.Preference;
import in.pmis.modules.preferences.PreferenceRepository;
import in.pmis.modules.eligibility.EligibilityRecord;
import in.pmis.modules.eligibility.EligibilityRecordRepository;
import in.pmis.modules.notifications.NotificationService;
import in.pmis.modules.audit.AuditService;
import in.pmis.modules.waitlist.WaitlistEntry;
import in.pmis.modules.waitlist.WaitlistEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllocationService {

    @Autowired
    private AllocationRunRepository allocationRunRepository;

    @Autowired
    private AllocationResultRepository allocationResultRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private InternshipListingRepository internshipListingRepository;

    @Autowired
    private PreferenceRepository preferenceRepository;

    @Autowired
    private EligibilityRecordRepository eligibilityRecordRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Value("${services.allocation-engine:http://localhost:8001}")
    private String allocationEngineUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> simulateOrRunAllocation(UUID cycleId, BigDecimal budgetCeiling, long seed, boolean commitToApprovalQueue) {
        // 1. Gather all student profiles, skills, and preferences
        List<Student> students = studentRepository.findAll();
        List<InternshipListing> listings = internshipListingRepository.findByStatus("PUBLISHED");
        List<Preference> preferences = preferenceRepository.findByCycleId(cycleId);
        List<EligibilityRecord> eligibilities = eligibilityRecordRepository.findAll();

        // Build mapping of student to profile
        Map<UUID, StudentProfile> profileMap = studentProfileRepository.findAll().stream()
                .collect(Collectors.toMap(StudentProfile::getStudentId, p -> p, (a, b) -> a));

        // Group eligibility by student
        Map<UUID, List<UUID>> studentEligibilityMap = new HashMap<>();
        for (EligibilityRecord er : eligibilities) {
            if (er.getEligible()) {
                studentEligibilityMap.computeIfAbsent(er.getStudent().getId(), k -> new ArrayList<>())
                        .add(er.getListing().getId());
            }
        }

        // Construct input snapshot payload
        Map<String, Object> solverPayload = new HashMap<>();
        solverPayload.put("cycle_id", cycleId.toString());
        solverPayload.put("budget_ceiling", budgetCeiling.doubleValue());
        solverPayload.put("seed", seed);

        // Serialize students list
        List<Map<String, Object>> studentPayloads = new ArrayList<>();
        for (Student s : students) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId().toString());
            map.put("fullName", s.getFullName());
            map.put("skills", s.getSkills().stream().map(skill -> skill.getName()).collect(Collectors.toList()));
            map.put("district", s.getDistrict());
            map.put("aspirationalDistrict", s.getAspirationalDistrict());
            
            StudentProfile sp = profileMap.get(s.getId());
            map.put("category", sp != null ? sp.getCategory() : "GENERAL");
            map.put("gender", sp != null ? sp.getGender() : "UNKNOWN");
            map.put("eligibleListings", studentEligibilityMap.getOrDefault(s.getId(), Collections.emptyList()));
            studentPayloads.add(map);
        }
        solverPayload.put("students", studentPayloads);

        // Serialize listings list
        List<Map<String, Object>> listingPayloads = new ArrayList<>();
        for (InternshipListing l : listings) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", l.getId().toString());
            map.put("companyId", l.getCompany().getId().toString());
            map.put("companyName", l.getCompany().getLegalName());
            map.put("title", l.getTitle());
            map.put("capacity", l.getCapacity());
            map.put("stipendCompanyShare", l.getStipendCompanyShare().doubleValue());
            map.put("location", l.getLocation());
            map.put("sector", l.getSector());
            map.put("requiredSkills", l.getRequiredSkills().stream().map(skill -> skill.getName()).collect(Collectors.toList()));
            listingPayloads.add(map);
        }
        solverPayload.put("listings", listingPayloads);

        // Serialize preferences
        List<Map<String, Object>> preferencePayloads = new ArrayList<>();
        for (Preference p : preferences) {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", p.getStudent().getId().toString());
            map.put("preferenceOrder", p.getPreferenceOrder().stream().map(UUID::toString).collect(Collectors.toList()));
            preferencePayloads.add(map);
        }
        solverPayload.put("preferences", preferencePayloads);

        // Convert solver payload to JSON string for input snapshot
        String snapshotJson = "{}";
        try {
            snapshotJson = objectMapper.writeValueAsString(solverPayload);
        } catch (Exception ignored) {}

        // Make HTTP Call to Python FastAPI allocation engine
        String solverUrl = allocationEngineUrl + "/v1/simulate";
        Map<String, Object> solverResponse;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(solverUrl, solverPayload, Map.class);
            solverResponse = response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("FastAPI allocation engine connection failed", e);
        }

        UUID runId = UUID.randomUUID();
        String status = commitToApprovalQueue ? "READY_FOR_APPROVAL" : "DRAFT";

        String metricsJson = "{}";
        try {
            metricsJson = objectMapper.writeValueAsString(solverResponse.get("metrics"));
        } catch (Exception ignored) {}

        AllocationRun run = AllocationRun.builder()
                .id(runId)
                .cycleId(cycleId)
                .status(status)
                .seed(seed)
                .budgetCeiling(budgetCeiling)
                .inputSnapshot(snapshotJson)
                .metrics(metricsJson)
                .build();
        allocationRunRepository.save(run);

        // Save proposed allocations
        List<Map<String, Object>> allocations = (List<Map<String, Object>>) solverResponse.get("allocations");
        if (allocations != null) {
            for (Map<String, Object> alloc : allocations) {
                UUID studentId = UUID.fromString((String) alloc.get("student_id"));
                String listingIdStr = (String) alloc.get("listing_id");
                UUID listingId = (listingIdStr != null) ? UUID.fromString(listingIdStr) : null;
                Integer assignedRank = (Integer) alloc.get("assigned_rank");
                Double compScore = (Double) alloc.get("compatibility_score");

                Map<String, Object> explanationTrace = (Map<String, Object>) alloc.get("explanation");
                String explanationJson = "{}";
                try {
                    explanationJson = objectMapper.writeValueAsString(explanationTrace);
                } catch (Exception ignored) {}

                Student student = studentRepository.findById(studentId).orElse(null);
                InternshipListing listing = (listingId != null) ? internshipListingRepository.findById(listingId).orElse(null) : null;

                if (student != null) {
                    AllocationResult result = AllocationResult.builder()
                            .id(UUID.randomUUID())
                            .allocationRun(run)
                            .student(student)
                            .listing(listing)
                            .assignedRank(assignedRank)
                            .compatibilityScore(compScore != null ? BigDecimal.valueOf(compScore) : null)
                            .explanation(explanationJson)
                            .status("PROPOSED")
                            .build();
                    allocationResultRepository.save(result);
                }
            }
        }

        // Audit the action
        auditService.logEvent(
                commitToApprovalQueue ? "ALLOCATION_RUN_READY_FOR_APPROVAL" : "ALLOCATION_SIMULATION_COMPLETED",
                "SYSTEM_ADMIN",
                null,
                Map.of("runId", runId, "cycleId", cycleId, "status", status, "metrics", solverResponse.get("metrics"))
        );

        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("runId", runId);
        serviceResult.put("status", status);
        serviceResult.put("metrics", solverResponse.get("metrics"));
        serviceResult.put("constraintTrace", solverResponse.get("constraint_trace"));
        return serviceResult;
    }

    @Transactional
    public void approveRun(UUID runId, String justification, String actorId) {
        AllocationRun run = allocationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation run not found"));

        if (!"READY_FOR_APPROVAL".equals(run.getStatus())) {
            throw new IllegalStateException("Run status is not READY_FOR_APPROVAL");
        }

        run.setStatus("APPROVED");
        allocationRunRepository.save(run);

        // Update proposed results to PROPOSED (awaiting student response) or auto-confirm?
        // Wait, the process specifies:
        // "Commit results transactionally, and publish notifications"
        List<AllocationResult> results = allocationResultRepository.findByAllocationRunId(runId);
        for (AllocationResult res : results) {
            res.setStatus("PROPOSED");
            allocationResultRepository.save(res);

            // Send notification to student
            if (res.getListing() != null) {
                Student s = res.getStudent();
                Map<String, String> variables = new HashMap<>();
                variables.put("studentName", s.getFullName());
                variables.put("listingTitle", res.getListing().getTitle());
                variables.put("companyName", res.getListing().getCompany().getLegalName());

                notificationService.sendNotification(
                        s.getPhone() != null ? s.getPhone() : s.getId().toString(),
                        "SMS",
                        "ALLOCATION_PROPOSED",
                        s.getPreferredLanguage(),
                        variables
                );
            }
        }

        // Save override audit trail
        auditService.logEvent(
                "ALLOCATION_RUN_APPROVED",
                actorId,
                null,
                Map.of("runId", runId, "justification", justification)
        );
    }

    @Transactional
    public void acceptAllocation(UUID studentId) {
        // Find proposed allocation result
        Optional<AllocationResult> proposedOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "PROPOSED");
        if (proposedOpt.isEmpty()) {
            throw new IllegalStateException("No proposed allocation found for student");
        }

        AllocationResult result = proposedOpt.get();
        result.setStatus("ACCEPTED");
        allocationResultRepository.save(result);

        // Audit the acceptance
        auditService.logEvent(
                "STUDENT_ACCEPTED_ALLOCATION",
                studentId.toString(),
                null,
                Map.of("studentId", studentId, "listingId", result.getListing().getId())
        );
    }

    @Transactional
    public void rejectAllocation(UUID studentId) {
        Optional<AllocationResult> proposedOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "PROPOSED");
        if (proposedOpt.isEmpty()) {
            throw new IllegalStateException("No proposed allocation found for student");
        }

        AllocationResult result = proposedOpt.get();
        result.setStatus("REJECTED");
        allocationResultRepository.save(result);

        // Trigger waitlist promotion
        promoteWaitlist(result.getListing());

        // Audit the rejection
        auditService.logEvent(
                "STUDENT_REJECTED_ALLOCATION",
                studentId.toString(),
                null,
                Map.of("studentId", studentId, "listingId", result.getListing().getId())
        );
    }

    @Transactional
    public void promoteWaitlist(InternshipListing listing) {
        if (listing == null) return;

        // Get waitlist entries for this listing
        List<WaitlistEntry> waitlist = waitlistEntryRepository.findByListingIdAndStatusOrderByRankPositionAsc(listing.getId(), "WAITING");
        if (waitlist.isEmpty()) {
            return;
        }

        // Promote the first student on the waitlist
        WaitlistEntry entry = waitlist.get(0);
        entry.setStatus("PROMOTED");
        waitlistEntryRepository.save(entry);

        // Create a new AllocationResult for this student
        AllocationResult result = AllocationResult.builder()
                .id(UUID.randomUUID())
                .allocationRun(allocationRunRepository.findAll().get(0)) // Link to first or active run
                .student(entry.getStudent())
                .listing(listing)
                .assignedRank(99) // Custom code representing waitlist promotion
                .compatibilityScore(BigDecimal.ONE)
                .explanation("{\"reason\": \"Promoted from waitlist position " + entry.getRankPosition() + "\"}")
                .status("PROPOSED")
                .build();
        allocationResultRepository.save(result);

        // Notify the promoted student
        Student s = entry.getStudent();
        Map<String, String> variables = new HashMap<>();
        variables.put("studentName", s.getFullName());
        variables.put("listingTitle", listing.getTitle());
        variables.put("companyName", listing.getCompany().getLegalName());

        notificationService.sendNotification(
                s.getPhone() != null ? s.getPhone() : s.getId().toString(),
                "SMS",
                "ALLOCATION_PROPOSED",
                s.getPreferredLanguage(),
                variables
        );

        auditService.logEvent(
                "WAITLIST_STUDENT_PROMOTED",
                "SYSTEM",
                null,
                Map.of("studentId", s.getId(), "listingId", listing.getId(), "waitlistPosition", entry.getRankPosition())
        );
    }

    public List<AllocationResult> getRunResults(UUID runId) {
        return allocationResultRepository.findByAllocationRunId(runId);
    }

    public Map<String, Object> getStudentExplanation(UUID studentId) {
        Optional<AllocationResult> resultOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "ACCEPTED");
        if (resultOpt.isEmpty()) {
            resultOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "PROPOSED");
        }
        if (resultOpt.isEmpty()) {
            resultOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "JOINED");
        }

        Map<String, Object> explanation = new HashMap<>();
        if (resultOpt.isPresent()) {
            AllocationResult res = resultOpt.get();
            explanation.put("studentId", studentId);
            explanation.put("assignedCompany", res.getListing() != null ? res.getListing().getCompany().getLegalName() : "Unassigned");
            explanation.put("assignedListing", res.getListing() != null ? res.getListing().getTitle() : "None");
            explanation.put("assignedRank", res.getAssignedRank());
            explanation.put("compatibilityScore", res.getCompatibilityScore());
            explanation.put("status", res.getStatus());
            
            try {
                Map trace = objectMapper.readValue(res.getExplanation(), Map.class);
                explanation.put("trace", trace);
            } catch (Exception e) {
                explanation.put("trace", Collections.emptyMap());
            }
        } else {
            explanation.put("studentId", studentId);
            explanation.put("assignedCompany", "Unassigned");
            explanation.put("assignedListing", "None");
            explanation.put("status", "UNASSIGNED");
            explanation.put("trace", Map.of("reason", "No eligible slots or budget caps hit. Retry next cycle."));
        }
        return explanation;
    }

    public Map<String, Object> queryCounterfactual(UUID studentId, String changedField, String changedValue) {
        // Find latest run to get snapshot base
        List<AllocationRun> runs = allocationRunRepository.findAll();
        if (runs.isEmpty()) {
            throw new IllegalStateException("No allocation runs exist to run a counterfactual analysis against.");
        }
        AllocationRun run = runs.get(runs.size() - 1); // get latest run

        try {
            Map<String, Object> solverPayload = objectMapper.readValue(run.getInputSnapshot(), Map.class);
            
            // 1. Find the target student in the payload
            List<Map<String, Object>> students = (List<Map<String, Object>>) solverPayload.get("students");
            Map<String, Object> targetStudent = null;
            for (Map<String, Object> s : students) {
                if (studentId.toString().equals(s.get("id"))) {
                    targetStudent = s;
                    break;
                }
            }

            if (targetStudent == null) {
                throw new IllegalArgumentException("Student not found in snapshot");
            }

            // 2. Apply the modification
            String causalChangeText = "";
            if ("add_skill".equalsIgnoreCase(changedField) || "skill".equalsIgnoreCase(changedField)) {
                List<String> skills = new ArrayList<>((List<String>) targetStudent.get("skills"));
                skills.add(changedValue.trim());
                targetStudent.put("skills", skills);
                causalChangeText = "Added skill '" + changedValue + "'.";
                
                // Automatically make the student eligible for listing that requires this skill
                List<String> eligibleListings = new ArrayList<>((List<String>) targetStudent.get("eligibleListings"));
                // Find all listings requiring this skill
                List<InternshipListing> allListings = internshipListingRepository.findAll();
                for (InternshipListing l : allListings) {
                    boolean requiresSkill = l.getRequiredSkills().stream().anyMatch(sk -> sk.getName().equalsIgnoreCase(changedValue));
                    if (requiresSkill && !eligibleListings.contains(l.getId().toString())) {
                        eligibleListings.add(l.getId().toString());
                    }
                }
                targetStudent.put("eligibleListings", eligibleListings);

            } else if ("change_preference".equalsIgnoreCase(changedField) || "preference".equalsIgnoreCase(changedField)) {
                List<Map<String, Object>> preferences = (List<Map<String, Object>>) solverPayload.get("preferences");
                for (Map<String, Object> pref : preferences) {
                    if (studentId.toString().equals(pref.get("studentId"))) {
                        // Move changedValue (listingId) to first choice
                        List<String> order = new ArrayList<>((List<String>) pref.get("preferenceOrder"));
                        order.remove(changedValue);
                        order.add(0, changedValue);
                        pref.put("preferenceOrder", order);
                        break;
                    }
                }
                causalChangeText = "Moved listing '" + changedValue + "' to 1st preference.";
            }

            // 3. Post to solver
            String solverUrl = allocationEngineUrl + "/v1/simulate";
            ResponseEntity<Map> response = restTemplate.postForEntity(solverUrl, solverPayload, Map.class);
            Map<String, Object> solverResponse = response.getBody();

            // 4. Extract new outcome
            List<Map<String, Object>> newAllocations = (List<Map<String, Object>>) solverResponse.get("allocations");
            Map<String, Object> targetAlloc = null;
            if (newAllocations != null) {
                for (Map<String, Object> a : newAllocations) {
                    if (studentId.toString().equals(a.get("student_id"))) {
                        targetAlloc = a;
                        break;
                    }
                }
            }

            // 5. Compare with original allocation in results
            Optional<AllocationResult> originalResultOpt = allocationResultRepository.findByAllocationRunIdAndStudentId(run.getId(), studentId);
            String originalCompany = originalResultOpt.isPresent() && originalResultOpt.get().getListing() != null 
                    ? originalResultOpt.get().getListing().getCompany().getLegalName() : "Unassigned";
            String originalTitle = originalResultOpt.isPresent() && originalResultOpt.get().getListing() != null 
                    ? originalResultOpt.get().getListing().getTitle() : "None";

            String newCompany = "Unassigned";
            String newTitle = "None";
            Integer newRank = null;
            if (targetAlloc != null && targetAlloc.get("listing_id") != null) {
                UUID newListingId = UUID.fromString((String) targetAlloc.get("listing_id"));
                InternshipListing newListing = internshipListingRepository.findById(newListingId).orElse(null);
                if (newListing != null) {
                    newCompany = newListing.getCompany().getLegalName();
                    newTitle = newListing.getTitle();
                }
                newRank = (Integer) targetAlloc.get("assigned_rank");
            }

            boolean outcomeChanged = !originalCompany.equals(newCompany) || !originalTitle.equals(newTitle);

            String explanation;
            if (outcomeChanged) {
                explanation = "Yes, your outcome changed! By making this change (" + causalChangeText + "), you would be assigned to '" + newTitle + " (" + newCompany + ")' instead of '" + originalTitle + " (" + originalCompany + ")'.";
            } else {
                explanation = "No, your outcome did not change. You remain assigned to '" + originalTitle + " (" + originalCompany + ")'. This is because even with this change, other candidates had higher priority, or the capacity constraints of your preferred companies were already saturated.";
            }

            Map<String, Object> cfResult = new HashMap<>();
            cfResult.put("studentId", studentId);
            cfResult.put("outcomeChanged", outcomeChanged);
            cfResult.put("causalChange", causalChangeText);
            cfResult.put("originalAllocation", Map.of("company", originalCompany, "title", originalTitle));
            cfResult.put("counterfactualAllocation", Map.of("company", newCompany, "title", newTitle, "rank", newRank != null ? newRank : "Unassigned"));
            cfResult.put("explanation", explanation);
            return cfResult;

        } catch (Exception e) {
            throw new RuntimeException("Failed to run counterfactual query", e);
        }
    }
}

