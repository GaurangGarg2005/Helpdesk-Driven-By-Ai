package com.helpdeskAi.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.extractAllClaims(token);

                UUID userId = UUID.fromString(claims.getSubject());
                String email = claims.get("email", String.class);
                String firstName = claims.get("firstName", String.class);
                String lastName = claims.get("lastName", String.class);
                String orgIdStr = claims.get("orgId", String.class);
                UUID orgId = orgIdStr != null ? UUID.fromString(orgIdStr) : null;
                String orgSlug = claims.get("orgSlug", String.class);
                String role = claims.get("role", String.class);

                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);

                UserPrincipal principal = UserPrincipal.builder()
                        .id(userId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .orgId(orgId)
                        .orgSlug(orgSlug)
                        .role(role)
                        .permissions(permissions != null ? permissions : List.of())
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (orgId != null) {
                    TenantContext.setTenantId(orgId);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
