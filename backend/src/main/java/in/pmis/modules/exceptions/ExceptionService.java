package in.pmis.modules.exceptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class ExceptionService {

    @Autowired
    private ExceptionCaseRepository exceptionCaseRepository;

    public List<ExceptionCase> getOpenExceptions() {
        return exceptionCaseRepository.findByStatus("OPEN");
    }

    public List<ExceptionCase> getAllExceptions() {
        return exceptionCaseRepository.findAll();
    }

    @Transactional
    public ExceptionCase resolveException(UUID exceptionId, String resolutionReason) {
        ExceptionCase exCase = exceptionCaseRepository.findById(exceptionId)
                .orElseThrow(() -> new IllegalArgumentException("Exception case not found"));

        exCase.setStatus("RESOLVED");
        exCase.setResolutionReason(resolutionReason);

        return exceptionCaseRepository.save(exCase);
    }
}
