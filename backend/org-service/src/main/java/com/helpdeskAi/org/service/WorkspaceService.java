package com.helpdeskAi.org.service;

import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.org.dto.*;
import com.helpdeskAi.org.model.*;
import com.helpdeskAi.org.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    public List<WorkspaceResponse> listWorkspaces() {
        UUID orgId = TenantContext.getTenantId();
        return workspaceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceResponse createWorkspace(CreateWorkspaceRequest request) {
        UUID orgId = TenantContext.getTenantId();
        Workspace workspace = Workspace.builder()
                .organizationId(orgId)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        workspace = workspaceRepository.save(workspace);
        log.info("Created workspace: {} for org: {}", workspace.getName(), orgId);
        return toResponse(workspace);
    }

    public WorkspaceResponse getWorkspace(UUID workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        return toResponse(workspace);
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, CreateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        if (request.getName() != null) workspace.setName(request.getName());
        if (request.getDescription() != null) workspace.setDescription(request.getDescription());
        workspace = workspaceRepository.save(workspace);
        return toResponse(workspace);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace", "id", workspaceId);
        }
        workspaceRepository.deleteById(workspaceId);
        log.info("Deleted workspace: {}", workspaceId);
    }

    private WorkspaceResponse toResponse(Workspace w) {
        return WorkspaceResponse.builder()
                .id(w.getId())
                .organizationId(w.getOrganizationId())
                .name(w.getName())
                .description(w.getDescription())
                .createdAt(w.getCreatedAt())
                .build();
    }
}
