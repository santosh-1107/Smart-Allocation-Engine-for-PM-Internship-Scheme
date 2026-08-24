package in.pmis;

import in.pmis.modules.companies.*;
import in.pmis.modules.internships.*;
import in.pmis.modules.students.*;
import in.pmis.modules.preferences.*;
import in.pmis.modules.eligibility.*;
import in.pmis.modules.policy.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Component
@org.springframework.context.annotation.Profile("!test")
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyReliabilityScoreRepository companyReliabilityScoreRepository;

    @Autowired
    private InternshipListingRepository internshipListingRepository;

    @Autowired
    private PreferenceRepository preferenceRepository;

    @Autowired
    private PreferenceVersionRepository preferenceVersionRepository;

    @Autowired
    private EligibilityRecordRepository eligibilityRecordRepository;

    @Autowired
    private PolicyDocumentRepository policyDocumentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] DISTRICTS = {"Mumbai", "Pune", "Nagpur", "Satara", "Nandurbar", "Washim", "Gadchiroli", "Hingoli"};
    private static final String[] ASPIRATIONAL_DISTRICTS = {"Nandurbar", "Washim", "Gadchiroli"};
    private static final String[] SECTORS = {"IT Services", "Finance", "Healthcare", "Manufacturing", "Education", "Retail"};
    private static final String[] DEGREES = {"B.E.", "B.Sc.", "B.Com.", "B.A.", "Diploma"};
    private static final String[] SUBJECTS = {"Computer Science", "Physics", "Commerce", "Economics", "Mechanical"};
    private static final String[] CATEGORIES = {"GENERAL", "OBC", "SC", "ST"};
    private static final String[] SKILL_POOL = {"SQL", "Excel", "Python", "Java", "React", "HTML", "Accounting", "Marketing", "Data Entry", "Business English", "Machine Learning", "Tally"};

    @Override
    public void run(String... args) throws Exception {
        if (studentRepository.count() > 0) {
            System.out.println("Database already seeded. Skipping seeder.");
            return;
        }

        System.out.println("Seeding database with PMIS Smart Allocation data...");

        // 1. Seed Skills
        List<Skill> skills = new ArrayList<>();
        for (String sName : SKILL_POOL) {
            Skill s = Skill.builder().id(UUID.randomUUID()).name(sName).build();
            skills.add(skillRepository.save(s));
        }

        // 2. Seed Companies (100)
        List<Company> companies = new ArrayList<>();
        String[] companyNames = {"Tata Consultancy", "Reliance Industries", "Infosys Tech", "Wipro Limited", "HDFC Bank", "ICICI Bank", "L&T Infotech", "Tech Mahindra", "State Bank of India", "Mahindra & Mahindra"};
        for (int i = 1; i <= 100; i++) {
            String baseName = companyNames[i % companyNames.length];
            String name = baseName + " Group " + i;
            Company c = Company.builder()
                    .id(UUID.randomUUID())
                    .legalName(name)
                    .cin("L" + (10000 + i) + "MH1973PLC" + (100000 + i))
                    .verified(true)
                    .build();
            companyRepository.save(c);
            companies.add(c);

            // Seed Reliability score
            CompanyReliabilityScore score = CompanyReliabilityScore.builder()
                    .companyId(c.getId())
                    .company(c)
                    .onboardingCount(randomRange(5, 20))
                    .withdrawalCount(randomRange(0, 2))
                    .build();
            companyReliabilityScoreRepository.save(score);
        }

        // 3. Seed Internship Listings (300)
        List<InternshipListing> listings = new ArrayList<>();
        String[] listingTitles = {"Software Engineering Intern", "Data Analyst Trainee", "Accounts Clerk Assistant", "Marketing Associate", "Front Desk Representative", "Lab Technician Intern"};
        for (int i = 1; i <= 300; i++) {
            Company c = companies.get(i % companies.size());
            String title = listingTitles[i % listingTitles.length];
            String location = DISTRICTS[i % DISTRICTS.length];
            String sector = SECTORS[i % SECTORS.length];
            int capacity = randomRange(1, 4);

            Set<Skill> reqSkills = new HashSet<>();
            reqSkills.add(skills.get(i % skills.size()));
            reqSkills.add(skills.get((i + 1) % skills.size()));

            InternshipListing listing = InternshipListing.builder()
                    .id(UUID.randomUUID())
                    .company(c)
                    .title(title)
                    .location(location)
                    .sector(sector)
                    .capacity(capacity)
                    .stipendCompanyShare(BigDecimal.valueOf(500.00 + randomRange(0, 10) * 150)) // stipend range 500 to 2000
                    .status("PUBLISHED")
                    .requiredSkills(reqSkills)
                    .build();
            
            internshipListingRepository.save(listing);
            listings.add(listing);
        }

        // 4. Seed Students (1000)
        List<Student> studentList = new ArrayList<>();
        String[] firstNames = {"Amit", "Priya", "Rahul", "Sneha", "Abhishek", "Aniket", "Kunal", "Neha", "Sachin", "Jyoti", "Vikram", "Pooja"};
        String[] lastNames = {"Sharma", "Patil", "Deshmukh", "Joshi", "Singh", "Shinde", "Kulkarni", "Jadhav", "Bhosale", "Gupta", "More", "Rane"};

        for (int i = 1; i <= 1000; i++) {
            String fName = firstNames[i % firstNames.length];
            String lName = lastNames[i % lastNames.length];
            String fullName = fName + " " + lName + " " + i;

            String district = DISTRICTS[i % DISTRICTS.length];
            boolean isAspirational = Arrays.asList(ASPIRATIONAL_DISTRICTS).contains(district);

            Set<Skill> studSkills = new HashSet<>();
            studSkills.add(skills.get(i % skills.size()));
            studSkills.add(skills.get((i + 3) % skills.size()));

            Student s = Student.builder()
                    .id(UUID.randomUUID())
                    .fullName(fullName)
                    .phone(i <= 3 ? "9999" + String.format("%06d", i) : "98" + String.format("%08d", i)) // 9999 mock for ekyc retries
                    .preferredLanguage(i % 3 == 0 ? "mr" : (i % 3 == 1 ? "hi" : "en"))
                    .district(district)
                    .aspirationalDistrict(isAspirational)
                    .skills(studSkills)
                    .build();
            studentRepository.save(s);
            studentList.add(s);

            // Student profile
            StudentProfile profile = StudentProfile.builder()
                    .studentId(s.getId())
                    .student(s)
                    .category(CATEGORIES[i % CATEGORIES.length])
                    .gender(i % 2 == 0 ? "Male" : "Female")
                    .dob(LocalDate.of(2001 + (i % 4), 1 + (i % 12), 1 + (i % 28)))
                    .ekycVerified(i != 42) // Student 42 has failed/degraded eKYC state
                    .ekycFailedReason(i == 42 ? "Aadhaar eKYC service timeout" : null)
                    .build();
            studentProfileRepository.save(profile);

            // Education
            Education edu = Education.builder()
                    .id(UUID.randomUUID())
                    .student(s)
                    .degree(DEGREES[i % DEGREES.length])
                    .fieldOfStudy(SUBJECTS[i % SUBJECTS.length])
                    .institution("University of Maharashtra College " + i)
                    .percentageOrCgpa(BigDecimal.valueOf(55.0 + randomRange(0, 370) / 10.0)) // cgpa 55% to 92%
                    .graduationYear(2025)
                    .build();
            educationRepository.save(edu);
        }

        // 5. Seed Preferences & Eligibility Records (For 1000 Students)
        UUID cycleId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        for (int i = 0; i < studentList.size(); i++) {
            Student s = studentList.get(i);
            
            // Choose 4 listings preferred by location, or random
            List<UUID> prefOrder = new ArrayList<>();
            for (int k = 0; k < 4; k++) {
                InternshipListing l = listings.get((i * 4 + k) % listings.size());
                prefOrder.add(l.getId());
            }

            Preference pref = Preference.builder()
                    .id(UUID.randomUUID())
                    .student(s)
                    .cycleId(cycleId)
                    .version(1)
                    .preferenceOrder(prefOrder)
                    .build();
            preferenceRepository.save(pref);

            PreferenceVersion version = PreferenceVersion.builder()
                    .id(UUID.randomUUID())
                    .studentId(s.getId())
                    .version(1)
                    .preferenceOrder(prefOrder)
                    .updatedBy(s.getFullName())
                    .build();
            preferenceVersionRepository.save(version);

            // Seed eligibility records (matching skill counts)
            for (UUID listingId : prefOrder) {
                InternshipListing listing = internshipListingRepository.findById(listingId).orElse(null);
                if (listing != null) {
                    // Eligible if shares at least 1 skill
                    boolean eligible = s.getSkills().stream().anyMatch(sk -> listing.getRequiredSkills().contains(sk));
                    
                    EligibilityRecord er = EligibilityRecord.builder()
                            .id(UUID.randomUUID())
                            .student(s)
                            .listing(listing)
                            .eligible(eligible)
                            .reason(eligible ? "Passed skill match eligibility." : "Missing required skills: " + listing.getRequiredSkills().stream().map(Skill::getName).collect(Collectors.joining(",")))
                            .build();
                    eligibilityRecordRepository.save(er);
                }
            }
        }

        // 6. Seed Policy Guidelines Documents for RAG vector search
        PolicyDocument doc = PolicyDocument.builder()
                .id(UUID.randomUUID())
                .title("PM Internship Scheme (PMIS) Guidelines 2026")
                .url("https://pminternship.mca.gov.in/guidelines")
                .build();
        policyDocumentRepository.save(doc);

        String[] sections = {
                "Section 1.1: Core Eligibility Criteria",
                "Section 2.4: Monthly Stipend Structure",
                "Section 3.2: Reservation Quotas and Quota Floors",
                "Section 4.1: Code of Conduct & Political Activity Freezes"
        };

        String[] contents = {
                "Candidates eligible to register for the scheme must be citizens of India, aged between 21 and 25 years. Educational qualification requires a minimum pass criteria in Matriculation, Higher Secondary, Graduation or equivalent Diploma certifications. Candidates must not be currently employed.",
                "Interns enrolled in the PM Internship Scheme shall receive a monthly stipend of Rs. 5000. The government share of Rs. 4500 is disbursed directly to the student's bank account via DBT. The empanelled partner company shall contribute Rs. 500 monthly from its corporate CSR funds.",
                "In order to promote spatial equity, a quota floor of 20% is established for students registering from identified Aspirational Districts. Social justice quota policies also mandate SC/ST/OBC representation allocations as governed by national internship scheme protocols.",
                "Under Code of Conduct protocols, publication of new lists or processing allocations in districts where the Model Code of Conduct (MCoC) is actively frozen by the Election Commission of India is suspended until election cycles conclude."
        };

        // Insert policy chunks using JdbcTemplate to bypass pgvector Hibernate mappings
        for (int i = 0; i < sections.length; i++) {
            UUID chunkId = UUID.randomUUID();
            String sectionTitle = sections[i];
            String content = contents[i];
            
            // Create a mock 384-dimensional vector embedding
            float[] embedding = new float[384];
            // Seed it with some values so cosine similarity works correctly
            for (int k = 0; k < 384; k++) {
                embedding[k] = (float) ((i + 1) * 0.01 + k * 0.0001);
            }
            
            String vectorStr = "[" + joinFloatArray(embedding) + "]";

            jdbcTemplate.update(
                "INSERT INTO policy_chunks (id, document_id, section_title, content, embedding) VALUES (?, ?, ?, ?, ?::vector)",
                chunkId, doc.getId(), sectionTitle, content, vectorStr
            );
        }

        System.out.println("PMIS Smart Allocation Database successfully seeded!");
    }

    private int randomRange(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    private String joinFloatArray(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
