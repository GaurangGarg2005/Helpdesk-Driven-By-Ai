package com.helpdeskAi.org.service;

import com.helpdeskAi.common.exception.BadRequestException;
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
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public List<TeamResponse> listTeams() {
        UUID orgId = TenantContext.getTenantId();
        return teamRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        UUID orgId = TenantContext.getTenantId();
        Team team = Team.builder()
                .organizationId(orgId)
                .name(request.getName())
                .description(request.getDescription())
                .workspaceId(request.getWorkspaceId())
                .build();
        team = teamRepository.save(team);
        log.info("Created team: {} for org: {}", team.getName(), orgId);
        return toResponse(team);
    }

    public TeamResponse getTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        return toResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, CreateTeamRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
        if (request.getName() != null) team.setName(request.getName());
        if (request.getDescription() != null) team.setDescription(request.getDescription());
        if (request.getWorkspaceId() != null) team.setWorkspaceId(request.getWorkspaceId());
        team = teamRepository.save(team);
        return toResponse(team);
    }

    @Transactional
    public void deleteTeam(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team", "id", teamId);
        }
        teamRepository.deleteById(teamId);
        log.info("Deleted team: {}", teamId);
    }

    @Transactional
    public void addMember(UUID teamId, AddTeamMemberRequest request) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team", "id", teamId);
        }
        if (teamMemberRepository.findByTeamIdAndUserId(teamId, request.getUserId()).isPresent()) {
            throw new BadRequestException("User is already a member of this team");
        }
        TeamMember member = TeamMember.builder()
                .teamId(teamId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : "MEMBER")
                .build();
        teamMemberRepository.save(member);
        log.info("Added user {} to team {}", request.getUserId(), teamId);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId) {
        teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
        log.info("Removed user {} from team {}", userId, teamId);
    }

    private TeamResponse toResponse(Team t) {
        int memberCount = teamMemberRepository.findByTeamId(t.getId()).size();
        return TeamResponse.builder()
                .id(t.getId())
                .organizationId(t.getOrganizationId())
                .workspaceId(t.getWorkspaceId())
                .name(t.getName())
                .description(t.getDescription())
                .memberCount(memberCount)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
