package dev.antoniogrillo.testldap.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TipologiaCorso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nome;
    @Column(length = 1024)
    @Lob
    private String patternGrafico;

}
