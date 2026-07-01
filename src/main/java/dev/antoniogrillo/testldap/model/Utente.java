package dev.antoniogrillo.testldap.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "utente")
public class Utente implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(updatable = false)
    private String nome;
    @Column(updatable = false)
    private String cognome;
    @Column(updatable = false, unique = true)
    private String codiceFiscale;
    @Column(unique = true)
    private String emailElis;
    private String usernameLdap;


    @OneToMany(mappedBy = "utente")
    private Set<Diritto> diritti;
    @ManyToOne
    @JoinColumn(name="anno_iscrizione_id")
    private AnnoIscrizione annoIscrizione;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return diritti==null?new ArrayList<>():
                diritti.stream().map(d->new SimpleGrantedAuthority("ROLE_"+d.getTipologiaCorso().getNome()+"_"+d.getRuolo().name())).toList();
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.emailElis;
    }
}
