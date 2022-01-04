package com.example.backend.Services;

import com.example.backend.Beans.AdditionalBoatService;
import com.example.backend.Beans.AdditionalCottageService;
import com.example.backend.Beans.AdditionalService;
import com.example.backend.Dtos.AdditionalServiceDto;
import com.example.backend.Repository.AdditionalCottageServiceRepository;
import com.example.backend.Services.Interfaces.IAdditionalCottageService;
import org.springframework.beans.factory.annotation.Autowired;

public class AdditionalCottageServiceService implements IAdditionalCottageService {

    @Autowired
    private final AdditionalCottageServiceRepository additionalServiceRepository;

    @Autowired
    private CottageService cottageService;


    public AdditionalCottageServiceService(AdditionalCottageServiceRepository additionalServiceRepository){
        this.additionalServiceRepository = additionalServiceRepository;
    }

    @Override
    public AdditionalCottageService findAdditionalServiceById(long id){
        return additionalServiceRepository.findById(id).orElse(null);
    }

    @Override
    public AdditionalCottageService createCottageService(AdditionalServiceDto serviceDto) {
        AdditionalCottageService additionalCottageService = new AdditionalCottageService();
        additionalCottageService.setName(serviceDto.getName());
        additionalCottageService.setAddPrice(serviceDto.getAddPrice());
        additionalCottageService.setPriceList(cottageService.findById(serviceDto.getPriceList()).getCottagePriceList());
        return additionalServiceRepository.save(additionalCottageService);
    }
}
