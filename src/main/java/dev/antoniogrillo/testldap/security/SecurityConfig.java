package dev.antoniogrillo.testldap.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final LdapUserDetailsMapper ldapUserDetailsMapper;

    @Value("${spring.ldap.urls:ldap://localhost:389}")
    private String ldapUrl;

    @Value("${spring.ldap.base:dc=example,dc=local}")
    private String ldapBase;

    @Value("${app.ldap.manager-dn:}")
    private String ldapManagerDn;

    @Value("${app.ldap.manager-password:}")
    private String ldapManagerPassword;

    @Value("${app.ldap.user-search-filter:(sAMAccountName={0})}")
    private String userSearchFilter;

    @Value("${app.ldap.form-login-enabled:true}")
    private boolean ldapFormLoginEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(
                    "/login", "/login/**",
                    "/error",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            );

        if (ldapFormLoginEnabled) {
            http.formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(ldapAuthSuccessHandler())
                .failureHandler(authFailureHandler())
                .permitAll()
            );
        }

        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; "
                    + "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                    + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                    + "font-src 'self' https://cdn.jsdelivr.net https://fonts.gstatic.com; "
                    + "img-src 'self' data: blob:; "
                    + "connect-src 'self'; "
                    + "frame-ancestors 'self'; "
                    + "base-uri 'self'"))
                .frameOptions(frame -> frame.sameOrigin())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(rp -> rp.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            .exceptionHandling(ex -> ex
                // sendRedirect garantisce sempre un GET, evitando 405 su forward di POST
                .accessDeniedHandler((request, response, e) -> {
                    if (e instanceof InvalidCsrfTokenException || e instanceof MissingCsrfTokenException) {
                        response.sendRedirect("/login?sessione-scaduta");
                    } else {
                        response.sendRedirect("/accesso-negato");
                    }
                })
            )
            .csrf(csrf -> {
                // Cookie-based CSRF token: SPA fetch() legge XSRF-TOKEN e lo rimanda come X-XSRF-TOKEN
                CsrfTokenRequestAttributeHandler handler = new CsrfTokenRequestAttributeHandler();
                handler.setCsrfRequestAttributeName(null); // espone il token raw per SPA
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(handler);
                // Aggiungi .ignoringRequestMatchers("/api/webhook/**") se hai callback esterne firmate
            })
            .sessionManagement(session -> session
                .invalidSessionUrl("/login?sessione-scaduta")
                .maximumSessions(5)
            );

        return http.build();
    }

    /** Necessario perche' maximumSessions tenga traccia delle sessioni concorrenti. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    private AuthenticationSuccessHandler ldapAuthSuccessHandler() {
        return (request, response, authentication) -> {
            log.info("LDAP login OK - user='{}'", authentication.getName());
            response.sendRedirect(SafeRedirects.sanitize(request.getParameter("redirect")));
        };
    }

    private AuthenticationFailureHandler authFailureHandler() {
        return (request, response, exception) -> {
            Throwable root = exception.getCause() != null ? exception.getCause() : exception;
            log.warn("AUTH FAILURE - type='{}' message='{}' rootCause='{}: {}'",
                exception.getClass().getSimpleName(), exception.getMessage(),
                root.getClass().getSimpleName(), root.getMessage());
            response.sendRedirect("/login?error");
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.ldap.form-login-enabled", havingValue = "true", matchIfMissing = true)
    public LdapAuthenticationProvider ldapAuthenticationProvider() {
        DefaultSpringSecurityContextSource contextSource =
            new DefaultSpringSecurityContextSource(ldapUrl + "/" + ldapBase);

        if (!ldapManagerDn.isBlank()) {
            log.info("LDAP INIT - managerDN='{}' url='{}/{}' filter='{}'",
                ldapManagerDn, ldapUrl, ldapBase, userSearchFilter);
            contextSource.setUserDn(ldapManagerDn);
            contextSource.setPassword(ldapManagerPassword);
        } else {
            log.warn("LDAP INIT - nessun manager DN, bind anonimo (potrebbe fallire su AD)");
        }
        contextSource.afterPropertiesSet();

        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
            "",               // ricerca dalla base: copre tutti i sottoalberi (Global Catalog)
            userSearchFilter,
            contextSource
        );

        BindAuthenticator authenticator = new DiagnosticBindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator);
        provider.setUserDetailsContextMapper(ldapUserDetailsMapper);
        return provider;
    }

    @Bean
    @ConditionalOnProperty(name = "app.ldap.form-login-enabled", havingValue = "true", matchIfMissing = true)
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(ldapAuthenticationProvider());
    }

    /** Loga ogni fase del bind LDAP: search -> bind -> errore. Utile in diagnostica prod. */
    private static class DiagnosticBindAuthenticator extends BindAuthenticator {
        private static final Logger diagLog =
            LoggerFactory.getLogger(DiagnosticBindAuthenticator.class);

        DiagnosticBindAuthenticator(DefaultSpringSecurityContextSource cs) {
            super(cs);
        }

        @Override
        public DirContextOperations authenticate(Authentication authentication) {
            String username = authentication.getName();
            diagLog.debug("LDAP [1/3] SEARCH START - username='{}'", username);
            try {
                DirContextOperations result = super.authenticate(authentication);
                diagLog.debug("LDAP [3/3] BIND OK - username='{}' resolvedDN='{}'",
                    username, result.getDn());
                return result;
            } catch (UsernameNotFoundException e) {
                diagLog.warn("LDAP [2/3] SEARCH FAILED - username='{}': {}", username, e.getMessage());
                throw e;
            } catch (BadCredentialsException e) {
                diagLog.warn("LDAP [2/3] BIND FAILED - username='{}': {}", username, e.getMessage());
                throw e;
            } catch (Exception e) {
                diagLog.error("LDAP [2/3] ERROR - username='{}' type='{}': {}",
                    username, e.getClass().getSimpleName(), e.getMessage(), e);
                throw e;
            }
        }
    }
}
