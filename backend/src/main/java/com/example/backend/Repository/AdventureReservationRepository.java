package com.example.backend.Repository;

import com.example.backend.Beans.AdventureReservation;
import com.example.backend.Beans.BoatReservation;
import com.example.backend.Beans.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ar from AdventureReservation ar where ar.id = ?1")
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "0")})
    AdventureReservation getAdventureReservationByReservationId(long id);
}
