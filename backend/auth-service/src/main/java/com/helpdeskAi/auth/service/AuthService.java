package com.helpdeskAi.auth.service;

import com.helpdeskAi.auth.dto.*;
import com.helpdeskAi.auth.model.*;
import com.helpdeskAi.auth.model.enums.Role;
import com.helpdeskAi.auth.repository.*;
import com.helpdeskAi.common.exception.BadRequestException;
import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.exception.UnauthorizedException;
import com.helpdeskAi.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${owner.email:owner@helpdeskai.com}")
    private String ownerEmail;

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Company Admin registration — creates a new organization and assigns ADMIN role.
     * OWNER email is reserved and cannot be used here.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        guardOwnerEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isActive(true)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        String slug = generateSlug(request.getOrganizationName());
        Organization org = Organization.builder()
                .name(request.getOrganizationName())
                .slug(slug)
                .plan("free")
                .primaryColor("#4F46E5")
                .build();
        org = organizationRepository.save(org);

        // Company admin gets ADMIN role (not OWNER)
        OrgMember member = OrgMember.builder()
                .organizationId(org.getId())
                .userId(user.getId())
                .role(Role.ADMIN.name())
                .isActive(true)
                .build();
        orgMemberRepository.save(member);

        List<String> permissions = getPermissionsForRole(Role.ADMIN.name());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                org.getId(), org.getSlug(), Role.ADMIN.name(), permissions);
        String refreshToken = createRefreshToken(user.getId());

        log.info("Company admin registered: {} (org: {})", user.getEmail(), org.getSlug());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, org, Role.ADMIN.name(), permissions))
                .build();
    }

    /**
     * Agent registration — joins an existing organization selected from the dropdown.
     * OWNER email is reserved and cannot be used here.
     */
    @Transactional
    public AuthResponse registerAgent(CustomerRegisterRequest request) {
        guardOwnerEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Organization org = organizationRepository.findByNameIgnoreCase(request.getCompanyName())
                .orElseThrow(() -> new BadRequestException(
                        "Company '" + request.getCompanyName() + "' not found. Please check the name or contact your company admin."));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : "")
                .isActive(true)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        OrgMember member = OrgMember.builder()
                .organizationId(org.getId())
                .userId(user.getId())
                .role(Role.AGENT.name())
                .isActive(true)
                .build();
        orgMemberRepository.save(member);

        List<String> permissions = getPermissionsForRole(Role.AGENT.name());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                org.getId(), org.getSlug(), Role.AGENT.name(), permissions);
        String refreshToken = createRefreshToken(user.getId());

        log.info("Agent registered: {} (org: {})", user.getEmail(), org.getSlug());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, org, Role.AGENT.name(), permissions))
                .build();
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        OrgMember member = orgMemberRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User has no organization membership"));

        Organization org = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", member.getOrganizationId()));

        List<String> permissions = getPermissionsForRole(member.getRole());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                org.getId(), org.getSlug(), member.getRole(), permissions);
        String refreshToken = createRefreshToken(user.getId());

        log.info("User logged in: {} (role: {})", user.getEmail(), member.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, org, member.getRole(), permissions))
                .build();
    }

    // ── Token Refresh ─────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (storedToken.getRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", storedToken.getUserId()));

        OrgMember member = orgMemberRepository.findFirstByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("User has no organization membership"));

        Organization org = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", member.getOrganizationId()));

        List<String> permissions = getPermissionsForRole(member.getRole());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                org.getId(), org.getSlug(), member.getRole(), permissions);
        String newRefreshToken = createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, org, member.getRole(), permissions))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        OrgMember member = orgMemberRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgMember", "userId", userId));

        Organization org = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", member.getOrganizationId()));

        List<String> permissions = getPermissionsForRole(member.getRole());

        return buildUserResponse(user, org, member.getRole(), permissions);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        user = userRepository.save(user);

        OrgMember member = orgMemberRepository.findFirstByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("OrgMember", "userId", userId));
        Organization org = organizationRepository.findById(member.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", member.getOrganizationId()));
        List<String> permissions = getPermissionsForRole(member.getRole());

        return buildUserResponse(user, org, member.getRole(), permissions);
    }

    // ── Customer Registration ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerCustomer(CustomerRegisterRequest request) {
        guardOwnerEmail(request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        Organization org = organizationRepository.findByNameIgnoreCase(request.getCompanyName())
                .orElseThrow(() -> new BadRequestException(
                        "Company '" + request.getCompanyName() + "' not found. Please check the name or contact your company admin."));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName() != null ? request.getLastName() : "")
                .isActive(true)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        OrgMember member = OrgMember.builder()
                .organizationId(org.getId())
                .userId(user.getId())
                .role(Role.CUSTOMER.name())
                .isActive(true)
                .build();
        orgMemberRepository.save(member);

        List<String> permissions = getPermissionsForRole(Role.CUSTOMER.name());

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                org.getId(), org.getSlug(), Role.CUSTOMER.name(), permissions);
        String refreshToken = createRefreshToken(user.getId());

        log.info("Customer registered: {} (org: {})", user.getEmail(), org.getSlug());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(buildUserResponse(user, org, Role.CUSTOMER.name(), permissions))
                .build();
    }

    /**
     * Search organizations by name — used by customer/agent registration dropdown.
     */
    public List<OrgSearchResult> searchOrganizations(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }
        return organizationRepository
                .findByNameContainingIgnoreCaseOrderByName(query.trim())
                .stream()
                .limit(10)
                .filter(org -> !org.getSlug().equals("helpdeskai-platform")) // hide platform org
                .map(org -> new OrgSearchResult(org.getId(), org.getName(), org.getSlug()))
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Prevents anyone from registering with the reserved OWNER email.
     */
    private void guardOwnerEmail(String email) {
        if (ownerEmail != null && ownerEmail.equalsIgnoreCase(email)) {
            throw new BadRequestException("This email address is reserved and cannot be used for registration.");
        }
    }

    private List<String> getPermissionsForRole(String role) {
        return rolePermissionRepository.findByRole(role).stream()
                .map(rp -> rp.getPermission().getName())
                .collect(Collectors.toList());
    }

    private String createRefreshToken(UUID userId) {
        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .token(tokenValue)
                .expiresAt(Instant.now().plusMillis(604800000)) // 7 days
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
        return tokenValue;
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        while (organizationRepository.existsBySlug(slug)) {
            String suffix = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
            slug = base + "-" + suffix;
        }
        return slug;
    }

    private UserResponse buildUserResponse(User user, Organization org, String role, List<String> permissions) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(role)
                .organizationId(org.getId())
                .organizationName(org.getName())
                .organizationSlug(org.getSlug())
                .permissions(permissions)
                .build();
    }
}
