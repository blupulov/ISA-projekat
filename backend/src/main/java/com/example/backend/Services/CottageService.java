package com.example.backend.Services;

import com.example.backend.Beans.*;
import com.example.backend.Dtos.AdditionalServiceDto;
import com.example.backend.Dtos.CottageDto;
import com.example.backend.Dtos.NewSubcriptionDto;
import com.example.backend.Repository.AddressRepository;
import com.example.backend.Repository.CottageRepository;
import com.example.backend.Repository.CottageReservationRepository;
import com.example.backend.Services.Interfaces.ICottageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
public class CottageService implements ICottageService {

    @Autowired
    private CottageOwnerService cottageOwnerService;

    @Autowired
    private CottageRepository cottageRepository;

    @Autowired
    private CottageReservationRepository cottageReservationRepository;

    @Autowired
    private AddressRepository addressRepository;
    @Autowired
    private CustomerService customerService;

    public CottageService(CottageRepository cottageRepository){
        this.cottageRepository = cottageRepository;
    }

    @Override
    public Collection<Cottage> findAllCottages() {
        return cottageRepository.findAll();
    }

    @Override
    public Cottage findById(long id) {
        Optional<Cottage> cottage = cottageRepository.findById(id);
        return cottage.orElse(null);
    }

    @Override
    @Transactional
    public Cottage saveCottage(CottageDto newCottage) {
        try {
        Cottage cottage = new Cottage();
        cottage.setName(newCottage.getName());
        cottage.setAddress(new Address());
        cottage.getAddress().setCity(newCottage.getCity());
        cottage.getAddress().setStreet(newCottage.getStreet());
        cottage.getAddress().setCountry(newCottage.getCountry());
        cottage.getAddress().setLatitude(newCottage.getLatitude());
        cottage.getAddress().setLongitude(newCottage.getLongitude());
        cottage.setConductRules(newCottage.getConductRules());
        cottage.setPromo(newCottage.getPromo());
        CottageOwner owner = cottageOwnerService.findCottageOwner(newCottage.getCottageOwnerId());
        cottage.setCottageOwner(owner);
        CottagePriceList priceList = new CottagePriceList();
        priceList.setPrice(newCottage.getPrice());
        cottage.setCottagePriceList(priceList);
        return save(cottage);
        }catch (Exception e)
        {
            return null;
        }
    }

    @Override
    @Transactional
    public Cottage updateCottage(CottageDto changeDto) {
        try {
            Cottage cottage = getCottageById(changeDto.getId());
            cottage.setName(changeDto.getName());
            cottage.getAddress().setCity(changeDto.getCity());
            cottage.getAddress().setStreet(changeDto.getStreet());
            cottage.getAddress().setCountry(changeDto.getCountry());
            cottage.getAddress().setLatitude(changeDto.getLatitude());
            cottage.getAddress().setLongitude(changeDto.getLongitude());
            cottage.setPromo(changeDto.getPromo());
            cottage.setConductRules(changeDto.getConductRules());
            cottage.getCottagePriceList().setPrice(changeDto.getPrice());
            return save(cottage);
        }catch (Exception e)
        {
            return null;
        }
    }

    @Transactional
    public Cottage addAdditionalService(AdditionalServiceDto serviceDto){
        Cottage cottage = findById(serviceDto.getPriceList());
        AdditionalCottageService additionalService45 = new AdditionalCottageService();
        additionalService45.setName(serviceDto.getName());
        additionalService45.setAddPrice(serviceDto.getAddPrice());
        additionalService45.setPriceList(cottage.getCottagePriceList());
        cottage.getCottagePriceList().getAdditionalServices().add(additionalService45);
        return save(cottage);
    }

    public boolean isUserSubcribed(long instructorId, long userId){
        Cottage cottage = findById(instructorId);
        Customer customer = customerService.findCustomerById(userId);

        return cottage.getPrepaidCustomers().contains(customer);
    }

    public void newSubscription(NewSubcriptionDto newSubcription){
        if(isUserSubcribed(newSubcription.getAdvertiserId(), newSubcription.getUserId())){
            return;
        }

        Cottage cottage = findById(newSubcription.getAdvertiserId());
        Customer customer = customerService.findCustomerById(newSubcription.getUserId());

        cottage.getPrepaidCustomers().add(customer);
        cottageRepository.save(cottage);
    }

    public void unsubscribe(NewSubcriptionDto toUnsubscribe){
        if(!isUserSubcribed(toUnsubscribe.getAdvertiserId(), toUnsubscribe.getUserId())){
            return;
        }

        Cottage cottage = findById(toUnsubscribe.getAdvertiserId());
        Customer customer = customerService.findCustomerById(toUnsubscribe.getUserId());

        cottage.getPrepaidCustomers().remove(customer);
        cottageRepository.save(cottage);
    }

    @Transactional
    public Cottage save(Cottage cottage){ return cottageRepository.save(cottage);}

    @Transactional(propagation = Propagation.REQUIRED)
    public Cottage getCottageById(long id){
        return cottageRepository.findCottageById(id);
    }

    @Transactional
    public boolean deleteCottage(long id) {
        try {
            if (getCottageById(id) != null) {
                Collection<CottageReservation> reservations = cottageReservationRepository.getAllCottageReservationInFuture(id, LocalDateTime.now());
                if (reservations.isEmpty()) {
                    deleteById(id);
                    return true;
                }
                return false;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    @Transactional
    public void deleteById(long id) {
        cottageRepository.deleteById(id);
    }
}
