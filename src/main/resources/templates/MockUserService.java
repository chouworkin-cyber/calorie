package com.example.calories.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Profile("dev")
public class MockUserService implements UserService {

    @Override
    public List<Map<String, Object>> getAllUsers() {
        Map<String, Object> user1 = new HashMap<>();
        user1.put("username", "MockUser_1");
        user1.put("targetKcal", 2000);
        user1.put("weight", 70.5);
        user1.put("bmiStatus", "normal");
        user1.put("points", 500);
        user1.put("totalEaten", 1200);

        return Arrays.asList(user1);
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        return null;
    }

    @Override
    public void resetUserData(String username) {
        System.out.println("[Mock] Resetting data for: " + username);
    }

    @Override
    public void deleteUser(String username) {
        System.out.println("[Mock] Deleting user: " + username);
    }
}