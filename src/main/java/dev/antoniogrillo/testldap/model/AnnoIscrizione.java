package dev.antoniogrillo.testldap.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "anno_iscrizione")

public class AnnoIscrizione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String anno;
    @ManyToOne
    @JoinColumn(name="tipologia_corso_id")
    private TipologiaCorso tipologiaCorso;
    @Column(nullable = false)
    private Boolean chiuso;


}