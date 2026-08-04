package com.aliozcan.airportops.flight_service.task;

import com.aliozcan.airportops.flight_service.security.IamPrincipal;
import com.aliozcan.airportops.flight_service.task.dto.TaskResponse;
import com.aliozcan.airportops.flight_service.task.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organizations/{orgId}/flights/{flightId}/tasks")
public class TurnaroundTaskController {

    private final TurnaroundTaskService taskService;

    public TurnaroundTaskController(TurnaroundTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('task:read')")
    public ResponseEntity<List<TaskResponse>> list(
            @PathVariable UUID orgId, @PathVariable UUID flightId, Authentication authentication) {
        return ResponseEntity.ok(taskService.list(orgId, flightId, principal(authentication)));
    }

    @PutMapping("/{taskId}/status")
    @PreAuthorize("hasAuthority('task:complete')")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable UUID orgId,
            @PathVariable UUID flightId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                taskService.updateStatus(orgId, flightId, taskId, principal(authentication), request));
    }

    private IamPrincipal principal(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof IamPrincipal principal) {
            return principal;
        }
        throw new AccessDeniedException("Missing IAM principal details");
    }
}
