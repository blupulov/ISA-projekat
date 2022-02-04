package com.example.backend.Services;

import com.example.backend.Beans.DeleteProfileRequest;
import com.example.backend.Dtos.MakeDeleteProfileRequestDto;
import com.example.backend.Repository.DeleteProfileRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class DeleteProfileRequestService {
    @Autowired
    private final DeleteProfileRequestRepository deleteProfileRequestRepository;
    @Autowired
    private UserService userService;

    public DeleteProfileRequestService(DeleteProfileRequestRepository deleteProfileRequestRepository){
        this.deleteProfileRequestRepository = deleteProfileRequestRepository;
    }

    @Transactional(readOnly = true)
    public Collection<DeleteProfileRequest> getAllNotReviewedRequestsForProfileDeleting(){
        return this.deleteProfileRequestRepository.getAllNotReviewedRequestsForProfileDeleting();
    }

    @Transactional
    public DeleteProfileRequest save(DeleteProfileRequest deleteProfileRequest){
        return this.deleteProfileRequestRepository.save(deleteProfileRequest);
    }

    @Transactional
    public DeleteProfileRequest findDeleteProfileRequestById(long id){
        return this.deleteProfileRequestRepository.findOneById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeleteProfileRequest markDeleteProfileRequestAsReviewed(long id){
        DeleteProfileRequest request = deleteProfileRequestRepository.findOneById(id);
        if(request.isReviewed())
            return null;
        request.setReviewed(true);
        return save(request);
    }

    @Transactional
    public DeleteProfileRequest makeDeleteProfileRequest(MakeDeleteProfileRequestDto dto){
        DeleteProfileRequest deleteProfileRequest = new DeleteProfileRequest();
        deleteProfileRequest.setRequestText(dto.getReason());
        deleteProfileRequest.setUser(userService.findUserById(dto.getUserId()));
        return save(deleteProfileRequest);
    }
}
