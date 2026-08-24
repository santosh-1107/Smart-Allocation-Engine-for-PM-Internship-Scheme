package in.pmis.modules.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exceptions")
public class ExceptionController {

    @Autowired
    private ExceptionService exceptionService;

    @Data
    public static class ResolveRequest {
        @NotBlank(message = "Resolution reason is required")
        private String resolutionReason;
    }

    @GetMapping
    public ResponseEntity<List<ExceptionCase>> getExceptions(@RequestParam(value = "all", defaultValue = "false") boolean all) {
        if (all) {
            return ResponseEntity.ok(exceptionService.getAllExceptions());
        } else {
            return ResponseEntity.ok(exceptionService.getOpenExceptions());
        }
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolveException(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ResolveRequest request) {

        ExceptionCase resolved = exceptionService.resolveException(id, request.getResolutionReason());
        return ResponseEntity.ok(resolved);
    }
}
