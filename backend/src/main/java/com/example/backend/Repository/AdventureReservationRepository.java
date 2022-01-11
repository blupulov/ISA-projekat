package com.example.backend.Repository;

import com.example.backend.Beans.AdventureReservation;
import com.example.backend.Beans.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface AdventureReservationRepository extends JpaRepository<AdventureReservation, Long> {
    Collection<AdventureReservation> findByreservationStartBetween(LocalDateTime reservationStart, LocalDateTime reservationStart2);

    @Query("select ar from AdventureReservation ar where ar.customer.id = ?1 and ar.rated = false" +
            " and ar.reservationStart < ?2")
    Collection<AdventureReservation> getAllReservationOfCustomerForEvaluation(long customerId , LocalDateTime now);

    @Query("select ar from AdventureReservation ar where ar.customer.id = ?1" +
            " and ar.reservationStart > ?2")
    Collection<AdventureReservation> getAllReservationOfCustomerInFuture(long customerId , LocalDateTime now);

    @Query("select ar from AdventureReservation ar where ar.customer.id = ?1" +
            " and ar.reservationStart < ?2")
    Collection<AdventureReservation> getAllReservationOfCustomerInPast(long customerId , LocalDateTime now);

    @Query("select ar from AdventureReservation ar where ar.customer.id = ?1" +
            " and ar.reservationStart < ?2 and ar.complaint is null")
    Collection<AdventureReservation> getAllReservationOfCustomerInPastWithoutComplaint(long customerId , LocalDateTime now);

    @Query("select ar from AdventureReservation ar where" +
            " ar.adventure.instructor.id = ?1 order by ar.reservationStart asc ")
    Collection<AdventureReservation> getAllReservationsOfInstructorForCalendar(long instructorId);

    @Query("select ar from AdventureReservation ar where ar.complaint is not null and ar.complaint.reviewed = false")
    Iterable<? extends AdventureReservation> getAllNotReviewedAdventureComplaint();

    @Query("select ar from AdventureReservation ar where ar.customer is not null and ar.reservationStart between ?1 and ?2")
    Collection<AdventureReservation> getAllReservationsForCalculatingIncome(LocalDateTime startTime, LocalDateTime endTime);

    @Query("select ar from AdventureReservation ar where ar.customer is not null and ar.adventure.instructor.id = ?3 and ar.reservationStart between ?1 and ?2" +
            " order by ar.reservationStart asc ")
    Collection<AdventureReservation> getAllReservationsForGraph(LocalDateTime startTime, LocalDateTime endTime, long id);

}
