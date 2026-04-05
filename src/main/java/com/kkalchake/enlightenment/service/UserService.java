package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.UserLoginDto;
import com.kkalchake.enlightenment.dto.UserRegistrationDto;

public interface UserService {
    void registerUser(UserRegistrationDto registrationDto);
    boolean verifyUser(UserLoginDto loginDto);
}