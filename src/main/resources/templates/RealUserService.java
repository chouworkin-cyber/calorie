package com.example.calories.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// สมมติว่ามี UserRepository และ User Entity อยู่แล้ว
// import com.example.calories.repository.UserRepository;

@Service
@Profile("prod")
public class RealUserService implements UserService {

    // @Autowired
    // private UserRepository userRepository;

    @Override
    public List<Map<String, Object>> getAllUsers() {
        // return userRepository.findAll().stream().map(user -> convertToMap(user)).collect(Collectors.toList());
        return null; // Logic ดึงจาก DB จริง
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        return null;
    }

    @Override
    public void resetUserData(String username) {
    }

    @Override
    public void deleteUser(String username) {
    }
}