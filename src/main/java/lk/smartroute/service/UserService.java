package lk.smartroute.service;

import lk.smartroute.dto.ApiDtos.*;
import lk.smartroute.model.Role;
import org.springframework.data.domain.Pageable;

public interface UserService {
    UserResponse currentProfile(String email);

    UserResponse updateProfile(String email, ProfileUpdateRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    PageResponse<UserResponse> listUsers(Role role, Boolean active, Pageable pageable);

    UserResponse getUser(Long id);

    UserResponse updateRole(Long id, RoleUpdateRequest request, String actorEmail);

    UserResponse updateActive(Long id, ActiveUpdateRequest request, String actorEmail);
}
