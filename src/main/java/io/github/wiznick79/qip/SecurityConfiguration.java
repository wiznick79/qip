package io.github.wiznick79.qip;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "qip.security", name = "enabled", matchIfMissing = true)
    UserDetailsService localUsers(
            @Value("${qip.security.users.investigator.username}") String investigatorUsername,
            @Value("${qip.security.users.investigator.password}") String investigatorPassword,
            @Value("${qip.security.users.reviewer.username}") String reviewerUsername,
            @Value("${qip.security.users.reviewer.password}") String reviewerPassword,
            @Value("${qip.security.users.admin.username}") String adminUsername,
            @Value("${qip.security.users.admin.password}") String adminPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(investigatorUsername)
                        .password("{noop}" + investigatorPassword)
                        .roles("INVESTIGATOR")
                        .build(),
                User.withUsername(reviewerUsername)
                        .password("{noop}" + reviewerPassword)
                        .roles("REVIEWER")
                        .build(),
                User.withUsername(adminUsername)
                        .password("{noop}" + adminPassword)
                        .roles("ADMIN", "INVESTIGATOR", "REVIEWER")
                        .build());
    }

    @Bean
    @ConditionalOnProperty(prefix = "qip.security", name = "enabled", matchIfMissing = true)
    SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/session")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/session/login")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/findings/*/reviews")
                        .hasAnyRole("REVIEWER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/investigations/*/closure")
                        .hasAnyRole("INVESTIGATOR", "ADMIN")
                        .requestMatchers("/api/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .formLogin(form -> form.loginProcessingUrl("/api/session/login")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                        .failureHandler((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Authentication failed",
                                "The username or password is invalid."))
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/api/session/logout")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value())))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                "Authentication required",
                                "Sign in before accessing this QIP resource."))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.FORBIDDEN,
                                "Access denied",
                                "Your QIP role does not permit this action.")));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "qip.security", name = "enabled", havingValue = "false")
    SecurityFilterChain testSecurity(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    private static void writeProblem(HttpServletResponse response, HttpStatus status, String title, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                        {"type":"about:blank","title":"%s","status":%d,"detail":"%s"}
                        """.formatted(title, status.value(), detail).strip());
    }
}
