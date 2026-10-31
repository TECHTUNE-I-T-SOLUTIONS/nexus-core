package com.techtune.nexus.user.application;

import com.techtune.nexus.user.domain.MembershipRole;
import com.techtune.nexus.user.domain.UserTenantMembership;
import com.techtune.nexus.user.persistence.UserTenantMembershipRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {

    private final UserTenantMembershipRepository membershipRepository;

    public AuthorizationService(UserTenantMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public boolean hasAccessToTenant(UUID userId, UUID tenantId) {
        return membershipRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    public UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof com.techtune.nexus.security.auth.UserPrincipal) {
            return ((com.techtune.nexus.security.auth.UserPrincipal) authentication.getPrincipal()).getUserId();
        }
        return null;
    }

    public boolean hasRoleInTenant(UUID userId, UUID tenantId, MembershipRole role) {
        return membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(m -> m.getRole() == role)
                .orElse(false);
    }
}
