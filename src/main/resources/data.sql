insert into tipologia_corso(nome, pattern_grafico) values ('ingegneria', 'pattern1');
insert into anno_iscrizione(anno, tipologia_corso_id, chiuso) values ('2025', 1, false);
insert into utente(nome,cognome,codice_fiscale,email_elis,username_ldap,anno_iscrizione_id) values ('User','visualizzatore','STUDENTE001','visualizzatore@elis.org','ingegneria-test-1',1);

INSERT INTO diritto (utente_id, tipologia_id, ruolo) VALUES
    (1,1, 0);