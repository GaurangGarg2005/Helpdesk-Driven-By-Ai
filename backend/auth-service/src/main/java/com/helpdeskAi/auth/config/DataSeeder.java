package com.helpdeskAi.auth.config;

import com.helpdeskAi.auth.model.*;
import com.helpdeskAi.auth.model.enums.Role;
import com.helpdeskAi.auth.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${owner.email:owner@helpdeskai.com}")
    private String ownerEmail;

    @Value("${owner.password:HelpDesk@Owner2024}")
    private String ownerPassword;

    // All permissions in the system
    private static final Map<String, String> ALL_PERMISSIONS = new LinkedHashMap<>() {{
        put("ticket:read", "View tickets");
        put("ticket:create", "Create tickets");
        put("ticket:update", "Update tickets");
        put("ticket:delete", "Delete tickets");
        put("ticket:assign", "Assign tickets to agents/teams");
        put("ticket:change_status", "Change ticket status");
        put("ticket:change_priority", "Change ticket priority");
        put("ticket:internal_note", "Add internal notes");
        put("ticket:view_internal", "View internal notes");
        put("chat:access", "Access live chat");
        put("chat:assign", "Assign chat conversations");
        put("kb:read", "View knowledge base articles");
        put("kb:write", "Create/edit knowledge base articles");
        put("kb:delete", "Delete knowledge base articles");
        put("kb:publish", "Publish knowledge base articles");
        put("analytics:read", "View analytics dashboards");
        put("analytics:export", "Export analytics data");
        put("org:manage", "Manage organization settings");
        put("org:branding", "Manage organization branding");
        put("org:billing", "Manage billing and subscription");
        put("users:read", "View team members");
        put("users:invite", "Invite new members");
        put("users:manage", "Manage member roles and status");
        put("users:remove", "Remove members from organization");
        put("teams:read", "View teams");
        put("teams:manage", "Create/edit/delete teams");
        put("departments:read", "View departments");
        put("departments:manage", "Create/edit/delete departments");
        put("workspaces:read", "View workspaces");
        put("workspaces:manage", "Create/edit/delete workspaces");
        put("api_keys:read", "View API keys");
        put("api_keys:manage", "Create/revoke API keys");
        put("webhooks:read", "View webhooks");
        put("webhooks:manage", "Create/edit/delete webhooks");
        put("audit:read", "View audit logs");
        put("sla:read", "View SLA policies");
        put("sla:manage", "Create/edit SLA policies");
        put("ai:suggestions", "View AI suggestions");
        put("ai:feedback", "Provide feedback on AI predictions");
    }};

    // Role → permission mappings
    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
        "OWNER", new ArrayList<>(ALL_PERMISSIONS.keySet()),

        "ADMIN", List.of(
            "ticket:read", "ticket:create", "ticket:update", "ticket:delete",
            "ticket:assign", "ticket:change_status", "ticket:change_priority",
            "ticket:internal_note", "ticket:view_internal",
            "chat:access", "chat:assign",
            "kb:read", "kb:write", "kb:delete", "kb:publish",
            "analytics:read", "analytics:export",
            "org:manage", "org:branding",
            "users:read", "users:invite", "users:manage", "users:remove",
            "teams:read", "teams:manage",
            "departments:read", "departments:manage",
            "workspaces:read", "workspaces:manage",
            "api_keys:read", "api_keys:manage",
            "webhooks:read", "webhooks:manage",
            "audit:read",
            "sla:read", "sla:manage",
            "ai:suggestions", "ai:feedback"
        ),

        "AGENT", List.of(
            "ticket:read", "ticket:create", "ticket:update",
            "ticket:change_status",
            "ticket:internal_note", "ticket:view_internal",
            "kb:read",
            "analytics:read",
            "departments:read",
            "workspaces:read",
            "sla:read",
            "ai:suggestions", "ai:feedback"
        ),

        "CUSTOMER", List.of(
            "ticket:create",
            "ticket:read"
        )
    );

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedOwnerAccount();
    }

    private void seedPermissions() {
        if (permissionRepository.count() > 0) {
            log.info("Permissions already seeded — skipping.");
            // Still seed OWNER account even if permissions exist
            return;
        }

        log.info("🌱 Seeding permissions and role mappings...");

        Map<String, Permission> savedPermissions = new HashMap<>();
        for (var entry : ALL_PERMISSIONS.entrySet()) {
            Permission permission = Permission.builder()
                    .name(entry.getKey())
                    .description(entry.getValue())
                    .build();
            savedPermissions.put(entry.getKey(), permissionRepository.save(permission));
        }
        log.info("  ✅ Created {} permissions", savedPermissions.size());

        int mappingCount = 0;
        for (var roleEntry : ROLE_PERMISSIONS.entrySet()) {
            String role = roleEntry.getKey();
            for (String permName : roleEntry.getValue()) {
                Permission perm = savedPermissions.get(permName);
                if (perm != null) {
                    RolePermission rp = RolePermission.builder()
                            .role(role)
                            .permissionId(perm.getId())
                            .build();
                    rolePermissionRepository.save(rp);
                    mappingCount++;
                }
            }
        }
        log.info("  ✅ Created {} role-permission mappings", mappingCount);
        log.info("🌱 Permission seeding complete!");
    }

    @Transactional
    public void seedOwnerAccount() {
        if (userRepository.existsByEmail(ownerEmail)) {
            log.info("✅ OWNER account already exists — skipping seed.");
            return;
        }

        log.info("🌱 Seeding platform OWNER account: {}", ownerEmail);

        // Create OWNER user
        User owner = User.builder()
                .email(ownerEmail)
                .passwordHash(passwordEncoder.encode(ownerPassword))
                .firstName("Platform")
                .lastName("Owner")
                .isActive(true)
                .emailVerified(true)
                .build();
        owner = userRepository.save(owner);

        // Create a platform-level org
        Organization platformOrg = Organization.builder()
                .name("HelpDeskAI Platform")
                .slug("helpdeskai-platform")
                .plan("enterprise")
                .primaryColor("#4F46E5")
                .build();
        platformOrg = organizationRepository.save(platformOrg);

        // Create OWNER membership
        OrgMember member = OrgMember.builder()
                .organizationId(platformOrg.getId())
                .userId(owner.getId())
                .role(Role.OWNER.name())
                .isActive(true)
                .build();
        orgMemberRepository.save(member);

        log.info("✅ OWNER account seeded successfully! Email: {}", ownerEmail);
    }
}
