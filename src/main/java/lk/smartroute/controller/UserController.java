package lk.smartroute.controller;

import jakarta.validation.Valid;
import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.Role;
import lk.smartroute.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return users.currentProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        return users.updateProfile(authentication.getName(), request);
    }

    @PatchMapping("/me/password")
    public void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        users.changePassword(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN','IT_SUPPORT')")
    public PageResponse<UserResponse> list(
        @RequestParam(required = false) Role role,
        @RequestParam(required = false) Boolean active,
        @PageableDefault(sort = "id") Pageable pageable
    ) {
        return users.listUsers(role, active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER_SERVICE','OPERATIONS_MANAGER','SYSTEM_ADMIN','IT_SUPPORT')")
    public UserResponse get(@PathVariable Long id) {
        return users.getUser(id);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public UserResponse updateRole(
        Authentication authentication,
        @PathVariable Long id,
        @Valid @RequestBody RoleUpdateRequest request
    ) {
        return users.updateRole(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','IT_SUPPORT')")
    public UserResponse updateActive(
        Authentication authentication,
        @PathVariable Long id,
        @Valid @RequestBody ActiveUpdateRequest request
    ) {
        return users.updateActive(id, request, authentication.getName());
    }
}
