package in.pmis.modules.internships;

import in.pmis.modules.companies.Company;
import in.pmis.modules.companies.CompanyRepository;
import in.pmis.modules.companies.JoiningConfirmation;
import in.pmis.modules.companies.JoiningConfirmationRepository;
import in.pmis.modules.students.Skill;
import in.pmis.modules.students.SkillRepository;
import in.pmis.modules.students.Student;
import in.pmis.modules.students.StudentRepository;
import in.pmis.modules.allocations.AllocationResult;
import in.pmis.modules.allocations.AllocationResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InternshipService {

    @Autowired
    private InternshipListingRepository internshipListingRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AllocationResultRepository allocationResultRepository;

    @Autowired
    private JoiningConfirmationRepository joiningConfirmationRepository;

    @Autowired
    private StudentRepository studentRepository;

    public List<InternshipListing> getAllListings() {
        return internshipListingRepository.findAll();
    }

    public List<InternshipListing> getActiveListings() {
        return internshipListingRepository.findByStatus("PUBLISHED");
    }

    @Transactional
    public InternshipListing createListing(UUID companyId, String title, String location, String sector,
                                           int capacity, BigDecimal stipendShare, List<String> skillNames) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Set<Skill> skills = resolveSkills(skillNames);

        InternshipListing listing = InternshipListing.builder()
                .id(UUID.randomUUID())
                .company(company)
                .title(title)
                .location(location)
                .sector(sector)
                .capacity(capacity)
                .stipendCompanyShare(stipendShare)
                .status("PUBLISHED")
                .requiredSkills(skills)
                .build();

        return internshipListingRepository.save(listing);
    }

    @Transactional
    public InternshipListing updateListing(UUID listingId, String title, String location, String sector,
                                           List<String> skillNames) {
        InternshipListing listing = internshipListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        listing.setTitle(title);
        listing.setLocation(location);
        listing.setSector(sector);

        if (skillNames != null) {
            listing.setRequiredSkills(resolveSkills(skillNames));
        }

        return internshipListingRepository.save(listing);
    }

    @Transactional
    public InternshipListing updateCapacity(UUID listingId, int capacity) {
        InternshipListing listing = internshipListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative");
        }

        listing.setCapacity(capacity);
        return internshipListingRepository.save(listing);
    }

    @Transactional
    public JoiningConfirmation confirmJoining(UUID listingId, UUID studentId, boolean confirmed, String comments) {
        InternshipListing listing = internshipListingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        // Retrieve the allocation result to update its status to JOINED if confirmed
        Optional<AllocationResult> resultOpt = allocationResultRepository.findByStudentIdAndStatus(studentId, "ACCEPTED");
        if (resultOpt.isPresent()) {
            AllocationResult result = resultOpt.get();
            if (confirmed) {
                result.setStatus("JOINED");
                allocationResultRepository.save(result);
            }
        }

        Optional<JoiningConfirmation> confirmationOpt = joiningConfirmationRepository.findByStudentIdAndListingId(studentId, listingId);
        JoiningConfirmation confirmation;
        if (confirmationOpt.isPresent()) {
            confirmation = confirmationOpt.get();
            confirmation.setConfirmed(confirmed);
            confirmation.setComments(comments);
            confirmation.setConfirmedAt(Instant.now());
        } else {
            confirmation = JoiningConfirmation.builder()
                    .id(UUID.randomUUID())
                    .student(student)
                    .listing(listing)
                    .confirmed(confirmed)
                    .comments(comments)
                    .build();
        }

        return joiningConfirmationRepository.save(confirmation);
    }

    public List<AllocationResult> getApplications(UUID listingId) {
        return allocationResultRepository.findByListingId(listingId);
    }

    private Set<Skill> resolveSkills(List<String> skillNames) {
        if (skillNames == null) {
            return Collections.emptySet();
        }
        return skillNames.stream().map(name -> {
            String cleanName = name.trim();
            return skillRepository.findByNameIgnoreCase(cleanName)
                    .orElseGet(() -> {
                        Skill s = Skill.builder().id(UUID.randomUUID()).name(cleanName).build();
                        return skillRepository.save(s);
                    });
        }).collect(Collectors.toSet());
    }
}
