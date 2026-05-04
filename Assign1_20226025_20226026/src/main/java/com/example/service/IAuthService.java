package com.example.service;


import com.example.model.User;

public interface IAuthService {
    User login(String username, String password) throws Exception;
    User register(String name, String username, String password) throws Exception;
}