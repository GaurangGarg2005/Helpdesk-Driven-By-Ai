package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.org.dto.*;
import com.helpdeskAi.org.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> listWorkspaces() {
        List<WorkspaceResponse> workspaces = workspaceService.listWorkspaces();
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspaces retrieved", workspaces, Instant.now()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Workspace created", workspace, Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getWorkspace(@PathVariable UUID id) {
        WorkspaceResponse workspace = workspaceService.getWorkspace(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace retrieved", workspace, Instant.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable UUID id, @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.updateWorkspace(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace updated", workspace, Instant.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable UUID id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Workspace deleted", null, Instant.now()));
    }
}
