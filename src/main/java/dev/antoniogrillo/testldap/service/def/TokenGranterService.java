package dev.antoniogrillo.testldap.service.def;

import dev.antoniogrillo.testldap.model.Utente;
import org.springframework.security.core.AuthenticatedPrincipal;

public interface TokenGranterService {
    String getToken(AuthenticatedPrincipal a);
    Utente getUtente(String token);
}
