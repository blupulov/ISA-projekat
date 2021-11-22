package com.example.backend.Beans;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Address {
    @Id
    @SequenceGenerator(name = "generator", sequenceName = "sequenceAddress")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "generator")
    @Column(name="address_id")
    private long id;
    @Column(length = 100)
    private String street;
    @Column(length = 100)
    private String city;
    @Column(length = 100)
    private String country;
}
