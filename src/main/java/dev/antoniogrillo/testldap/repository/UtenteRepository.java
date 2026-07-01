package dev.antoniogrillo.testldap.repository;

import dev.antoniogrillo.testldap.model.Utente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UtenteRepository extends JpaRepository<Utente, Long> {
    @EntityGraph(attributePaths = {"diritti", "diritti.tipologiaCorso"})
    Optional<Utente> findByEmailElis(String emailUtente);

    @EntityGraph(attributePaths = {"diritti", "diritti.tipologiaCorso"})
    Optional<Utente> findByUsernameLdap(String usernameLdap);
}