package com.techtune.nexus.user.domain;

import com.techtune.nexus.common.domain.UuidIdentity;
import com.techtune.nexus.tenant.domain.Tenant;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_tenant_memberships")
public class UserTenantMembership extends UuidIdentity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private MembershipRole role = MembershipRole.MEMBER;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected UserTenantMembership() {
        // JPA
    }

    public UserTenantMembership(User user, Tenant tenant, MembershipRole role) {
        this.user = user;
        this.tenant = tenant;
        this.role = role;
        this.updatedAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public MembershipRole getRole() {
        return role;
    }
}
