package dev.antoniogrillo.testldap.security;

import dev.antoniogrillo.testldap.model.Utente;
import dev.antoniogrillo.testldap.repository.UtenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.Collection;

/**
 * Chiamato da Spring Security dopo un bind LDAP andato a buon fine.
 * <p>
 * Responsabilita' (solo autenticazione, non profilazione):
 * <ol>
 *   <li>legge gli attributi LDAP (cn, mail);</li>
 *   <li>JIT provisioning: se l'utente non esiste in DB (match su usernameLdap) lo crea;</li>
 *   <li>se esiste, aggiorna l'email (nome/cognome/codiceFiscale sono immutabili);</li>
 *   <li>restituisce l'entita' {@link Utente}, che implementa gia' {@code UserDetails}.</li>
 * </ol>
 * Le authorities derivano dai {@code diritti} dell'utente: un utente appena
 * provisionato non ha diritti finche' non viene profilato dall'applicazione.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LdapUserDetailsMapper implements UserDetailsContextMapper {

    private final UtenteRepository utenteRepository;

    @Override
    @Transactional
    public UserDetails mapUserFromContext(DirContextOperations ctx,
                                          String username,
                                          Collection<? extends GrantedAuthority> authorities) {

        log.info("LDAP bind OK - username='{}' DN='{}'", username, ctx.getDn());
        logAllAttributes(ctx);

        String cn = ctx.getStringAttribute("cn");
        String mail = ctx.getStringAttribute("mail");

        String nome = (cn != null && cn.contains(" "))
                ? cn.substring(0, cn.indexOf(" ")) : cn;
        String cognome = (cn != null && cn.contains(" "))
                ? cn.substring(cn.indexOf(" ") + 1) : "";

        Utente utente = utenteRepository.findByUsernameLdap(username).orElse(null);

        if (utente == null) {
            log.info("LDAP JIT provisioning - nuovo utente '{}'", username);
            utente = new Utente();
            utente.setUsernameLdap(username);
            utente.setNome(nome);
            utente.setCognome(cognome);
            utente.setEmailElis(mail != null ? mail : username + "@elis.org");
            // Nessun diritto/annoIscrizione: la profilazione e' responsabilita'
            // dell'applicazione, non del layer di autenticazione.
            utente = utenteRepository.save(utente);
        } else if (mail != null && !mail.equals(utente.getEmailElis())) {
            // nome/cognome/codiceFiscale sono @Column(updatable = false): immutabili.
            // L'email e' l'unico dato anagrafico che aggiorniamo dall'LDAP.
            log.info("LDAP update email utente '{}' -> '{}'", username, mail);
            utente.setEmailElis(mail);
            utente = utenteRepository.save(utente);
        }

        return utente;
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("LDAP in sola lettura");
    }

    /** Diagnostica: loga a DEBUG tutti gli attributi LDAP ricevuti (utile in setup). */
    private void logAllAttributes(DirContextOperations ctx) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            NamingEnumeration<? extends Attribute> allAttrs = ctx.getAttributes().getAll();
            while (allAttrs.hasMore()) {
                Attribute attr = allAttrs.next();
                log.debug("LDAP attr '{}' = {}", attr.getID(), attr.get());
            }
        } catch (NamingException e) {
            log.warn("LDAP impossibile enumerare gli attributi: {}", e.getMessage());
        }
    }
}
