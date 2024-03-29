package com.example.backend.Controllers;

import com.example.backend.Beans.AdventureReservation;
import com.example.backend.Beans.Cottage;
import com.example.backend.Beans.CottageReservation;
import com.example.backend.Dtos.CancelTermDto;
import com.example.backend.Dtos.CustomerReserveCottageDto;
import com.example.backend.Dtos.CustomerReserveTermDto;
import com.example.backend.Dtos.ReservationSearchDto;
import com.example.backend.Dtos.*;
import com.example.backend.Services.CottageReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@CrossOrigin(allowedHeaders = "*")
@RestController
@RequestMapping("cottageReservation")
public class CottageReservationController {
    @Autowired
    private final CottageReservationService cottageReservationService;

    public CottageReservationController(CottageReservationService cottageReservationService) {
        this.cottageReservationService = cottageReservationService;
    }

    @GetMapping
    public ResponseEntity<Collection<CottageReservation>> getAllCottageReservations(){
        return new ResponseEntity<>(cottageReservationService.getAllCottageReservations(), HttpStatus.OK);
    }

    @GetMapping(path = "/futureCustomerReservation/{customerId}")
    public ResponseEntity<Collection<CottageReservation>> getAllFutureTermsByCustomerId(
            @PathVariable long customerId){
        return new ResponseEntity<>(cottageReservationService.getAllFutureTermsByCustomerId(customerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/pastCustomerReservation/{customerId}")
    public ResponseEntity<Collection<CottageReservation>> getAllPastTermsByCustomerId(
            @PathVariable long customerId){
        return new ResponseEntity<>(cottageReservationService.getAllPastTermsByCustomerId(customerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/ownerReservation/{ownerId}")
    public ResponseEntity<Collection<CottageReservation>> getAllTermsByOwnerId(
            @PathVariable long ownerId){
        return new ResponseEntity<>(cottageReservationService.getAllTermsByOwnerId(ownerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/ownerFastReservation/{ownerId}")
    public ResponseEntity<Collection<CottageReservation>> getAllFastTermsByOwnerId(
            @PathVariable long ownerId){
        return new ResponseEntity<>(cottageReservationService.getAllFastTermsByOwnerId(ownerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/calendar/{ownerId}")
    public ResponseEntity<Collection<CalendarReservationsDto>>
    getAllReservationForCalendarOfOwner(@PathVariable long ownerId){
        return new ResponseEntity<>(cottageReservationService.getAllReservationsOfOwnerForCalendar(ownerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/calendarCottage/{cottageId}")
    public ResponseEntity<Collection<CalendarReservationsDto>>
    getAllReservationForCalendarOfCottage(@PathVariable long cottageId){
        return new ResponseEntity<>(cottageReservationService.getAllReservationsOfCottageForCalendar(cottageId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/reservation/{reservationId}")
    public ResponseEntity<CottageReservation> getReservationById(
            @PathVariable long reservationId){
        return new ResponseEntity<>(cottageReservationService.findCottageReservationById(reservationId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/pastCustomerReservationWithOutComplaint/{customerId}")
    public ResponseEntity<Collection<CottageReservation>> getAllPastTermsByCustomerIdWithoutComplaint(
            @PathVariable long customerId){
        return new ResponseEntity<>(cottageReservationService.getAllPastTermsWithoutComplaintByCustomerId(customerId),
                HttpStatus.OK);
    }

    @GetMapping(path = "/fastReservations/")
    public ResponseEntity<Collection<CottageReservation>> getFastCottageReservations(){
        return new ResponseEntity<>(cottageReservationService.getAllFastReservations(), HttpStatus.OK);
    }

    @GetMapping(path = "/nextActions/{id}")
    public ResponseEntity<Collection<CottageReservation>> getFastCottageReservations(@PathVariable long id){
        return new ResponseEntity<>(cottageReservationService.getAllNextActionsOfCottage(id), HttpStatus.OK);
    }

    @PostMapping(path = "/availableCottages/")
    public ResponseEntity<Collection<Cottage>> getAllAvailableCottages(@RequestBody ReservationSearchDto search){
        return new ResponseEntity<>(cottageReservationService.getAllAvailableCottagesForSearch(search),
                HttpStatus.OK);
    }

    @PutMapping(path = "/reserveTerm/")
    public ResponseEntity<CottageReservation> ReserveTerm(@RequestBody CustomerReserveCottageDto reservation) throws InterruptedException {
        CottageReservation cottageReservation = cottageReservationService.makeNewAppointment(reservation);
        if (cottageReservation == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        else if (cottageReservation.getCustomer() == null)
            return new ResponseEntity<>(null, HttpStatus.OK);
        return new ResponseEntity<>(cottageReservation, HttpStatus.OK);
    }

    @PutMapping(path = "/cancelTerm/")
    public ResponseEntity<CottageReservation> CancelTerm(@RequestBody CancelTermDto cancel){
        CottageReservation cottageReservation = cottageReservationService.cancelTerm(cancel);
        if (cottageReservation == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(cottageReservation, HttpStatus.OK);
    }


    @PutMapping(path = "/fastReservationPeriod/")
    public ResponseEntity<CottageReservation> FastReservationPeriod(@RequestBody FastReservationDto reservation) throws InterruptedException {
        CottageReservation cottageReservation = cottageReservationService.makeFastReservationSlot(reservation);
        if (cottageReservation == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(cottageReservation, HttpStatus.OK);
    }

    @PutMapping(path = "/reserveFastReservation/")
    public ResponseEntity<CottageReservation> ReserveFastReservation(@RequestBody ReservingFastReservationDto reservation) throws InterruptedException {
        CottageReservation cottageReservation = cottageReservationService.reserveFastReservation(reservation.getUserId(), reservation.getReservationId());
        if (cottageReservation == null)
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(cottageReservation, HttpStatus.OK);
    }
}
