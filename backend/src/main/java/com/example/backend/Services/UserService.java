package com.example.backend.Services;

import com.example.backend.Beans.User;
import com.example.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class UserService {
    @Autowired
    public final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public Collection<User> getAllNotEnabledUsers(){
        List<User> users = new ArrayList<>();
        for(User u : userRepository.findAll())
            if(!u.getEnabled() && !u.getRole().getName().equals("ROLE_CUSTOMER"))
                users.add(u);
        return users;
    }
}