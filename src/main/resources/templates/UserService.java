package com.example.calories.service;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<Map<String, Object>> getAllUsers();
    Map<String, Object> getUserByUsername(String username);
    void resetUserData(String username);
    void deleteUser(String username);
}