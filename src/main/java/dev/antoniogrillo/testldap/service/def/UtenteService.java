package dev.antoniogrillo.testldap.service.def;
import dev.antoniogrillo.testldap.model.Utente;
import org.springframework.validation.annotation.Validated;

@Validated
public interface UtenteService {

    Utente getbyId(long idUtente);
    Utente findByEmailElis(String emailUtente);
}