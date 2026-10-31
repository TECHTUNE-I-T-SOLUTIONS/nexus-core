package com.techtune.nexus.user.persistence;

import com.techtune.nexus.user.domain.UserTenantMembership;
import com.techtune.nexus.user.domain.MembershipRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTenantMembershipRepository extends JpaRepository<UserTenantMembership, UUID> {
    List<UserTenantMembership> findByUserId(UUID userId);
    Optional<UserTenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    List<UserTenantMembership> findByTenantId(UUID tenantId);
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);
}
