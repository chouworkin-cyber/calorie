package com.example.calories.controller;

import com.example.calories.model.UserProfile;
import com.example.calories.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class CalculationController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @GetMapping("/calculate")
    public String showCalculationForm(Model model, HttpSession session) {
        return "calculate";
    }

    @PostMapping("/calculate")
    public String calculateCalories(
            @RequestParam("weight") double weight,
            @RequestParam("height") double height,
            @RequestParam("age") int age,
            @RequestParam("gender") String gender,
            @RequestParam("activity") double activity,
            Model model,
            HttpSession session 
    ) {
        UserProfile userProfile = new UserProfile(weight, height, age, gender, activity);

       
        userProfileRepository.save(userProfile);
        System.out.println("User profile saved to database: " + userProfile); 

        double bmr;
        if ("male".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }
        double tdee = bmr * activity;

        model.addAttribute("weight", weight);
        model.addAttribute("height", height);
        model.addAttribute("age", age);
        model.addAttribute("gender", gender);
        model.addAttribute("activity", activity);
        model.addAttribute("calories", tdee);

        return "calculate";
    }
}