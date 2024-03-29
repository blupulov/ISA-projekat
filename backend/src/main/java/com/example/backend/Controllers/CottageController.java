package com.example.backend.Controllers;

import com.example.backend.Beans.*;
import com.example.backend.Dtos.AdditionalServiceDto;
import com.example.backend.Dtos.AvailablePeriodDto;
import com.example.backend.Dtos.CottageDto;
import com.example.backend.Dtos.NewSubcriptionDto;
import com.example.backend.Repository.CottagePriceListRepository;
import com.example.backend.Services.AdditionalCottageServiceService;
import com.example.backend.Services.AdminService;
import com.example.backend.Services.AvailablePeriodCottageService;
import com.example.backend.Services.CottageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@CrossOrigin(allowedHeaders = "*")
@RestController
@RequestMapping("cottage")
public class CottageController {

    private final CottageService cottageService;

    @Autowired
    private AdditionalCottageServiceService additionalCottageServiceService;

    @Autowired
    private AvailablePeriodCottageService availablePeriodCottageService;

    @Autowired
    public CottageController(CottageService cottageService){
        this.cottageService = cottageService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<Collection<Cottage>> getAll(){
        return new ResponseEntity<>(this.cottageService.findAllCottages(), HttpStatus.OK);
    }


    @GetMapping(path = "{id}")
    public ResponseEntity<Cottage> getCottageById(@PathVariable long id){
        Cottage cottage = cottageService.findById(id);
        if (cottage != null)
            return new ResponseEntity<>(cottage, HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/additionalServices/{cottageId}")
    public ResponseEntity<Collection<AdditionalCottageService>> getAdditionalServicesOfAdventure(
            @PathVariable long cottageId){
        return new ResponseEntity<>(cottageService.findById(cottageId).getCottagePriceList().getAdditionalServices(),
                HttpStatus.OK);
    }

    @GetMapping(path = "/availablePeriod/{cottageId}")
    public ResponseEntity<Collection<AvailablePeriodCottage>> getAvailablePeriodOfCottage(
            @PathVariable long cottageId){
        return new ResponseEntity<>(cottageService.findById(cottageId).getPeriods(),
                HttpStatus.OK);
    }

    @GetMapping (path = "isSubcribed/{cottageId}/{userId}")
    public ResponseEntity<Boolean> isCustomerSubscribedToCottage(@PathVariable long cottageId, @PathVariable long userId){

        return new ResponseEntity<>(cottageService.isUserSubcribed(cottageId, userId), HttpStatus.OK);
    }

    @PostMapping(consumes = "application/json")
    public Cottage save(@RequestBody CottageDto cottage){
        return this.cottageService.saveCottage(cottage);
    }

    @PutMapping(consumes = "application/json")
    public Cottage update(@RequestBody CottageDto cottage){

        return this.cottageService.updateCottage(cottage);
    }

    @PutMapping(path = "additionalService/")
    public Cottage addAdditionalService(@RequestBody AdditionalServiceDto serviceDto){
        return this.cottageService.addAdditionalService(serviceDto);
    }

    @PutMapping(path = "availablePeriod/")
    public AvailablePeriodCottage addAvailablePeriod(@RequestBody AvailablePeriodDto periodDto){
        return this.availablePeriodCottageService.createCottageAvailablePeriod(periodDto);
    }

    @PutMapping(path = "/subscribe", consumes = "application/json")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> subscribe(@RequestBody NewSubcriptionDto newSubcriptionDto){
        try {
            cottageService.newSubscription(newSubcriptionDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(path = "/unsubscribe", consumes = "application/json")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> unsubscribe(@RequestBody NewSubcriptionDto forUnsubscribe){
        try {
            cottageService.unsubscribe(forUnsubscribe);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<?> delete(@PathVariable long id){
        if(cottageService.deleteCottage(id)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "additionalService/{id}")
    public ResponseEntity<?> deleteAddService(@PathVariable long id){
        additionalCottageServiceService.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
