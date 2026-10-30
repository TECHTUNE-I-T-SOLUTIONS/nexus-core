package com.techtune.nexus.tenant.domain;

import com.techtune.nexus.common.domain.UuidIdentity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant extends UuidIdentity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TenantStatus status = TenantStatus.ACTIVE;

    protected Tenant() {
        // JPA
    }

    public Tenant(String name) {
        this.name = name;
        this.status = TenantStatus.ACTIVE;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void suspend() {
        if (this.status == TenantStatus.DELETED) {
            throw new IllegalStateException("Cannot suspend a deleted tenant");
        }
        this.status = TenantStatus.SUSPENDED;
    }

    public void activate() {
        if (this.status == TenantStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted tenant");
        }
        this.status = TenantStatus.ACTIVE;
    }

    public void delete() {
        this.status = TenantStatus.DELETED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(name, tenant.name) && status == tenant.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, status);
    }
}
