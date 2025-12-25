package com.neobrutalism.gateway.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Custom OAuth2 Login Success Handler
 *
 * After successful OAuth2 authentication:
 * 1. Extract user info from OIDC token
 * 2. Log authentication event
 * 3. Redirect to application home
 */
public class OAuth2LoginSuccessHandler implements ServerAuthenticationSuccessHandler {

    @Override
    public Mono<Void> onAuthenticationSuccess(
            WebFilterExchange webFilterExchange,
            Authentication authentication) {

        ServerWebExchange exchange = webFilterExchange.getExchange();

        // Extract OIDC user info
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            // Log successful authentication
            System.out.println("✅ OAuth2 Login Success:");
            System.out.println("   User: " + oidcUser.getPreferredUsername());
            System.out.println("   Email: " + oidcUser.getEmail());
            System.out.println("   Subject: " + oidcUser.getSubject());

            // Optional: Store additional user info in session attributes
            exchange.getSession().doOnNext(session -> {
                session.getAttributes().put("userId", oidcUser.getSubject());
                session.getAttributes().put("username", oidcUser.getPreferredUsername());
                session.getAttributes().put("email", oidcUser.getEmail());
            }).subscribe();
        }

        // Redirect to frontend application
        String redirectUrl = System.getenv().getOrDefault(
            "OAUTH2_SUCCESS_REDIRECT_URL",
            "http://localhost:3000"
        );

        return exchange.getSession()
            .doOnNext(session -> session.save().subscribe())
            .then(Mono.fromRunnable(() -> {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create(redirectUrl));
            }))
            .then(exchange.getResponse().setComplete());
    }
}
