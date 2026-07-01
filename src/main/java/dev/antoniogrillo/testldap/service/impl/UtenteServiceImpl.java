package dev.antoniogrillo.testldap.service.impl;


import dev.antoniogrillo.testldap.model.Utente;
import dev.antoniogrillo.testldap.repository.UtenteRepository;
import dev.antoniogrillo.testldap.service.def.UtenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UtenteServiceImpl implements UtenteService{

    private final UtenteRepository repo;

    @Override
    public Utente getbyId(long idUtente)
    {
        return repo.findById(idUtente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessun utente con id " + idUtente));
    }

    @Override
    public Utente findByEmailElis(String emailUtente)
    {
        return repo.findByEmailElis(emailUtente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nessun utente trovato per l'email: " + emailUtente));
    }
}