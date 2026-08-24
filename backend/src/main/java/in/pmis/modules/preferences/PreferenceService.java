package in.pmis.modules.preferences;

import in.pmis.modules.students.Student;
import in.pmis.modules.students.StudentRepository;
import in.pmis.modules.internships.InternshipListing;
import in.pmis.modules.internships.InternshipListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PreferenceService {

    @Autowired
    private PreferenceRepository preferenceRepository;

    @Autowired
    private PreferenceVersionRepository preferenceVersionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private InternshipListingRepository internshipListingRepository;

    // In-memory session state for pairwise elicitation
    private final ConcurrentHashMap<UUID, ElicitationState> elicitationSessions = new ConcurrentHashMap<>();

    @Data
    public static class ElicitationState implements Serializable {
        private UUID studentId;
        private List<UUID> sortedList = new ArrayList<>();
        private List<UUID> pendingList = new ArrayList<>();
        private int low;
        private int high;
        private UUID currentPending;
    }

    @Transactional
    public Preference submitPreferences(UUID studentId, UUID cycleId, List<UUID> preferenceOrder, int requestVersion) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        int maxVersion = preferenceRepository.findMaxVersionByStudentIdAndCycleId(studentId, cycleId);

        if (maxVersion > 0 && requestVersion != maxVersion) {
            throw new ObjectOptimisticLockingFailureException(Preference.class, studentId);
        }

        int newVersion = maxVersion + 1;

        // Save active preference
        Optional<Preference> activePrefOpt = preferenceRepository.findByStudentIdAndCycleId(studentId, cycleId);
        Preference preference;
        if (activePrefOpt.isPresent()) {
            preference = activePrefOpt.get();
            preference.setVersion(newVersion);
            preference.setPreferenceOrder(preferenceOrder);
        } else {
            preference = Preference.builder()
                    .id(UUID.randomUUID())
                    .student(student)
                    .cycleId(cycleId)
                    .version(newVersion)
                    .preferenceOrder(preferenceOrder)
                    .build();
        }
        preferenceRepository.save(preference);

        // Append to preference_versions for audit trail
        PreferenceVersion history = PreferenceVersion.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .version(newVersion)
                .preferenceOrder(preferenceOrder)
                .updatedBy(student.getFullName())
                .build();
        preferenceVersionRepository.save(history);

        return preference;
    }

    public int getCurrentVersion(UUID studentId, UUID cycleId) {
        return preferenceRepository.findMaxVersionByStudentIdAndCycleId(studentId, cycleId);
    }

    public Optional<Preference> getPreferences(UUID studentId, UUID cycleId) {
        return preferenceRepository.findByStudentIdAndCycleId(studentId, cycleId);
    }

    // Initialize pairwise preference elicitation
    public Map<String, Object> startPairwise(UUID studentId, UUID cycleId) {
        // Load some listings to compare (e.g. active listings)
        List<InternshipListing> activeListings = internshipListingRepository.findByStatus("PUBLISHED");
        if (activeListings.size() < 2) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Not enough active listings to compare.");
            return response;
        }

        List<UUID> listingIds = activeListings.stream()
                .limit(6) // limit to 6 listings to keep comparisons manageable for the user
                .map(InternshipListing::getId)
                .collect(Collectors.toList());

        ElicitationState state = new ElicitationState();
        state.setStudentId(studentId);
        
        // Seed sortedList with the first item
        state.getSortedList().add(listingIds.get(0));
        
        // Put remaining items in pendingList
        state.setPendingList(listingIds.subList(1, listingIds.size()));
        
        // Set up first binary search insertion
        UUID firstPending = state.getPendingList().remove(0);
        state.setCurrentPending(firstPending);
        state.setLow(0);
        state.setHigh(0); // sortedList size is 1

        elicitationSessions.put(studentId, state);

        return getPairwiseNextQuestion(state, cycleId);
    }

    // Process pairwise comparison and return next question or complete status
    @Transactional
    public Map<String, Object> processPairwise(UUID studentId, UUID cycleId, UUID chosenId, UUID otherId) {
        ElicitationState state = elicitationSessions.get(studentId);
        if (state == null) {
            return startPairwise(studentId, cycleId);
        }

        UUID current = state.getCurrentPending();
        int mid = (state.getLow() + state.getHigh()) / 2;
        UUID comparedWith = state.getSortedList().get(mid);

        // Validate choice
        if (!chosenId.equals(current) && !chosenId.equals(comparedWith)) {
            throw new IllegalArgumentException("Invalid choices in pairwise comparison request");
        }

        boolean currentPreferred = chosenId.equals(current);

        if (currentPreferred) {
            // current (new item) is preferred over comparedWith (index mid)
            // It should be placed at or before mid (which means index <= mid, i.e., lower index in preferred order)
            state.setHigh(mid - 1);
        } else {
            // comparedWith is preferred over current
            // It should be placed after mid
            state.setLow(mid + 1);
        }

        if (state.getLow() > state.getHigh()) {
            // Insertion index found at low
            int insertIndex = state.getLow();
            state.getSortedList().add(insertIndex, current);

            // Fetch next pending item if available
            if (!state.getPendingList().isEmpty()) {
                UUID nextPending = state.getPendingList().remove(0);
                state.setCurrentPending(nextPending);
                state.setLow(0);
                state.setHigh(state.getSortedList().size() - 1);
            } else {
                // Elicitation complete!
                elicitationSessions.remove(studentId);
                
                // Submit preference order
                Preference preference = submitPreferences(studentId, cycleId, state.getSortedList(), getCurrentVersion(studentId, cycleId));

                Map<String, Object> response = new HashMap<>();
                response.put("status", "COMPLETED");
                response.put("preferenceOrder", state.getSortedList());
                return response;
            }
        }

        return getPairwiseNextQuestion(state, cycleId);
    }

    private Map<String, Object> getPairwiseNextQuestion(ElicitationState state, UUID cycleId) {
        int mid = (state.getLow() + state.getHigh()) / 2;
        UUID comparedWith = state.getSortedList().get(mid);

        // Fetch details of choice A and choice B
        InternshipListing choiceA = internshipListingRepository.findById(state.getCurrentPending()).orElse(null);
        InternshipListing choiceB = internshipListingRepository.findById(comparedWith).orElse(null);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "COMPARING");
        response.put("choiceA", choiceA);
        response.put("choiceB", choiceB);
        response.put("progress", (state.getSortedList().size() * 100) / (state.getSortedList().size() + state.getPendingList().size() + 1));
        return response;
    }
}
