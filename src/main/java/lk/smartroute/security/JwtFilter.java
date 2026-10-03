package lk.smartroute.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.smartroute.repository.UserRepository;
import lk.smartroute.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final String TOKEN_COOKIE = "SRL_AUTH";
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String token = bearerToken(request);
        if (token == null) {
            token = cookieToken(request);
        }
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        chain.doFilter(request, response);
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String cookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void authenticate(String token) {
        try {
            String email = jwtService.email(token);
            users.findByEmailIgnoreCase(email)
                .filter(user -> user.isActive() && user.isEmailVerified())
                .ifPresent(user -> {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().authority());
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user.getEmail(), null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
