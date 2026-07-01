package dev.antoniogrillo.testldap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name="diritto", uniqueConstraints = @UniqueConstraint(name = "diritti_univoci", columnNames = {"utente_id", "tipologia_id","ruolo"}))
public class Diritto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name="utente_id",nullable = false)
    private Utente utente;
    @ManyToOne
    @JoinColumn(name="tipologia_id",nullable = false)
    private TipologiaCorso tipologiaCorso;
    @Column(nullable = false)
    private Ruolo ruolo;

}
