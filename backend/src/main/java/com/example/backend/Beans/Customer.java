package com.example.backend.Beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@DiscriminatorValue("CU")
public class Customer extends User {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        ArrayList<Role> retVal = new ArrayList<Role>();
        retVal.add(getRole());
        return retVal;
    }

    @Column
    private int points;

    @Column
    private int penaltyPoints;

    @ManyToMany(mappedBy = "prepaidCustomers")
    @JsonIgnore
    private List<Adventure> prepaidAdventures = new ArrayList<>();

    @Override
    public String getUsername() {
        return this.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.getEnabled();
    }

}
