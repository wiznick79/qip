package io.github.wiznick79.qip;

import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
class SessionController {

    @GetMapping
    SessionResponse session(Authentication authentication, CsrfToken csrfToken) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        List<String> roles = authenticated
                ? authentication.getAuthorities().stream()
                        .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                        .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                        .sorted()
                        .toList()
                : List.of();
        return new SessionResponse(
                authenticated,
                authenticated ? authentication.getName() : null,
                roles,
                csrfToken.getHeaderName(),
                csrfToken.getToken());
    }
}
