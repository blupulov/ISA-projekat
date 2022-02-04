package com.example.backend.Services;

import com.example.backend.Beans.*;
import com.example.backend.Dtos.*;
import com.example.backend.Repository.CottageRepository;
import com.example.backend.Repository.CottageReservationRepository;
import com.example.backend.Repository.ForbiddenCustomerToCottageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CottageReservationService {

    @Autowired
    private final CottageReservationRepository cottageReservationRepository;

    @Autowired
    private final CottageRepository cottageRepository;

    @Autowired
    private final ForbiddenCustomerToCottageRepository forbiddenCustomerToCottageRepository;

    @Autowired
    private CottageService cottageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AdditionalServiceService additionalServiceService;

    @Autowired
    private AdditionalCottageServiceService additionalCottageServiceService;

    @Autowired
    private BoatReservationService boatReservationService;

    @Autowired
    private AdventureReservationService adventureReservationService;


    public CottageReservationService(CottageReservationRepository repository, CottageRepository cottageRepository, ForbiddenCustomerToCottageRepository forbiddenCustomerToCottageRepository){
        this.cottageReservationRepository = repository;
        this.cottageRepository = cottageRepository;
        this.forbiddenCustomerToCottageRepository = forbiddenCustomerToCottageRepository;
    }



    public Collection<CottageReservation> getAll(){
        return cottageReservationRepository.findAll();
    }

    public CottageReservation getCurrentReservationOfInstructor(long cottageId){
        Cottage cottage = this.cottageService.findById(cottageId);
        for(CottageReservation reservation : cottage.getReservations()) {
            if (reservation.getReservationStart().isBefore(LocalDateTime.now()) && reservation.getReservationEnd().isAfter(LocalDateTime.now()))
                return reservation;
        }
        return null;
    }

    public CottageReservation getCottageReservationById(long id){
        return findCottageReservationById(id);
    }

    public Collection<CottageReservation> getAllCottageReservations(){
        return cottageReservationRepository.findAll();
    }

    public CottageReservation findCottageReservationById(long id){
        return cottageReservationRepository.findById(id).orElse(null);
    }

    public Collection<Cottage> getAllAvailableCottagesForSearch(ReservationSearchDto search){
        List<Cottage> cottages = new ArrayList<>();
        for (Cottage cottage : cottageService.findAllCottages()){
            if (isSearchInCottageAvailablePeriod(search.getDateFrom(), search.getDateTo(), cottage) &&
                    isCottageHaveEnoughBeds(search.getPersons(), cottage) && isUserNotForbidden(search, cottage)
                    && IsInLocation(cottage, search.getCity())
                    && IsInCountry(cottage, search.getCountry())){
                boolean valid = true;
                for (CottageReservation cr: cottage.getReservations()){
                     if(isReservationsOverlapForSearch(cr, search.getDateFrom(), search.getDateTo())) {
                         valid = false;
                         break;
                     }
                }
                if (valid)
                    cottages.add(cottage);
            }
        }
        return cottages;
    }

    @Transactional
    public CottageReservation makeNewAppointment(CustomerReserveCottageDto reservation) {
        CottageReservation retVal = null;
        try {
            Customer customer = customerService.findCustomerById(reservation.getUserId());

            CottageReservation cottageReservation = new CottageReservation();
            cottageReservation.setReservationStart(reservation.getFrom());
            cottageReservation.setReservationEnd(reservation.getTo());
            cottageReservation.setCustomer(customer);
            cottageReservation.setCottage(cottageService.findById(reservation.getCottageId()));
            cottageReservation.setFast(false);
            try {
                if (IsCustomersReservationsOverlapsWithNew(customer, cottageReservation)) {
                    return null;
                }
            }catch (Exception e){
                return new CottageReservation();
            }

            cottageReservation.setDiscount(0);
            cottageReservation.setCottagePriceList(cottageReservation.getCottage().getCottagePriceList()); //DA LI MENI OVO TREBA??
            List<AdditionalCottageService> services = findAllSelectedAdditionalServices(reservation.getServices());

            calculateFullPriceOfReservation(cottageReservation, services);
            cottageReservation.setLength((int) ChronoUnit.DAYS.between(reservation.getFrom(), reservation.getTo()));

            retVal = save(cottageReservation);
            emailService.sendCottageReservationConfirm(customer, cottageReservation);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CottageReservation save(CottageReservation cottageReservation){
        return this.cottageReservationRepository.save(cottageReservation);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<CottageReservation> getAllFutureCottageReservationForCottage(long id){
        return this.cottageReservationRepository.getAllFutureCottageReservationOfCottage(id, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<CottageReservation> getAllFutureTermsByCustomerId(long id){
        return cottageReservationRepository.getAllReservationOfCustomerInFuture(id,
                LocalDateTime.now());
    }

    public Collection<CottageReservation> getAllTermsByOwnerId(long id){
        return cottageReservationRepository.getAllReservationOfOwner(id);
    }

    public Collection<CottageReservation> getAllFastTermsByOwnerId(long id){
        return cottageReservationRepository.getAllFastReservationOfOwner(id);
    }

    public Collection<CottageReservation> getAllPastTermsByCustomerId(long id){
        return cottageReservationRepository.getAllReservationOfCustomerInPast(id, LocalDateTime.now());
    }

    public Collection<CottageReservation> getAllPastTermsWithoutComplaintByCustomerId(long id){
        return cottageReservationRepository.getAllReservationOfCustomerInPastWithoutComplaint(id,
                LocalDateTime.now());
    }

    public Collection<CalendarReservationsDto> getAllReservationsOfOwnerForCalendar(long ownerId){
        List<CalendarReservationsDto> reservations = new ArrayList<>();
        for(CottageReservation cr :
                cottageReservationRepository.getAllReservationsOfOwnerForCalendar(ownerId)){
            reservations.add(new CalendarReservationsDto(cr));
        }
        return reservations;
    }

    public Collection<CalendarReservationsDto> getAllReservationsOfCottageForCalendar(long cottageId){
        List<CalendarReservationsDto> reservations = new ArrayList<>();
        for(CottageReservation cr :
                cottageReservationRepository.getAllReservationOfCottage(cottageId)){
            reservations.add(new CalendarReservationsDto(cr));
        }
        return reservations;
    }

    @Transactional
    public CottageReservation cancelTerm(CancelTermDto data){
        try {
            Customer customer = this.customerService.findCustomerById(data.getUserId());
            CottageReservation reservation = findCottageReservationById(data.getReservationId());
            if(customer.getId() != reservation.getCustomer().getId())
                return null;

            ForbiddenCustomerToCottage forbidden = new ForbiddenCustomerToCottage();
            forbidden.setCottage(reservation.getCottage());
            forbidden.setCustomer(customer);
            forbidden.setReservationStart(reservation.getReservationStart());
            forbidden.setReservationEnd(reservation.getReservationEnd());

            forbiddenCustomerToCottageRepository.save(forbidden);
            if (!reservation.isFast())
                reservation.setCottage(null);
            reservation.setCustomer(null);
            return save(reservation);
        }catch (Exception e){
            return null;
        }
    }

    @Transactional
    public CottageReservation makeFastReservationSlot(FastReservationDto reservation) throws InterruptedException {
        try {
        CottageReservation cottageReservation = new CottageReservation();
        cottageReservation.setReservationStart(reservation.getDate1());
        cottageReservation.setReservationEnd(reservation.getDate2());
        cottageReservation.setCottage(cottageService.findById(reservation.getCottageId()));
        cottageReservation.setDiscount(reservation.getSale());
        cottageReservation.setCottagePriceList(cottageReservation.getCottage().getCottagePriceList());
        cottageReservation.setFast(true);

        calculateFullPriceOfReservation(cottageReservation, cottageReservation.getCottagePriceList().getAdditionalServices());
        cottageReservation.setLength((int) ChronoUnit.DAYS.between(reservation.getDate1(), reservation.getDate2()));
        try {
            if (isFastReservationPeriodValid(cottageReservation)) {
                for (Customer cr : cottageService.findById(reservation.getCottageId()).getPrepaidCustomers()) {
                    emailService.sendNotificationForCreatingFastReservation(cr, reservation, cottageReservation.getCottage().getName());
                }
                return save(cottageReservation);
            }
        } catch (Exception e) {
            return null;
        }

        }catch (Exception e){
            return null;
        }

        return null;
    }

    public Collection<CottageReservation> getAllFastReservations(){
        List<CottageReservation> fastReservations = new ArrayList<CottageReservation>();
        for (CottageReservation reservation: getAllCottageReservations()) {
            if(reservation.isFast() && reservation.getCustomer() == null){
                fastReservations.add(reservation);
            }
        }
        return fastReservations;
    }

    @Transactional
    public CottageReservation reserveFastReservation(long usedId, long reservationId) {
        CottageReservation retVal = null;
        try {
            CottageReservation cottageReservation = findCottageReservationById(reservationId);
            if(cottageReservation.getCustomer() != null)
                return null;
            Customer customer = customerService.findCustomerById(usedId);
            if (IsCustomersReservationsOverlapsWithNew(customer, cottageReservation)) {
                return null;
            }
            ReservationSearchDto pom = new ReservationSearchDto();
            pom.setDateFrom(cottageReservation.getReservationStart());
            pom.setDateTo(cottageReservation.getReservationEnd());
            pom.setId(customer.getId());
            if (!isUserNotForbidden(pom, cottageReservation.getCottage())) {
                return null;
            }

            cottageReservation.setCustomer(customer);
            retVal = save(cottageReservation);
            emailService.sendCottageReservationConfirm(customer, cottageReservation);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    private boolean IsInLocation(Cottage cottage, String city) {
        if (city == null)
            return true;
        return cottage.getAddress().getCity().toUpperCase(Locale.ROOT).contains(city.toUpperCase(Locale.ROOT));
    }

    private boolean IsInCountry(Cottage cottage, String country) {
        if (country == null)
            return true;
        return cottage.getAddress().getCountry().toUpperCase(Locale.ROOT).contains(country.toUpperCase(Locale.ROOT));
    }

    private boolean isReservationsOverlapForSearch(CottageReservation existingReservation, LocalDateTime start, LocalDateTime end){
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

    private boolean isSearchInCottageAvailablePeriod(LocalDateTime start, LocalDateTime end, Cottage cottage){
         for(AvailablePeriodCottage periodCottage: cottage.getPeriods()){
             if (start.isAfter(periodCottage.getFromDate()) && end.isBefore(periodCottage.getToDate())) {

                 return true;
             }
         }
         return false;
    }

    private boolean isCottageHaveEnoughBeds(int numberOfBeds, Cottage cottage){
        int beds = 0;
        for (Room room : cottage.getRooms())
            beds += room.getNumberOfBeds();
        return beds >= numberOfBeds;
    }

    private boolean IsCustomersReservationsOverlapsWithNew(Customer customer, CottageReservation newReservation){
        for (AdventureReservation ar : adventureReservationService.getAllFutureTermsByCustomerId(customer.getId())){
            if (isReservationsOverlapWithAdventureReservations(ar, newReservation))
                return true;
        }

        for (CottageReservation cr : getAllFutureTermsByCustomerId(customer.getId())){
            if (isReservationsOverlapWithCottageReservations(cr, newReservation)){
                return true;
            }
        }

        for (BoatReservation br: boatReservationService.getAllFutureCottageReservationForBoat(customer.getId())){
            if (isReservationsOverlapWithBoatReservations(br, newReservation)){
                return true;
            }
        }

        if (newReservation.isFast()) //Ova provera je napravljena kod pravljenja brze rezervacije
            return false;
        //OVDE SE DOGADJA ZAKLJUCAVANJE
        for (CottageReservation cr: getAllFutureCottageReservationForCottage(newReservation.getCottage().getId())){
            if (isReservationsOverlapWithCottageReservations(cr, newReservation)){
                return true;
            }
        }
        return false;
    }

    private boolean isFastReservationPeriodValid(CottageReservation reservation){
        if(isSearchInCottageAvailablePeriod(reservation.getReservationStart(), reservation.getReservationEnd(), reservation.getCottage())){
            for (CottageReservation cr : getAllFutureCottageReservationForCottage(reservation.getCottage().getId())){
                if (isReservationsOverlapWithCottageReservations(cr, reservation)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isReservationsOverlapWithAdventureReservations(
            AdventureReservation existingReservation, CottageReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationStart().plusHours(existingReservation.getLength())
                .plusMinutes(existingReservation.getLengthMin());
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationEnd();
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapWithCottageReservations(
            CottageReservation existingReservation, CottageReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationEnd();
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationEnd();
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapWithBoatReservations(
            BoatReservation existingReservation, CottageReservation newReservation){
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

    private List<AdditionalCottageService> findAllSelectedAdditionalServices(List<Long> ids){
        List<AdditionalCottageService> services = new ArrayList<>();
        for(long id : ids)
            services.add(this.additionalCottageServiceService.findAdditionalServiceById(id));
        return services;
    }

    private void calculateFullPriceOfReservation(
            CottageReservation reservation, List<AdditionalCottageService> addServices){
        int price = reservation.getCottagePriceList().getPrice();
        for(AdditionalCottageService as : addServices){
            price += as.getAddPrice();
            reservation.addAdditionalService(as);
        }
        long days = ChronoUnit.DAYS.between(reservation.getReservationStart(), reservation.getReservationEnd());
        price = price*(int)days;
        reservation.setPrice((int) (price * (1 - reservation.getDiscount()/100)));
    }

    private void calculateFullPriceOfReservation(
            CottageReservation reservation, Set<AdditionalCottageService> addServices){
        int price = reservation.getCottagePriceList().getPrice();
        for(AdditionalCottageService as : addServices){
            price += as.getAddPrice();
            reservation.addAdditionalService(as);
        }
        long days = ChronoUnit.DAYS.between(reservation.getReservationStart(), reservation.getReservationEnd());
        price = price*(int)days;
        reservation.setPrice((int) (price * (1 - reservation.getDiscount()/100)));
    }


    private boolean isUserNotForbidden(ReservationSearchDto searchDto, Cottage cottage){
        Customer customer = customerService.findCustomerById(searchDto.getId());
        for (ForbiddenCustomerToCottage fc : forbiddenCustomerToCottageRepository.getAllCancellationsOfCustomerToCottage(searchDto.getId(), cottage.getId())){
            if (isToRangesOverlaps(fc.getReservationStart(), fc.getReservationEnd(), searchDto.getDateFrom(), searchDto.getDateTo())){
                return false;
            }
        }
        return true;
    }

    @Transactional
    public CottageReservation markReservationAsReported(long id){
        CottageReservation reservation = findCottageReservationById(id);
        reservation.setReported(true);
        return save(reservation);
    }

    @Transactional
    public CottageReservation markReservationAsEvaluated(long reservationId){
        CottageReservation cottageReservation = findCottageReservationById(reservationId);
        cottageReservation.setRated(true);
        return save(cottageReservation);
    }

    public Collection<CottageReservation> getAllNextActionsOfCottage(long id) {
        return cottageReservationRepository.getAllFreeFutureActionsOfCottage(id, LocalDateTime.now());
    }
}
