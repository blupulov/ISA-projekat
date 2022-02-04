package com.example.backend.Services;

import com.example.backend.Beans.DeleteProfileRequest;
import com.example.backend.Beans.User;
import com.example.backend.Dtos.AnswerOnRequestForDeletingDto;
import com.example.backend.Repository.DeleteProfileRequestRepository;
import com.example.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserService {
    @Autowired
    public final UserRepository userRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private DeleteProfileRequestService deleteProfileRequestService;
    @Autowired
    private DeleteProfileRequestRepository deleteProfileRequestRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Collection<User> getAllNotEnabledUsers(){
        List<User> users = new ArrayList<>();
        for(User u : userRepository.findAll())
            if(!u.getEnabled() && !u.getRole().getName().equals("ROLE_CUSTOMER"))
                users.add(u);
        return users;
    }

    @Transactional(readOnly = true)
    public User findUserById(long id){
        return userRepository.findById(id).orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User enableUser(long id) {
        User user = userRepository.findOneById(id);

        if(user.isEnabled())
            return null;

        user.setEnabled(true);
        try {
            emailService.sendConfirmMailToUser(user);
        }catch (Exception e){ return null; }

        return user;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean disableUserRegistration(long id, String reason){
        User user = userRepository.findOneById(id);
        try {
            deleteUser(id);
            emailService.sendNegativeMailToUser(user, reason);
            return true;
        }catch (Exception e){ System.out.println(e.toString()); return false;}
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(long id){
        userRepository.deleteById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void answerOnRequestForDeleting(AnswerOnRequestForDeletingDto dto) {
        DeleteProfileRequest request = deleteProfileRequestService.markDeleteProfileRequestAsReviewed(dto.getRequestId());

        if(request == null)
            return;

        if(dto.isForDelete())
            deleteUser(request.getUser().getId());

        try { emailService.sendAnswerOnRequestForDeletingProfile(request.getUser(), dto.getAnswer());
        }catch (Exception e){ System.out.println(e);}
    }

    @Transactional(readOnly = true)
    public Collection<User> getAllUsersExceptAdmins(){
        return userRepository.getAllUsersExceptAdmins();
    }

    @Transactional
    public User disableUser(long id){
        User user = userRepository.findOneById(id);
        if(!user.isEnabled())
            return null;
        user.setEnabled(false);
        try {
            emailService.sendMailForDisabling(user);
        }catch (Exception ignore) {}
        return save(user);
    }

    @Transactional
    public User save(User user){
        return userRepository.save(user);
    }
}
