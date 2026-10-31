package com.techtune.nexus.user.application;

import com.techtune.nexus.user.domain.User;
import com.techtune.nexus.user.domain.UserTenantMembership;
import com.techtune.nexus.user.domain.MembershipRole;
import com.techtune.nexus.user.persistence.UserRepository;
import com.techtune.nexus.user.persistence.UserTenantMembershipRepository;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserTenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                      UserTenantMembershipRepository membershipRepository,
                      TenantRepository tenantRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        String passwordHash = passwordEncoder.encode(password);
        User user = new User(email, passwordHash, fullName);
        return userRepository.save(user);
    }

    public Optional<User> authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!user.getIsActive()) {
            return Optional.empty();
        }
        if (passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public UserTenantMembership createMembership(UUID userId, UUID tenantId, MembershipRole role) {
        if (membershipRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new IllegalArgumentException("Membership already exists");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        UserTenantMembership membership = new UserTenantMembership(user, tenant, role);
        return membershipRepository.save(membership);
    }

    public List<UserTenantMembership> getUserMemberships(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }

    public boolean hasRoleInTenant(UUID userId, UUID tenantId, MembershipRole role) {
        return membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(m -> m.getRole() == role)
                .orElse(false);
    }

    public boolean hasAnyMembershipInTenant(UUID userId, UUID tenantId) {
        return membershipRepository.existsByUserIdAndTenantId(userId, tenantId);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
