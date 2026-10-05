package com.helpdeskAi.auth.repository;

import com.helpdeskAi.auth.model.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {
    Optional<OrgMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    List<OrgMember> findByUserId(UUID userId);
    List<OrgMember> findByOrganizationId(UUID organizationId);
    Optional<OrgMember> findFirstByUserId(UUID userId);
}
