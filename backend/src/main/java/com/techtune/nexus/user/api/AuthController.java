package com.techtune.nexus.user.api;

import com.techtune.nexus.user.application.UserService;
import com.techtune.nexus.user.application.EmailService;
import com.techtune.nexus.user.domain.User;
import com.techtune.nexus.user.domain.UserTenantMembership;
import com.techtune.nexus.user.domain.MembershipRole;
import com.techtune.nexus.user.domain.PasswordResetToken;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import com.techtune.nexus.user.persistence.UserTenantMembershipRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final TenantRepository tenantRepository;
    private final UserTenantMembershipRepository membershipRepository;
    private final EmailService emailService;

    public AuthController(UserService userService,
                           TenantRepository tenantRepository,
                           UserTenantMembershipRepository membershipRepository,
                           org.springframework.beans.factory.ObjectProvider<EmailService> emailServiceProvider) {
        this.userService = userService;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.emailService = emailServiceProvider.getIfAvailable();
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.email, request.password, request.fullName);
        return ResponseEntity.status(201).body(UserResponse.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        var userOpt = userService.authenticateUser(request.email, request.password);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        User user = userOpt.get();
        String token = user.getId().toString();
        return ResponseEntity.ok(new LoginResponse(token, UserResponse.from(user)));
    }

    @PostMapping("/create-tenant")
    @Operation(summary = "Create a new tenant and make the user admin")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof com.techtune.nexus.security.auth.UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        
        com.techtune.nexus.security.auth.UserPrincipal principal = 
            (com.techtune.nexus.security.auth.UserPrincipal) authentication.getPrincipal();
        
        Tenant tenant = new Tenant(request.tenantName);
        tenant = tenantRepository.save(tenant);
        
        UserTenantMembership membership = userService.createMembership(
            principal.getUserId(), 
            tenant.getId(), 
            MembershipRole.ADMIN
        );
        
        return ResponseEntity.status(201).body(TenantResponse.from(tenant));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<UserResponse> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication.getPrincipal() instanceof com.techtune.nexus.security.auth.UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        
        com.techtune.nexus.security.auth.UserPrincipal principal = 
            (com.techtune.nexus.security.auth.UserPrincipal) authentication.getPrincipal();
        
        return ResponseEntity.ok(new UserResponse(principal.getUserId().toString(), principal.getEmail(), principal.getFullName()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        if (emailService == null || !emailService.isEmailConfigured()) {
            return ResponseEntity.status(503).body(Map.of("error", "Email service not configured"));
        }

        try {
            PasswordResetToken token = userService.createPasswordResetToken(request.email);
            emailService.sendPasswordResetEmail(request.email, token.getToken());
            return ResponseEntity.ok(Map.of("message", "Password reset email sent if email exists"));
        } catch (IllegalArgumentException e) {
            // Don't reveal if email exists
            return ResponseEntity.ok(Map.of("message", "Password reset email sent if email exists"));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.token, request.newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired token"));
        }
    }

    record RegisterRequest(String email, String password, String fullName) {}
    record LoginRequest(String email, String password) {}
    record CreateTenantRequest(String tenantName) {}
    record LoginResponse(String token, UserResponse user) {}
    record ForgotPasswordRequest(String email) {}
    record ResetPasswordRequest(String token, String newPassword) {}
    
    record UserResponse(String id, String email, String fullName) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId().toString(), user.getEmail(), user.getFullName());
        }
    }
    
    record TenantResponse(String id, String name, String status) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(tenant.getId().toString(), tenant.getName(), tenant.getStatus().name());
        }
    }
}
