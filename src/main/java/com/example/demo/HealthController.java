package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;

@Controller
public class HealthController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username, HttpSession session) {
        session.setAttribute("username", username);

        session.setAttribute("breakfastTotal", 0);
        session.setAttribute("lunchTotal", 0);
        session.setAttribute("dinnerTotal", 0);
        session.setAttribute("breakfastItems", new ArrayList<String>());
        session.setAttribute("lunchItems", new ArrayList<String>());
        session.setAttribute("dinnerItems", new ArrayList<String>());

        Managercontroller.getSharedWorkoutPlans().forEach(plan -> {
            session.setAttribute(plan.getKey() + "Claimed", false);
        });

        syncWithManager(session); 
        return "redirect:/home";
    }
    
    @GetMapping("/calculate")
    public String getCalculatePage(HttpSession session, Model model) {
        Integer targetKcal = (Integer) session.getAttribute("targetKcal");
        model.addAttribute("calories", targetKcal);
        return "calculate";
    }

    @PostMapping("/calculate")
    public String handleCalculation(
            @RequestParam double weight,
            @RequestParam double height,
            @RequestParam int age,
            @RequestParam String gender,
            @RequestParam double activity,
            HttpSession session,
            Model model) {

        HealthSolution userHealth = new HealthSolution(weight, height, age, gender, activity);
        int targetKcal = (int) userHealth.getTDEE();

        session.setAttribute("targetKcal", targetKcal);
        session.setAttribute("userWeight", weight);
        session.setAttribute("userHeight", height);
        session.setAttribute("userStatus", userHealth.getStatus());
        syncWithManager(session);

        model.addAttribute("weight", weight);
        model.addAttribute("height", height);
        model.addAttribute("age", age);
        model.addAttribute("gender", gender);
        model.addAttribute("activity", activity);
        model.addAttribute("calories", targetKcal);

        return "calculate";
    }

    @GetMapping("/food")
    public String showFoodPage(HttpSession session, Model model) {
        populateFoodModel(session, model);
        return "food";
    }

    @PostMapping("/food")
    public String addFood(
            @RequestParam String meal,
            @RequestParam(required = false) String foodName,
            @RequestParam(required = false) Integer kcal,
            @RequestParam(required = false) String presetFood,
            HttpSession session) {

        String selectedFoodName = foodName != null ? foodName.trim() : "";
        Integer selectedKcal = kcal;

        if ((selectedFoodName.isEmpty() || selectedKcal == null || selectedKcal <= 0)
                && presetFood != null
                && !presetFood.isBlank()) {
            String[] presetParts = presetFood.split("\\|");
            if (presetParts.length == 2) {
                selectedFoodName = presetParts[0];
                selectedKcal = Integer.parseInt(presetParts[1]);
            }
        }

        if (!selectedFoodName.isEmpty() && selectedKcal != null && selectedKcal > 0) {
            addFoodToSession(session, meal, selectedFoodName, selectedKcal);
        }
        syncWithManager(session);

        return "redirect:/food";
    }

    @GetMapping("/health")
    public String showHealthPage(HttpSession session, Model model) {
        model.addAttribute("currentWeight", getCurrentWeight(session));
        model.addAttribute("nowWeight", getCurrentWeight(session));
        model.addAttribute("goalWeight", getGoalWeight(session));
        model.addAttribute("weightHistory", getWeightHistory(session));
        return "health";
    }

    @PostMapping("/health")
    public String saveHealthData(
            @RequestParam(required = false) Double currentWeight,
            @RequestParam(required = false) Double goalWeight,
            @RequestParam(required = false) String recordDate,
            @RequestParam(required = false, defaultValue = "false") boolean updateGoalOnly,
            HttpSession session) {

        if (goalWeight != null && goalWeight > 0) {
            session.setAttribute("goalWeight", goalWeight);
        }

        if (!updateGoalOnly && currentWeight != null && currentWeight > 0) {
            session.setAttribute("userWeight", currentWeight);

            String savedDate = (recordDate != null && !recordDate.isBlank()) ? recordDate : "No date";
            List<String> history = getWeightHistory(session);
            history.add(0, savedDate + " - " + currentWeight + " kg");
            session.setAttribute("weightHistory", history);
        }
        syncWithManager(session);

        return "redirect:/health";
    }

    @GetMapping("/workout")
    public String showWorkoutPage(HttpSession session, Model model) {
        model.addAttribute("points", getPoints(session));
        model.addAttribute("workoutPlans", getWorkoutPlans(session));
        return "workout";
    }

    @PostMapping("/workout/claim")
    public String claimWorkoutPoint(@RequestParam String level, HttpSession session) {
        if (!isWorkoutClaimed(session, level)) {
            int rewardPoints = getWorkoutReward(level);
            session.setAttribute(level + "Claimed", true);
            session.setAttribute("points", getPoints(session) + rewardPoints);
            syncWithManager(session);
        }
        return "redirect:/workout";
    }

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        syncWithManager(session);
        model.addAttribute("points", getPoints(session));
        model.addAttribute("currentWeight", getCurrentWeight(session));
        model.addAttribute("breakfast", getMealTotal(session, "breakfast"));
        model.addAttribute("lunch", getMealTotal(session, "lunch"));
        model.addAttribute("dinner", getMealTotal(session, "dinner"));
        int totalEaten = getMealTotal(session, "breakfast") + getMealTotal(session, "lunch") + getMealTotal(session, "dinner");
        model.addAttribute("totalEaten", totalEaten);
        model.addAttribute("targetKcal", session.getAttribute("targetKcal"));
        return "home";
    }

    @GetMapping("/person")
    public String showProfile(HttpSession session, Model model) {
        syncWithManager(session);
        String username = (String) session.getAttribute("username");
        UserRecord user = Managercontroller.getUserRecord(username != null ? username : "Guest");
        
        model.addAttribute("user", user);
        model.addAttribute("weightHistory", getWeightHistory(session));
        return "person";
    }

    @PostMapping("/person/update")
    public String updateProfile(
            @RequestParam String username,
            @RequestParam(required = false) MultipartFile profileImage,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage,
            HttpSession session) throws IOException {
        
        session.setAttribute("username", username);

        if (removeImage) {
            session.removeAttribute("userImage");
        } else if (profileImage != null && !profileImage.isEmpty()) {
            byte[] bytes = profileImage.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            session.setAttribute("userImage", "data:" + profileImage.getContentType() + ";base64," + base64Image);
        }
        return "redirect:/person";
    }

    private void populateFoodModel(HttpSession session, Model model) {
        List<String> breakfastItems = getMealItems(session, "breakfast");
        List<String> lunchItems = getMealItems(session, "lunch");
        List<String> dinnerItems = getMealItems(session, "dinner");

        int breakfast = getMealTotal(session, "breakfast");
        int lunch = getMealTotal(session, "lunch");
        int dinner = getMealTotal(session, "dinner");
        int totalEaten = breakfast + lunch + dinner;

        model.addAttribute("breakfastItems", breakfastItems);
        model.addAttribute("lunchItems", lunchItems);
        model.addAttribute("dinnerItems", dinnerItems);
        model.addAttribute("breakfast", breakfast);
        model.addAttribute("lunch", lunch);
        model.addAttribute("dinner", dinner);
        model.addAttribute("totalEaten", totalEaten);
        model.addAttribute("presetFoods", getPresetFoods());
    }

    private void addFoodToSession(HttpSession session, String meal, String foodName, int kcal) {
        List<String> mealItems = getMealItems(session, meal);
        mealItems.add(foodName + " - " + kcal + " kcal");
        session.setAttribute(meal + "Items", mealItems);
        session.setAttribute(meal + "Total", getMealTotal(session, meal) + kcal);
    }

    @SuppressWarnings("unchecked")
    private List<String> getMealItems(HttpSession session, String meal) {
        List<String> items = (List<String>) session.getAttribute(meal + "Items");
        if (items == null) {
            items = new ArrayList<>();
            session.setAttribute(meal + "Items", items);
        }
        return items;
    }

    private int getMealTotal(HttpSession session, String meal) {
        Integer total = (Integer) session.getAttribute(meal + "Total");
        return total != null ? total : 0;
    }

    private Map<String, Integer> getPresetFoods() {
        return Managercontroller.getSharedPresetFoods();
    }

    private double getCurrentWeight(HttpSession session) {
        Double currentWeight = (Double) session.getAttribute("userWeight");
        return currentWeight != null ? currentWeight : 0.0;
    }

    private double getGoalWeight(HttpSession session) {
        Double goalWeight = (Double) session.getAttribute("goalWeight");
        return goalWeight != null ? goalWeight : 0.0;
    }

    @SuppressWarnings("unchecked")
    private List<String> getWeightHistory(HttpSession session) {
        List<String> history = (List<String>) session.getAttribute("weightHistory");
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute("weightHistory", history);
        }
        return history;
    }

    private int getPoints(HttpSession session) {
        Integer points = (Integer) session.getAttribute("points");
        return points != null ? points : 0;
    }

    private List<Map<String, Object>> getWorkoutPlans(HttpSession session) {
        return Managercontroller.getSharedWorkoutPlans().stream()
                .map(p -> createWorkoutPlan(
                        session,
                        p.getKey(),
                        p.getLevel(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getPoints(),
                        p.getExercises()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> createWorkoutPlan(
            HttpSession session,
            String key,
            String level,
            String title,
            String description,
            int points,
            List<String> exercises) {

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("key", key);
        plan.put("level", level);
        plan.put("title", title);
        plan.put("description", description);
        plan.put("points", points);
        plan.put("exercises", exercises);
        plan.put("completed", isWorkoutClaimed(session, key));
        return plan;
    }

    private boolean isWorkoutClaimed(HttpSession session, String levelKey) {
        Boolean claimed = (Boolean) session.getAttribute(levelKey + "Claimed");
        return claimed != null && claimed;
    }

    private int getWorkoutReward(String levelKey) {
        return Managercontroller.getSharedWorkoutPlans().stream()
                .filter(p -> p.getKey().equals(levelKey))
                .mapToInt(ManagedWorkoutPlan::getPoints)
                .findFirst()
                .orElse(0);
    }


    private void syncWithManager(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null || username.isBlank()) return;

        Integer targetKcal = (Integer) session.getAttribute("targetKcal");
        Double weight = (Double) session.getAttribute("userWeight");
        Double height = (Double) session.getAttribute("userHeight");
        String bmiStatus = (String) session.getAttribute("userStatus");
        Integer points = (Integer) session.getAttribute("points");
        int totalEaten = getMealTotal(session, "breakfast") + getMealTotal(session, "lunch") + getMealTotal(session, "dinner");

        Managercontroller.syncUser(username, targetKcal, weight, height, bmiStatus, points, totalEaten);
    }
}