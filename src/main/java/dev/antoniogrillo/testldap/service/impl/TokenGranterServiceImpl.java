package dev.antoniogrillo.testldap.service.impl;

import dev.antoniogrillo.testldap.model.Diritto;
import dev.antoniogrillo.testldap.model.Utente;
import dev.antoniogrillo.testldap.service.def.TokenGranterService;
import dev.antoniogrillo.testldap.service.def.UtenteService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenGranterServiceImpl implements TokenGranterService {

    private final UtenteService utenteService;
    @Value("${custom.secret.key}")
    private String secretKey;
    @Value("${jwt-token.expirationTime}")
    private long tempoMax;

    private SecretKey getSignInKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    @Override
    public String getToken(AuthenticatedPrincipal a) {
        String emailUtente=a.getName();
        try {
            Utente userDetails = utenteService.findByEmailElis(emailUtente);
            return generateToken(creaClaims(userDetails));
        }catch (Exception e){
            System.out.println("Errore durante la generazione del token: "+e.getMessage());
            return null;
        }
    }

    private String generateToken(JwtBuilder.BuilderClaims bc) {
        return bc.and()
                .signWith(getSignInKey())
                .compact();
    }

    private JwtBuilder.BuilderClaims creaClaims(Utente u){
        JwtBuilder.BuilderClaims bc= Jwts.builder().claims();
        Set<Diritto> diritti = u.getDiritti();
        Map<String, List<String>> dirittiMap = diritti.stream()
                .collect(Collectors.groupingBy(d->d.getTipologiaCorso().getNome(),
                                Collectors.mapping(d->d.getRuolo().name(),Collectors.toList())
                        )
                );

        bc.add("diritti",dirittiMap);
        bc.add("nome",u.getNome());
        bc.add("cognome",u.getCognome());
        bc.subject(u.getEmailElis());
        bc.issuedAt(new Date(System.currentTimeMillis()));
        bc.expiration(new Date(System.currentTimeMillis() + tempoMax));
        return bc;
    }

    private Claims getClaims(String token) {
        return (Claims) Jwts.parser().verifyWith(getSignInKey()).build().parse(token).getPayload();
    }

    @Override
    public Utente getUtente(String token) {
        Claims c = getClaims(token);
        String user = c.getSubject();
        Utente u = utenteService.findByEmailElis(user);
        return u;
    }
}
