package com.helpdeskAi.org.repository;

import com.helpdeskAi.org.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    List<TeamMember> findByTeamId(UUID teamId);
    List<TeamMember> findByUserId(UUID userId);
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);
    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
