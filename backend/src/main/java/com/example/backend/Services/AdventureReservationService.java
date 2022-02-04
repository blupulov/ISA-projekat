package com.example.backend.Services;

import com.example.backend.Beans.AdditionalService;
import com.example.backend.Beans.AdventureReservation;
import com.example.backend.Beans.Customer;
import com.example.backend.Beans.FishingInstructor;
import com.example.backend.Dtos.*;
import com.example.backend.Beans.*;
import com.example.backend.Dtos.CustomerReserveTermDto;
import com.example.backend.Dtos.MakeFastReservationDto;
import com.example.backend.Dtos.ReservationSearchDto;
import com.example.backend.Dtos.ReserveAdventureDto;
import com.example.backend.Repository.AdventureReservationRepository;
import com.example.backend.Repository.CottageReservationRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
public class AdventureReservationService {

    @Autowired
    private final AdventureReservationRepository adventureReservationRepository;

    @Autowired
    private AdventureService adventureService;

    @Autowired
    private FishingInstructorService fishingInstructorService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AdditionalServiceService additionalServiceService;

    @Autowired
    private CottageReservationService cottageReservationService;

    @Autowired
    private BoatReservationService boatReservationService;


    public AdventureReservationService(AdventureReservationRepository repository, CustomerService customerServiceMock){
        this.adventureReservationRepository = repository;
        this.customerService = customerServiceMock;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AdventureReservation save(AdventureReservation adventureReservation){
        return this.adventureReservationRepository.save(adventureReservation);
    }

    public Collection<CalendarReservationsDto> getAllReservationsOfInstructorForCalendar(long instructorId){
        List<CalendarReservationsDto> reservations = new ArrayList<>();
        for(AdventureReservation ar :
                adventureReservationRepository.getAllReservationsOfInstructorForCalendar(instructorId)){
            reservations.add(new CalendarReservationsDto(ar));
        }
        return reservations;
    }

    public AdventureReservation getCurrentReservationOfInstructor(long instructorId){
        FishingInstructor instructor = this.fishingInstructorService.findFishingInstructorById(instructorId);
        for(Adventure adventure : instructor.getAdventures())
            for(AdventureReservation ar : adventure.getReservations()){
                LocalDateTime endTime = ar.getReservationStart().plusHours(ar.getLength()).plusMinutes(ar.getLengthMin());
                if(ar.getReservationStart().isBefore(LocalDateTime.now()) && endTime.isAfter(LocalDateTime.now()))
                    return ar;
            }
        return null;
    }

    public AdventureReservation getAdventureReservationById(long id){
        return findAdventureReservationById(id);
    }

    public Collection<AdventureReservation> getAllAdventureReservations(){
        return adventureReservationRepository.findAll();
    }

    @Transactional
    public AdventureReservation makeNewAppointment(CustomerReserveTermDto reservation) {
        AdventureReservation retVal = null;
        try{
           Customer customer = customerService.findCustomerById(reservation.getUserId());
           AdventureReservation adventureReservation = findAdventureReservation(reservation.getReservationId());
           if(adventureReservation.getCustomer() != null)
               return null;
            for (AdventureReservation ar : getAllFutureTermsByCustomerId(customer.getId())){
                if (isReservationsOverlapForAdventures(ar, adventureReservation))
                    return null;
            }
            for (CottageReservation cr : cottageReservationService.getAllFutureTermsByCustomerId(customer.getId())){
                if(isReservationsOverlapForCottages(cr, adventureReservation))
                    return null;
            }
            for(BoatReservation br: boatReservationService.getAllFutureTermsByCustomerId(customer.getId())){
                if(isReservationsOverlapForBoats(br, adventureReservation))
                    return null;
            }
            List<AdditionalService> services = findAllSelectedAdditionalServices(reservation.getServices());
            adventureReservation.setCustomer(customer);
            calculateFullPriceOfReservation(adventureReservation, services);
            retVal = save(adventureReservation);
            emailService.sendAdventureReservationConfirm(customer);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    //Isto kao obicna samo bez racunanje cene, jer je ona u napred zadata
    @Transactional
    public AdventureReservation makeNewAppointmentOnAction(CustomerReserveTermDto reservation) {
        AdventureReservation retVal = null;
        try {
            Customer customer = customerService.findCustomerById(reservation.getUserId());
            AdventureReservation adventureReservation = getAdventureReservationById(reservation.getReservationId());
            if (adventureReservation.getCustomer() != null)
                return null;
            for (AdventureReservation ar : getAllFutureTermsByCustomerId(customer.getId())) {
                if (isReservationsOverlapForAdventures(ar, adventureReservation))
                    return null;
            }
            for (CottageReservation cr : cottageReservationService.getAllFutureTermsByCustomerId(customer.getId())){
                if(isReservationsOverlapForCottages(cr, adventureReservation))
                    return null;
            }
            for(BoatReservation br: boatReservationService.getAllFutureTermsByCustomerId(customer.getId())){
                if(isReservationsOverlapForBoats(br, adventureReservation))
                    return null;
            }
            if (IsForbiddenToCustomer(adventureReservation, customer.getId()))
                return null;

            adventureReservation.setCustomer(customer);
            retVal = save(adventureReservation);
            emailService.sendAdventureReservationConfirm(customer);
            return retVal;
        }catch (Exception e){
            return retVal;
        }
    }

    public Collection<AdventureReservation> getAllNextReservedTermsOfAdventure(long adventureId){
        List<AdventureReservation> adventureReservations = new ArrayList<>();
        for (AdventureReservation ar : adventureReservationRepository.findAll())
            if(ar.isReserved() && ar.getAdventure().getId() == adventureId &&
                    ar.getReservationStart().isAfter(LocalDateTime.now()))
                adventureReservations.add(ar);

        return adventureReservations;
    }

    public Collection<AdventureReservation> getAllFreeFastReservations(long adventureId){
        List<AdventureReservation> reservations = new ArrayList<>();
        for(AdventureReservation ar : adventureService.findAdventureById(adventureId).getReservations())
            if(!ar.isReserved() && ar.getLastDateToReserve().isAfter(LocalDateTime.now()))
                reservations.add(ar);
        return reservations;
    }

    public Collection<AdventureReservation> getAllPastReservationOfInstructor(long instructorId){
        List<AdventureReservation> pastAdventure = new ArrayList<>();
        for (AdventureReservation ar: adventureReservationRepository.findAll()) {
            LocalDateTime reservationEnd = ar.getReservationStart().plusHours(ar.getLength()).plusMinutes(ar.getLengthMin());
            if(reservationEnd.isBefore(LocalDateTime.now()) && ar.isReserved() &&
                    ar.getAdventure().getInstructor().getId() == instructorId){
                pastAdventure.add(ar);
            }
        }
        return pastAdventure;
    }

    public Collection<AdventureReservation> getAllNextFreeActionsOfInstructor(long instructorId){
        List<AdventureReservation> nextActions = new ArrayList<>();
        for (AdventureReservation ar: adventureReservationRepository.findAll()) {
            if(ar.getReservationStart().isAfter(LocalDateTime.now()) && !ar.isReserved() &&
                    ar.getAdventure().getInstructor().getId() == instructorId && ar.getDiscount() != 0){
                nextActions.add(ar);
            }
        }
        return nextActions;
    }

    public Collection<AdventureReservation> getAllReservationOfCustomerForEvaluation(long customerId){
        return adventureReservationRepository.getAllReservationOfCustomerForEvaluation(customerId,
                LocalDateTime.now());
    }

    @Transactional
    public AdventureReservation markReservationAsEvaluated(long reservationId){
        AdventureReservation adventureReservation = findAdventureReservationById(reservationId);
        adventureReservation.setRated(true);
        return save(adventureReservation);
    }

    public AdventureReservation findAdventureReservationById(long id){
        return adventureReservationRepository.findById(id).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AdventureReservation findAdventureReservation(long id){
        return adventureReservationRepository.getAdventureReservationByReservationId(id);
    }

    public Collection<AdventureReservation> getAllAvailableReservationsForSearch(ReservationSearchDto search){
        List<AdventureReservation> reservations = new ArrayList<>();
        for (AdventureReservation ar: adventureReservationRepository.findByreservationStartBetween(search.getDateFrom(), search.getDateTo())){
            if (!ar.isReserved() && ar.getAdventure().getMaxPersons() >= search.getPersons() &&
                    ar.getLastDateToReserve().isAfter(LocalDateTime.now().plusDays(3)) &&
                    !IsForbiddenToCustomer(ar, search.getId()) && IsInLocation(ar, search.getCity()) && IsInCountry(ar, search.getCountry())){
                reservations.add(ar);
            }
        }
        return reservations;
    }


    @Transactional
    public AdventureReservation createFreeFastReservation(MakeFastReservationDto dto){
        AdventureReservation adventureReservation = makeAdventureReservation(dto);
        if(!isReservationInAvailableTimespanOfInstructor(adventureReservation, dto.getInstructorId()))
            return null;
        for(AdventureReservation ar : getAllNextReservationsOfInstructor(dto.getInstructorId())){
            if(isReservationsOverlapForAdventures(ar, adventureReservation))
                return null;
        }
        adventureReservation.setAdventure(adventureService.findAdventureById(dto.getAdventureId()));
        calculateFullPriceOfReservation(adventureReservation, dto.getAddServices());
        sendNotificationMailToAllPrepaidCustomers(dto.getAdventureId());
        return save(adventureReservation);
    }

    public boolean deleteAdventureReservation(long id){
        AdventureReservation adventureReservation = findAdventureReservationById(id);
        if(adventureReservation.isReserved())
            return false;
        adventureReservationRepository.deleteById(id);
        return true;
    }

    @Transactional
    public AdventureReservation cancelAdventureReservation(long id){
        AdventureReservation adventureReservation = findAdventureReservationById(id);
        LocalDateTime lastDateForCanceling = adventureReservation.getReservationStart().minusDays(3);
        if(LocalDateTime.now().isAfter(lastDateForCanceling))
            return null;
        adventureReservation.setCustomer(null);
        return save(adventureReservation);
    }


    //TODO: Rezervacija avanture
    @Transactional
    public AdventureReservation customReserveAdventure(ReserveAdventureDto dto){
        AdventureReservation reservation = prepareCustomReservationForSaving(dto);
        if(reservation != null)
            sendNotificationMailForCreatingCustomReservationToCustomer(reservation.getCustomer());
        return save(reservation);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<AdventureReservation> getAllFutureTermsByCustomerId(long id){
        return adventureReservationRepository.getAllReservationOfCustomerInFuture(id,
                LocalDateTime.now());
    }

    public Collection<AdventureReservation> getAllPastTermsWithoutComplaintByCustomerId(long id){
        return adventureReservationRepository.getAllReservationOfCustomerInPastWithoutComplaint(id,
                LocalDateTime.now());
    }

    public Collection<AdventureReservation> getAllPastTermsByCustomerId(long id){
        return adventureReservationRepository.getAllReservationOfCustomerInPast(id,
                LocalDateTime.now());
    }

    @Transactional
    public AdventureReservation cancelTerm(CancelTermDto data){
        try {
            Customer customer = this.customerService.findCustomerById(data.getUserId());
            AdventureReservation reservation = findAdventureReservation(data.getReservationId());
            if(customer.getId() != reservation.getCustomer().getId())
                return null;
            reservation.getForbidenCustomers().add(customer);
            reservation.setCustomer(null);
            return save(reservation);
        }catch (Exception e){
            return null;
        }
    }

    @Transactional
    public AdventureReservation markReservationAsReported(long id){
        try {
            AdventureReservation reservation = findAdventureReservationById(id);
            reservation.setReported(true);
            return save(reservation);
        }catch (Exception e){
            return null;
        }
    }

    private boolean IsInLocation(AdventureReservation ar, String city) {
        if (city == null)
            return true;
        return ar.getAdventure().getAddress().getCity().toUpperCase(Locale.ROOT).contains(city.toUpperCase(Locale.ROOT));
    }

    private boolean IsInCountry(AdventureReservation ar, String country) {
        if (country == null)
            return true;
        return ar.getAdventure().getAddress().getCountry().toUpperCase(Locale.ROOT).contains(country.toUpperCase(Locale.ROOT));
    }

    private AdventureReservation prepareCustomReservationForSaving(ReserveAdventureDto dto){
        AdventureReservation reservation = makeCustomReservation(dto);
        if(reservation == null)
            return null;
        List<AdditionalService> services = findAllSelectedAdditionalServices(dto.getSelectedAdditionalServicesIds());
        calculateFullPriceOfReservation(reservation, services);
        return reservation;
    }

    private AdventureReservation makeCustomReservation(ReserveAdventureDto dto){
        AdventureReservation reservation = new AdventureReservation();
        if(dto.getReservationStart().isBefore(LocalDateTime.now()))
            return null;

        reservation.setReservationStart(dto.getReservationStart());
        reservation.setAdventure(this.adventureService.findAdventureById(dto.getAdventureId()));
        reservation.setLength(dto.getLength());
        reservation.setLastDateToReserve(LocalDateTime.now());
        reservation.setDiscount(dto.getDiscount());
        reservation.setLengthMin(dto.getLengthMin());
        if(!addCustomerToReservation(reservation, dto))
            return null;
        else return reservation;
    }

    //PROVERA DA LI CUSTOMER IMA NEKE REZERVACIJE U TO VREME
    private boolean addCustomerToReservation(AdventureReservation reservation, ReserveAdventureDto dto) {
        Customer customer = this.customerService.findByEmail(dto.getCustomerMail());
        for(AdventureReservation ar : customer.getAdventureReservations()){
            if(isReservationsOverlapForAdventures(ar, reservation))
                return false;
        }
        for (CottageReservation cr : customer.getCottageReservations()){
            if(isReservationsOverlapForCottages(cr, reservation))
                return false;
        }

        for(BoatReservation br: customer.getBoatReservations()){
            if(isReservationsOverlapForBoats(br, reservation))
                return false;
        }
        reservation.setCustomer(customer);
        return true;
    }

    private List<AdditionalService> findAllSelectedAdditionalServices(List<Long> ids){
        List<AdditionalService> services = new ArrayList<>();
        for(long id : ids)
            services.add(this.additionalServiceService.findAdditionalServiceById(id));
        return services;
    }

    private Collection<AdventureReservation> getAllNextReservationsOfInstructor(long instructorId){
        List<AdventureReservation> allNextReservations = new ArrayList<>();
        for(AdventureReservation ar : getAllAdventureReservations())
            if(ar.getAdventure().getInstructor().getId() == instructorId &&
                    ar.getReservationStart().isAfter(LocalDateTime.now()))
                allNextReservations.add(ar);

        return allNextReservations;
    }

    //METODA PROVERAVA DA LI SE DVA TERMINA PREKLAPAJU
    private boolean isReservationsOverlapForAdventures(
            AdventureReservation existingReservation, AdventureReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationStart().plusHours(existingReservation.getLength())
                .plusMinutes(existingReservation.getLengthMin());
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationStart().plusHours(newReservation.getLength())
                .plusMinutes(newReservation.getLengthMin());
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapForCottages(
            CottageReservation existingReservation, AdventureReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationEnd();
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationStart().plusHours(newReservation.getLength())
                .plusMinutes(newReservation.getLengthMin());
        return isToRangesOverlaps(existingStart, existingEnd, newStart, newEnd);
    }

    private boolean isReservationsOverlapForBoats(
            BoatReservation existingReservation, AdventureReservation newReservation){
        LocalDateTime existingStart = existingReservation.getReservationStart();
        LocalDateTime existingEnd = existingReservation.getReservationEnd();
        LocalDateTime newStart = newReservation.getReservationStart();
        LocalDateTime newEnd = newReservation.getReservationStart().plusHours(newReservation.getLength())
                .plusMinutes(newReservation.getLengthMin());
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

    private boolean isReservationInAvailableTimespanOfInstructor(
            AdventureReservation adventureReservation, long instructorId) {
        LocalDateTime reservationEnd = adventureReservation.getReservationStart()
                .plusHours(adventureReservation.getLength()).plusMinutes(adventureReservation.getLengthMin());
        FishingInstructor instructor = fishingInstructorService.findFishingInstructorById(instructorId);

        return adventureReservation.getReservationStart().isAfter(instructor.getAvailable().getFromDate()) &&
                reservationEnd.isBefore(instructor.getAvailable().getToDate());
    }

    private AdventureReservation makeAdventureReservation(MakeFastReservationDto dto){
        AdventureReservation adventureReservation = new AdventureReservation();
        adventureReservation.setReservationStart(dto.getAdventureStart());
        adventureReservation.setLastDateToReserve(dto.getLastDateToReserve());
        adventureReservation.setLength(dto.getLength());
        adventureReservation.setDiscount(dto.getDiscount());
        adventureReservation.setLengthMin(dto.getLengthMin());
        return adventureReservation;
    }

    private void sendNotificationMailToAllPrepaidCustomers(long adventureId){
        for(Customer customer : adventureService.findAdventureById(adventureId).getInstructor().getPrepaidCustomers()){
            try {
                emailService.sendNotificationForCreatingNewAdventureReservation(customer);
            }catch (Exception e){System.out.println(e.toString());}
        }
    }

    private void sendNotificationMailForCreatingCustomReservationToCustomer(Customer customer){
        try {
            emailService.sendNotificationMailForCreatingCustomReservationToCustomer(customer);
        }catch (Exception e){System.out.println(e.toString());}
    }

    //PROVERA DA LI JE ZABRANJENO KORISNIKU DA ZAKAZUJE PONOVO OVAJ TERMIN
    private boolean IsForbiddenToCustomer(AdventureReservation ar, long id){
        Customer customer = customerService.findCustomerById(id);
        return ar.getForbidenCustomers().contains(customer);
    }

    //TODO: MENJANO NAKNADNO U SLUCAJU BAGOVA
    private void calculateFullPriceOfReservation(
            AdventureReservation reservation, List<AdditionalService> addServices){
        int price = reservation.getAdventure().getPriceList().getPrice();
        for(AdditionalService as : addServices){
            price += as.getAddPrice();
            reservation.addAdditionalService(as);
        }
        reservation.setPrice((int) (price * (1 - reservation.getDiscount()/100)));
    }

    private void deleteReservation(long id){
        adventureReservationRepository.deleteById(id);
    }


    public Collection<GraphDataDto> getAllReservationsForGraph(GraphRequestDto dto){
        List<GraphDataDto> retVal = new ArrayList<>();
        List<GraphDataDto> list = (List<GraphDataDto>) prepareReservationForGraphCounting(dto);
        for(GraphDataDto mainG : list){
            if(!isGraphDataDtoExistsInList(retVal, mainG))
                for(GraphDataDto minorG : list)
                    if(minorG.getDate().equals(mainG.getDate()))
                        mainG.incrementNumber();
            if(mainG.getNumber() != 0)
                retVal.add(mainG);
        }
        return retVal;
    }

    private boolean isGraphDataDtoExistsInList(List<GraphDataDto> list, GraphDataDto graphDataDto){
        for (GraphDataDto g : list)
            if(g.getDate().equals(graphDataDto.getDate()))
                return true;
        return false;
    }
    private Collection<GraphDataDto> prepareReservationForGraphCounting(GraphRequestDto dto){
        List<GraphDataDto> retVal = new ArrayList<>();
        List<AdventureReservation> reservations =
                (List<AdventureReservation>) adventureReservationRepository.getAllReservationsForGraph(
                        dto.getStartTime(), dto.getEndTime(), dto.getInstructorId());
        for (AdventureReservation ar : reservations){
            GraphDataDto dataDto = new GraphDataDto();
            dataDto.setDate(ar.getReservationStart().toLocalDate());
            retVal.add(dataDto);
        }
        return retVal;
    }
}
