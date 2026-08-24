package in.pmis.modules.students;

import in.pmis.modules.exceptions.ExceptionCase;
import in.pmis.modules.exceptions.ExceptionCaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ExceptionCaseRepository exceptionCaseRepository;

    @Transactional
    public StudentProfile createOrUpdateProfile(UUID studentId, String fullName, String phone, String language,
                                                String district, boolean aspirationalDistrict, String category,
                                                String gender, String dobStr, boolean failEkyc, String operatorId) {
        
        Optional<Student> studentOpt = studentRepository.findById(studentId);
        Student student;
        if (studentOpt.isPresent()) {
            student = studentOpt.get();
            student.setFullName(fullName);
            student.setPhone(phone);
            student.setPreferredLanguage(language);
            student.setDistrict(district);
            student.setAspirationalDistrict(aspirationalDistrict);
        } else {
            student = Student.builder()
                    .id(studentId)
                    .fullName(fullName)
                    .phone(phone)
                    .preferredLanguage(language)
                    .district(district)
                    .aspirationalDistrict(aspirationalDistrict)
                    .build();
        }
        studentRepository.save(student);

        Optional<StudentProfile> profileOpt = studentProfileRepository.findById(studentId);
        StudentProfile profile;
        if (profileOpt.isPresent()) {
            profile = profileOpt.get();
            profile.setCategory(category);
            profile.setGender(gender);
            profile.setUpdatedAt(Instant.now());
        } else {
            profile = StudentProfile.builder()
                    .studentId(studentId)
                    .student(student)
                    .category(category)
                    .gender(gender)
                    .build();
        }

        if (operatorId != null) {
            profile.setActingOnBehalfOf(operatorId);
        }

        if (failEkyc) {
            profile.setEkycVerified(false);
            profile.setEkycFailedReason("Aadhaar service degraded or credential mismatch");
            
            // Create a manual review exception
            ExceptionCase exceptionCase = ExceptionCase.builder()
                    .id(UUID.randomUUID())
                    .caseType("EKYC_DEGRADED")
                    .severity("HIGH")
                    .entityId(studentId)
                    .context(String.format("{\"studentId\": \"%s\", \"reason\": \"Aadhaar validation failed manually requested\"}", studentId))
                    .status("OPEN")
                    .build();
            exceptionCaseRepository.save(exceptionCase);
        } else {
            profile.setEkycVerified(true);
            profile.setEkycFailedReason(null);
        }

        return studentProfileRepository.save(profile);
    }
}
