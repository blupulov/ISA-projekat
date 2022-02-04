package com.example.backend.Services;

import com.example.backend.Beans.*;
import com.example.backend.Dtos.*;
import com.example.backend.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class BoatReservationService {
    @Autowired
    private final BoatReservationRepository boatReservationRepository;


    @Autowired
    private final ForbiddenCustomerToBoatRepository forbiddenCustomerToBoatRepository;

    @Autowired
    private BoatService boatService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AdditionalServiceService additionalServiceService;

    @Autowired
    private AdditionalBoatServiceService additionalBoatServiceService;

    @Autowired
    private CottageReservationService cottageReservationService;

    @Autowired
    private AdventureReservationService adventureReservationService;

    public BoatReservationService(BoatReservationRepository repository, ForbiddenCustomerToBoatRepository forbiddenCustomerToBoatRepository){
        this.boatReservationRepository = repository;
        this.forbiddenCustomerToBoatRepository = forbiddenCustomerToBoatRepository;
    }

    public BoatReservation getCurrentReservationOfInstructor(long boatId){
        Boat boat = this.boatService.findById(boatId);
        for(BoatReservation reservation : boat.getReservations()) {
            if (reservation.getReservationStart().isBefore(LocalDateTime.now()) && reservation.getReservationEnd().isAfter(LocalDateTime.now()))
                return reservation;
        }
        return null;
    }

    public BoatReservation getBoatReservationById(long id){
        return findBoatReservationById(id);
    }

    public Collection<BoatReservation> getAllBoatReservations(){
        return boatReservationRepository.findAll();
    }

    public BoatReservation findBoatReservationById(long id){
        return boatReservationRepository.findById(id).orElse(null);
    }

    @Transactional
    public Collection<Boat> getAllAvailableBoatsForSearch(ReservationSearchDto search){
        List<Boat> boats = new ArrayList<>();
        for (Boat boat : boatService.findAllBoats()){
            if(!boat.isCaptain() && search.isCaptain()) {
                continue;
            }

                if (isSearchInBoatAvailablePeriod(search.getDateFrom(), search.getDateTo(), boat) &&
                        search.getPersons() < boat.getCapacity() && isUserNotForbidden(search, boat)
                        && IsInCountry(boat, search.getCountry())
                        && IsInLocation(boat, search.getCity())) {
                    boolean valid = true;
                    for (BoatReservation br : boat.getReservations()) {
                        if (isReservationsOverlapForSearch(br, search.getDateFrom(), search.getDateTo())) {
                            valid = false;
                            break;
                        }
                    }

                    try {
                        if (search.isCaptain()) {
                            for (BoatReservation br : getAllTermsByCaptainId(boat.getBoatOwner().getId())) {
                                if (isReservationsOverlapForSearch(br, search.getDateFrom(), search.getDateTo())) {
                                    valid = false;
                                    break;
                                }
                            }

                        }
                    }catch (Exception e){
                        return null;
                    }
                    if (valid)
                        boats.add(boat);
                }

        }
        return boats;
    }

    @Transactional
    public BoatReservation makeNewAppointment(CustomerReserveCottageDto reservation) {
        BoatReservation retVal = null;
        try {
            Customer customer = customerService.findCustomerById(reservation.getUserId());

            BoatReservation boatReservation = new BoatReservation();
            boatReservation.setReservationStart(reservation.getFrom());
            boatReservation.setReservationEnd(reservation.getTo());
            boatReservation.setCustomer(customer);
            boatReservation.setBoat(boatService.findById(reservation.getCottageId()));
            boatReservation.setFast(false);
            try {
                if (IsCustomersReservationsOverlapsWithNew(customer, boatReservation)) {
                    return null;
                }
            }catch (Exception e){
                return new BoatReservation();
            }

            boatReservation.setDiscount(0);
            boatReservation.setBoatPriceList(boatReservation.getBoat().getPriceList()); //DA LI MENI OVO TREBA??
            List<AdditionalBoatService> services = findAllSelectedAdditionalServices(reservation.getServices());

            calculateFullPriceOfReservation(boatReservation, services);
            boatReservation.setLength((int) ChronoUnit.DAYS.between(reservation.getFrom(), reservation.getTo()));

            retVal = save(boatReservation);
            emailService.sendBoatReservationConfirm(customer, boatReservation);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    @Transactional
    public BoatReservation save(BoatReservation boatReservation){
        return this.boatReservationRepository.save(boatReservation);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<BoatReservation> getAllFutureCottageReservationForBoat(long id){
        return this.boatReservationRepository.getAllFutureBoatReservationOfBoat(id, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<BoatReservation> getAllFutureTermsByCustomerId(long id){
        return boatReservationRepository.getAllReservationOfCustomerInFuture(id,
                LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<BoatReservation> getAllTermsByCaptainId(long id){
        return boatReservationRepository.getAllReservationOfCaptain(id);
    }

    public Collection<BoatReservation> getAllPastTermsByCustomerId(long id){
        return boatReservationRepository.getAllReservationOfCustomerInPast(id, LocalDateTime.now());
    }

    public Collection<BoatReservation> getAllTermsByOwnerId(long id){
        return boatReservationRepository.getAllReservationOfOwner(id);
    }

    public Collection<BoatReservation> getAllFastTermsByOwnerId(long id){
        return boatReservationRepository.getAllFastReservationOfOwner(id);
    }

    public Collection<BoatReservation> getAllPastTermsWithoutComplaintByCustomerId(long id){
        return boatReservationRepository.getAllReservationOfCustomerInPastWithoutComplaint(id,
                LocalDateTime.now());
    }

    @Transactional
    public BoatReservation cancelTerm(CancelTermDto data){
        try {
            Customer customer = this.customerService.findCustomerById(data.getUserId());
            BoatReservation reservation = findBoatReservationById(data.getReservationId());
            if(customer.getId() != reservation.getCustomer().getId())
                return null;

            ForbiddenCustomerToBoat forbidden = new ForbiddenCustomerToBoat();
            forbidden.setBoat(reservation.getBoat());
            forbidden.setCustomer(customer);
            forbidden.setReservationStart(reservation.getReservationStart());
            forbidden.setReservationEnd(reservation.getReservationEnd());

            forbiddenCustomerToBoatRepository.save(forbidden);
            if (!reservation.isFast())
                reservation.setBoat(null);
            reservation.setCustomer(null);
            return save(reservation);
        }catch (Exception e){
            return null;
        }
    }

    @Transactional
    public BoatReservation markReservationAsReported(long id){
        BoatReservation reservation = findBoatReservationById(id);
        reservation.setReported(true);
        return save(reservation);
    }

    public Collection<CalendarReservationsDto> getAllReservationsOfOwnerForCalendar(long ownerId){
        List<CalendarReservationsDto> reservations = new ArrayList<>();
        for(BoatReservation br :
                boatReservationRepository.getAllReservationsOfOwnerForCalendar(ownerId)){
            reservations.add(new CalendarReservationsDto(br));
        }
        return reservations;
    }

    public Collection<CalendarReservationsDto> getAllReservationsOfBoatForCalendar(long boatId){
        List<CalendarReservationsDto> reservations = new ArrayList<>();
        for(BoatReservation br :
                boatReservationRepository.getAllReservationsOfBoatForCalendar(boatId)){
            reservations.add(new CalendarReservationsDto(br));
        }
        return reservations;
    }

    @Transactional
    public BoatReservation makeFastReservationSlot(FastReservationDto reservation) throws InterruptedException {

        try {

            BoatReservation boatReservation = new BoatReservation();
            boatReservation.setReservationStart(reservation.getDate1());
            boatReservation.setReservationEnd(reservation.getDate2());
            boatReservation.setBoat(boatService.findById(reservation.getCottageId()));
            boatReservation.setDiscount(reservation.getSale());
            boatReservation.setBoatPriceList(boatReservation.getBoat().getPriceList());
            boatReservation.setFast(true);

            calculateFullPriceOfReservation(boatReservation, boatReservation.getBoatPriceList().getAdditionalServices());
            boatReservation.setLength((int) ChronoUnit.DAYS.between(reservation.getDate1(), reservation.getDate2()));
            try {
                if (isFastReservationPeriodValid(boatReservation)) {
                    for (Customer cr : boatService.findById(reservation.getCottageId()).getPrepaidCustomers()) {
                        emailService.sendNotificationForCreatingFastReservationBoat(cr, reservation, boatReservation.getBoat().getName());
                    }
                    return save(boatReservation);
                }
            } catch (Exception e) {
            return null;
        }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public Collection<BoatReservation> getAllFastReservations(){
        List<BoatReservation> fastReservations = new ArrayList<BoatReservation>();
        for (BoatReservation reservation: getAllBoatReservations()) {
            if(reservation.isFast() && reservation.getCustomer() == null){
                fastReservations.add(reservation);
            }
        }
        return fastReservations;
    }

    @Transactional
    public BoatReservation reserveFastReservation(long usedId, long reservationId) {
        BoatReservation retVal = null;
        try {
            BoatReservation boatReservation = findBoatReservationById(reservationId);
            if(boatReservation.getCustomer() != null)
                return null;
            Customer customer = customerService.findCustomerById(usedId);
            if (IsCustomersReservationsOverlapsWithNew(customer, boatReservation)) {
                return null;
            }
            ReservationSearchDto pom = new ReservationSearchDto();
            pom.setDateFrom(boatReservation.getReservationStart());
            pom.setDateTo(boatReservation.getReservationEnd());
            pom.setId(customer.getId());
            if (!isUserNotForbidden(pom, boatReservation.getBoat())) {
                return null;
            }

            boatReservation.setCustomer(customer);
            retVal = save(boatReservation);
            emailService.sendBoatReservationConfirm(customer, boatReservation);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    private boolean IsInLocation(Boat boat, String city) {
        if (city == null)
            return true;
        return boat.getAddress().getCity().toUpperCase(Locale.ROOT).contains(city.toUpperCase(Locale.ROOT));
    }

    private boolean IsInCountry(Boat boat, String country) {
        if (country == null)
            return true;
        return boat.getAddress().getCountry().toUpperCase(Locale.ROOT).contains(country.toUpperCase(Locale.ROOT));
    }

    private boolean isReservationsOverlapForSearch(BoatReservation existingReservation, LocalDateTime start, LocalDateTime end){
        if(start.isAfter(existingReservation.getReservationStart()) &&
                start.isBefore(existingReservation.getReservationEnd()))
            return true;
        else if(end.isAfter(existingReservation.getReservationStart()) &&
                end.isBefore(existingReservation.getReservationEnd()))
            return true;
        else if(start.isEqual(existingReservation.getReservationStart()))
            return true;
        else return start.isBefore(existingReservation.getReservationStart()) &&
                    end.isAfter(existingReservation.getReservationEnd());
    }



    private boolean isSearchInBoatAvailablePeriod(LocalDateTime start, LocalDateTime end, Boat boat){
        for(AvailablePeriodBoat periodBoat: boat.getPeriods()){
            if (start.isAfter(periodBoat.getFromDate()) && end.isBefore(periodBoat.getToDate())) {

                return true;
            }
        }
        return false;
    }

    private boolean IsCustomersReservationsOverlapsWithNew(Customer customer, BoatReservation newReservation){
        for (AdventureReservation ar : adventureReservationService.getAllFutureTermsByCustomerId(customer.getId())){
            if (isReservationsOverlapWithAdventureReservations(ar, newReservation))
                return true;
        }

        for (CottageReservation cr : cottageReservationService.getAllFutureTermsByCustomerId(customer.getId())){
            if (isReservationsOverlapWithCottageReservations(cr, newReservation)){
                return true;
            }
        }

        for (BoatReservation br: getAllFutureTermsByCustomerId(customer.getId())){
            if (isReservationsOverlapWithBoatReservations(br, newReservation)){
                return true;
            }
        }
        if(newReservation.isFast())
            return false;

        for (BoatReservation br: getAllFutureCottageReservationForBoat(newReservation.getBoat().getId())){
            if (isReservationsOverlapWithBoatReservations(br, newReservation)){
                return true;
            }
        }

        return false;
    }

    private boolean isFastReservationPeriodValid(BoatReservation reservation){
        if(isSearchInBoatAvailablePeriod(reservation.getReservationStart(), reservation.getReservationEnd(), reservation.getBoat())){
            for (BoatReservation cr : reservation.getBoat().getReservations()){
                if (isReservationsOverlapWithBoatReservations(cr, reservation)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isReservationsOverlapWithAdventureReservations(
            AdventureReservation existingReservation, BoatReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationStart().plusHours(existingReservation.getLength())
                .plusMinutes(existingReservation.getLengthMin());
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationEnd();
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapWithCottageReservations(
            CottageReservation existingReservation, BoatReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationEnd();
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationEnd();
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapWithBoatReservations(
            BoatReservation existingReservation, BoatReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationEnd();
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationEnd();
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isToRangesOverlaps(LocalDateTime existingStart, LocalDateTime existingEnd,
                                       LocalDateTime newStart, LocalDateTime newEnd){
        if(existingStart.isAfter(newStart) && existingStart.isBefore(newEnd))
            return true;
        else if (existingEnd.isAfter(newStart) && existingEnd.isBefore(newEnd))
            return true;
        else if (newStart.isAfter(existingStart) && newStart.isBefore(existingEnd))
            return true;
        else if (newStart.isEqual(existingStart) && newEnd.isEqual(existingEnd))
            return true;
        else return newEnd.isAfter(existingStart) && newEnd.isBefore(existingEnd);
    }

    private List<AdditionalBoatService> findAllSelectedAdditionalServices(List<Long> ids){
        List<AdditionalBoatService> services = new ArrayList<>();
        for(long id : ids)
            services.add(this.additionalBoatServiceService.findAdditionalServiceById(id));
        return services;
    }

    private void calculateFullPriceOfReservation(
            BoatReservation reservation, List<AdditionalBoatService> addServices){
        int price = reservation.getBoatPriceList().getPrice();
        for(AdditionalBoatService as : addServices){
            price += as.getAddPrice();
            reservation.addAdditionalService(as);
        }
        long days = ChronoUnit.DAYS.between(reservation.getReservationStart(), reservation.getReservationEnd());
        price = price*(int)days;
        reservation.setPrice((int) (price * (1 - reservation.getDiscount()/100)));
    }

    private void calculateFullPriceOfReservation(
            BoatReservation reservation, Set<AdditionalBoatService> addServices){
        int price = reservation.getBoatPriceList().getPrice();
        for(AdditionalBoatService as : addServices){
            price += as.getAddPrice();
            reservation.addAdditionalService(as);
        }
        long days = ChronoUnit.DAYS.between(reservation.getReservationStart(), reservation.getReservationEnd());
        price = price*(int)days;
        reservation.setPrice((int) (price * (1 - reservation.getDiscount()/100)));
    }


    private boolean isUserNotForbidden(ReservationSearchDto searchDto, Boat boat){
        for (ForbiddenCustomerToBoat fc : forbiddenCustomerToBoatRepository.getAllCancellationsOfCustomerToBoat(searchDto.getId(), boat.getId())){
            if (isToRangesOverlaps(fc.getReservationStart(), fc.getReservationEnd(), searchDto.getDateFrom(), searchDto.getDateTo())){
                return false;
            }
        }
        return true;
    }

    @Transactional
    public BoatReservation markReservationAsEvaluated(long reservationId){
        BoatReservation boatReservation = findBoatReservationById(reservationId);
        boatReservation.setRated(true);
        return save(boatReservation);
    }

    public Collection<BoatReservation> getAllNextActionsOfBoat(long id) {
        return boatReservationRepository.getAllFreeFutureActionsOfBoat(id, LocalDateTime.now());
    }
}
