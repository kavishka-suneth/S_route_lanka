package lk.smartroute.security;

import lk.smartroute.model.Role;
import org.springframework.security.core.Authentication;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static boolean hasAnyRole(Authentication authentication, Role... roles) {
        if (authentication == null) {
            return false;
        }
        Set<String> requiredAuthorities = Arrays.stream(roles)
            .map(Role::authority)
            .collect(Collectors.toSet());
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> requiredAuthorities.contains(authority.getAuthority()));
    }
}
