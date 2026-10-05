package com.helpdeskAi.auth.controller;

import com.helpdeskAi.auth.dto.*;
import com.helpdeskAi.auth.service.AuthService;
import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Company Admin registration — creates a new organization with ADMIN role. */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Registration successful", response, Instant.now()));
    }

    /** Agent registration — joins an existing organization with AGENT role. */
    @PostMapping("/register/agent")
    public ResponseEntity<ApiResponse<AuthResponse>> registerAgent(@Valid @RequestBody CustomerRegisterRequest request) {
        AuthResponse response = authService.registerAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Agent account created", response, Instant.now()));
    }

    /** Customer self-registration — joins an existing organization by name. */
    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponse>> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        AuthResponse response = authService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Customer account created", response, Instant.now()));
    }

    /**
     * Public endpoint — search organizations by name for the customer registration dropdown.
     * Returns only id, name, slug (no sensitive data).
     */
    @GetMapping("/organizations/search")
    public ResponseEntity<ApiResponse<List<OrgSearchResult>>> searchOrganizations(
            @RequestParam(required = false, defaultValue = "") String name) {
        List<OrgSearchResult> results = authService.searchOrganizations(name);
        return ResponseEntity.ok(new ApiResponse<>(true, "Organizations found", results, Instant.now()));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Login successful", response, Instant.now()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Token refreshed", response, Instant.now()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out successfully", null, Instant.now()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UUID userId = getCurrentUserId();
        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved", response, Instant.now()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@RequestBody UpdateProfileRequest request) {
        UUID userId = getCurrentUserId();
        UserResponse response = authService.updateProfile(userId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated", response, Instant.now()));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getId();
    }
}
