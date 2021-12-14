package com.example.backend.Beans;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@Table(name = "AdventureReservations")
public class AdventureReservation {
    @Id
    @SequenceGenerator(name = "myGen_adventureReservation", sequenceName = "mySeq_adventureReservation",
            initialValue = 1, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myGen_adventureReservation")
    @Column(name = "adventure_reservation_id", unique = true, nullable = false)
    private long id;

    @Column(nullable = false)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm")
    private LocalDateTime lastDateToReserve;

    @Column(nullable = false)
    @JsonFormat(pattern="yyyy-MM-dd hh:mm")
    private LocalDateTime reservationStart;

    @Column(nullable = false)
    private int length;

    @Column(nullable = false)
    private int price;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "adventure_id")
    private Adventure adventure;

    @Column(length = 2000)
    private String report;

    @Column
    private boolean isReserved;
}
