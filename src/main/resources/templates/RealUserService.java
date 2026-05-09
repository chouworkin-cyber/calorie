package com.example.calories.service;

import com.example.calories.model.User;
import com.example.calories.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Profile("prod")
public class RealUserService implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(this::convertToMap).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToMap)
                .orElse(null);
    }

    @Override
    @Transactional
    public void resetUserData(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setTotalPoints(0);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            userRepository.delete(user);
        });
    }

    private Map<String, Object> convertToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("targetKcal", user.getTargetKcal());
        map.put("weight", user.getWeight());
        map.put("bmiStatus", user.getBmiStatus());
        map.put("points", user.getTotalPoints());
        map.put("email", user.getEmail());
        return map;
    }
}