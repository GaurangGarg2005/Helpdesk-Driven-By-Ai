package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.org.dto.*;
import com.helpdeskAi.org.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> listTeams() {
        List<TeamResponse> teams = teamService.listTeams();
        return ResponseEntity.ok(new ApiResponse<>(true, "Teams retrieved", teams, Instant.now()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse team = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Team created", team, Instant.now()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeam(@PathVariable UUID id) {
        TeamResponse team = teamService.getTeam(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Team retrieved", team, Instant.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id, @Valid @RequestBody CreateTeamRequest request) {
        TeamResponse team = teamService.updateTeam(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Team updated", team, Instant.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Team deleted", null, Instant.now()));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable UUID id, @Valid @RequestBody AddTeamMemberRequest request) {
        teamService.addMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Member added to team", null, Instant.now()));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID teamId, @PathVariable UUID userId) {
        teamService.removeMember(teamId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Member removed from team", null, Instant.now()));
    }
}
