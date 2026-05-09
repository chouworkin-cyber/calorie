package com.example.demo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
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
    public String handleLogin(@RequestParam String username, 
                              @RequestParam String password, 
                              HttpServletRequest request,
                              RedirectAttributes ra) {

        boolean isManager = "manager".equalsIgnoreCase(username) || 
                            "admin".equalsIgnoreCase(username) || 
                            "admin1".equalsIgnoreCase(username);

        
        if (!isManager && (password == null || password.length() < 8)) {
            ra.addFlashAttribute("loginError", "password must be at least 8 characters for regular users");
            return "redirect:/login";
        }

        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true); 
        
        session.setAttribute("username", username);

        UserRecord existingUser = Managercontroller.getUserRecord(username);
        if (existingUser != null) {
            if (!password.equals(existingUser.getPassword())) {
                ra.addFlashAttribute("loginError", "Invalid password for existing user");
                return "redirect:/login";
            }
        
            session.setAttribute("password", existingUser.getPassword());
            session.setAttribute("userImage", existingUser.getProfileImage());
            session.setAttribute("targetKcal", existingUser.getTargetKcal());
            session.setAttribute("userWeight", existingUser.getWeight());
            session.setAttribute("goalWeight", existingUser.getGoalWeight());
            session.setAttribute("userHeight", existingUser.getHeight());
            session.setAttribute("userAge", existingUser.getAge());
            session.setAttribute("userGender", existingUser.getGender());
            session.setAttribute("userActivity", existingUser.getActivity());
            session.setAttribute("userStatus", existingUser.getBmiStatus());
            session.setAttribute("hasSetup", existingUser.isHasSetup());
            session.setAttribute("points", existingUser.getPoints());
            session.setAttribute("breakfastTotal", existingUser.getBreakfastTotal());
            session.setAttribute("lunchTotal", existingUser.getLunchTotal());
            session.setAttribute("dinnerTotal", existingUser.getDinnerTotal());
            session.setAttribute("breakfastItems", new ArrayList<>(existingUser.getBreakfastItems()));
            session.setAttribute("lunchItems", new ArrayList<>(existingUser.getLunchItems()));
            session.setAttribute("dinnerItems", new ArrayList<>(existingUser.getDinnerItems()));
            session.setAttribute("weightHistory", new ArrayList<>(existingUser.getWeightHistory()));
            session.setAttribute("foodLog", new ArrayList<>(existingUser.getFoodLog()));
            session.setAttribute("workoutLog", new ArrayList<>(existingUser.getWorkoutLog()));
            session.setAttribute("dailyKcalLog", new ArrayList<>(existingUser.getDailyKcalLog()));
            
            existingUser.getClaimedWorkouts().forEach((key, val) -> {
                session.setAttribute(key + "Claimed", val);
            });
        } else {
            session.setAttribute("password", password);
            session.setAttribute("userWeight", 0.0);
            session.setAttribute("goalWeight", 0.0);
            session.setAttribute("userHeight", 0.0);
            session.setAttribute("userAge", 0);
            session.setAttribute("userGender", "male");
            session.setAttribute("userActivity", 1.2);
            session.setAttribute("userStatus", "normal");
            session.setAttribute("hasSetup", false);
            session.setAttribute("breakfastTotal", 0);
            session.setAttribute("lunchTotal", 0);
            session.setAttribute("dinnerTotal", 0);
            session.setAttribute("breakfastItems", new ArrayList<String>());
            session.setAttribute("lunchItems", new ArrayList<String>());
            session.setAttribute("dinnerItems", new ArrayList<String>());
            session.setAttribute("points", 0);
            session.setAttribute("weightHistory", new ArrayList<String>());
            session.setAttribute("foodLog", new ArrayList<String>());
            session.setAttribute("workoutLog", new ArrayList<String>());
            session.setAttribute("dailyKcalLog", new ArrayList<String>());
            session.setAttribute("targetKcal", 2000);

            Managercontroller.getSharedWorkoutPlans().forEach(plan -> {
                session.setAttribute(plan.getKey() + "Claimed", false);
            });
        }

        syncWithManager(session); 
        return "redirect:/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        syncWithManager(session); // บันทึกข้อมูลครั้งสุดท้ายก่อนล้าง Session
        session.invalidate();
        return "redirect:/login";
    }
    
    @GetMapping("/calculate")
    public String getCalculatePage(HttpSession session, Model model) {
        Integer targetKcal = (Integer) session.getAttribute("targetKcal");
        
        // ดึงข้อมูลร่างกายจาก Session มากรอกในฟอร์มให้อัตโนมัติ (Persistence)
        model.addAttribute("weight", session.getAttribute("userWeight"));
        model.addAttribute("height", session.getAttribute("userHeight"));
        model.addAttribute("age", session.getAttribute("userAge"));
        model.addAttribute("gender", session.getAttribute("userGender"));
        model.addAttribute("activity", session.getAttribute("userActivity"));
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
        session.setAttribute("userAge", age);
        session.setAttribute("userGender", gender);
        session.setAttribute("userActivity", activity);
        session.setAttribute("userStatus", userHealth.getStatus());

        List<String> history = getWeightHistory(session);
        String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
        history.add(0, dateStr + " - " + String.format("%.1f", weight) + " kg (Calculated)");
        session.setAttribute("weightHistory", history);

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
    public String showFoodPage(@RequestParam(required = false) String searchPreset, 
                               @RequestParam(required = false, defaultValue = "lowCal") String sortBy,
                               @RequestParam(required = false) Integer maxCal,
                               HttpSession session, Model model) {
        populateFoodModel(session, model, searchPreset, sortBy, maxCal);
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
            
            // เพิ่มประวัติการกินลงใน Log รวม
            String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
            List<String> log = getFoodLog(session);
            log.add(0, dateStr + " - " + selectedFoodName + " (" + selectedKcal + " kcal)");
            session.setAttribute("foodLog", log);
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

            String savedDate = (recordDate != null && !recordDate.isBlank()) ? recordDate : new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
            List<String> history = getWeightHistory(session);
            history.add(0, savedDate + " - " + String.format("%.1f", currentWeight) + " kg");
            session.setAttribute("weightHistory", history);
        }
        syncWithManager(session);

        return "redirect:/health";
    }

    @GetMapping("/workout")
    public String showWorkoutPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String level,
            HttpSession session, Model model) {
        
        List<Map<String, Object>> plans = getWorkoutPlans(session);

        if (level != null && !level.isBlank()) {
            plans = plans.stream()
                    .filter(p -> ((String) p.get("level")).equalsIgnoreCase(level))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase();
            plans = plans.stream()
                    .filter(p -> ((String) p.get("title")).toLowerCase().contains(query) ||
                                 ((String) p.get("level")).toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        if ("points".equals(sortBy)) {
            plans.sort(Comparator.comparingInt(p -> (int) p.get("points")));
        } else if ("title".equals(sortBy)) {
            plans.sort(Comparator.comparing(p -> (String) p.get("title")));
        } else if ("level".equals(sortBy)) {
            plans.sort(Comparator.comparing(p -> (String) p.get("level")));
        }

        model.addAttribute("points", getPoints(session));
        model.addAttribute("workoutPlans", plans);
        model.addAttribute("search", search);
        model.addAttribute("level", level);
        model.addAttribute("sortBy", sortBy);
        return "workout";
    }

    @PostMapping("/workout/claim")
    public String claimWorkoutPoint(@RequestParam(required = false) String key, HttpSession session) {
        if (key != null && !key.isBlank() && !isWorkoutClaimed(session, key)) { 
            int rewardPoints = getWorkoutReward(key);
            session.setAttribute(key + "Claimed", true);
            session.setAttribute("points", getPoints(session) + rewardPoints);
            
        
            String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());
            List<String> log = getWorkoutLog(session);
            log.add(0, dateStr + " - " + getWorkoutTitle(key) + " (+" + rewardPoints + " pts)");
            session.setAttribute("workoutLog", log);

            syncWithManager(session);
        }
        return "redirect:/workout";
    }

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        syncWithManager(session);
        String username = (String) session.getAttribute("username");

        List<UserRecord> rankedUsers = Managercontroller.getAllUsersSorted();
        int rank = -1;
        if (username != null) {
            for (int i = 0; i < rankedUsers.size(); i++) {
                if (rankedUsers.get(i).getUsername().equals(username)) {
                    rank = i + 1;
                    break;
                }
            }
        }

        model.addAttribute("points", getPoints(session));
        model.addAttribute("userRank", rank);
        model.addAttribute("totalRankedUsers", rankedUsers.size());
        model.addAttribute("currentWeight", getCurrentWeight(session));
        model.addAttribute("breakfast", getMealTotal(session, "breakfast"));
        model.addAttribute("lunch", getMealTotal(session, "lunch"));
        model.addAttribute("dinner", getMealTotal(session, "dinner"));
        int totalEaten = getMealTotal(session, "breakfast") + getMealTotal(session, "lunch") + getMealTotal(session, "dinner");
        model.addAttribute("totalEaten", totalEaten);
        model.addAttribute("targetKcal", session.getAttribute("targetKcal"));

        model.addAttribute("foodLog", getFoodLog(session));
        model.addAttribute("workoutLog", getWorkoutLog(session));
        model.addAttribute("dailyKcalLog", getDailyKcalLog(session));
        return "home";
    }

    @GetMapping("/person")
    public String showProfile(
            @RequestParam(required = false) String sortHistory,
            HttpSession session, Model model) {
        syncWithManager(session);
        String username = (String) session.getAttribute("username");
        if (username == null) username = "Guest";

        List<UserRecord> rankedUsers = Managercontroller.getAllUsersSorted();
        int rank = 0;
        for (int i = 0; i < rankedUsers.size(); i++) {
            if (rankedUsers.get(i).getUsername().equals(username)) {
                rank = i + 1;
                break;
            }
        }
        model.addAttribute("userRank", rank > 0 ? rank : "-");
        model.addAttribute("totalUsers", rankedUsers.size());

        UserRecord user = Managercontroller.getUserRecord(username);
        List<String> rawHistory = getWeightHistory(session);
        
        if (user == null) {
            user = new UserRecord(-1, username);
            user.setPoints(getPoints(session));
            user.setWeight(getCurrentWeight(session));
            user.setGoalWeight(getGoalWeight(session));
            user.setBmiStatus((String) session.getAttribute("userStatus"));
            user.setProfileImage((String) session.getAttribute("userImage"));
            
            Double h = (Double) session.getAttribute("userHeight");
            user.setHeight(h != null ? h : 0.0);
            Integer tk = (Integer) session.getAttribute("targetKcal");
            user.setTargetKcal(tk != null ? tk : 2000);
            user.getWeightHistory().addAll(rawHistory);
        }
        
        List<String> history = new ArrayList<>(rawHistory);
        if ("oldest".equals(sortHistory)) {
            Collections.reverse(history);
        }

        model.addAttribute("user", user);
        model.addAttribute("weightHistory", history);
        model.addAttribute("sortHistory", sortHistory);
        return "person";
    }

    @PostMapping("/person/update")
    public String updateProfile(
            @RequestParam(required = false) MultipartFile profileImage,
            @RequestParam(required = false, defaultValue = "false") boolean removeImage,
            HttpSession session,
            RedirectAttributes ra) throws IOException {
        
        String currentUsername = (String) session.getAttribute("username");

        if (removeImage) {
            session.removeAttribute("userImage");
        } else if (profileImage != null && !profileImage.isEmpty()) {
            byte[] bytes = profileImage.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(bytes);
            session.setAttribute("userImage", "data:" + profileImage.getContentType() + ";base64," + base64Image);
        }
        
        syncWithManager(session);
        return "redirect:/person";
    }

    private void populateFoodModel(HttpSession session, Model model, String searchPreset, String sortBy, Integer maxCal) {
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

        Map<String, Integer> presets = getPresetFoods();
        
        if (maxCal != null && maxCal > 0) {
            presets = presets.entrySet().stream()
                    .filter(e -> e.getValue() <= maxCal)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        }

        if ((searchPreset == null || searchPreset.isBlank()) && (maxCal == null || maxCal <= 0)) {
            presets = presets.entrySet().stream()
                    .limit(6)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        } else {
            String query = searchPreset.toLowerCase();
            presets = presets.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase().contains(query))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        }

        if ("lowCal".equals(sortBy) || "caloriesAsc".equals(sortBy)) {
            presets = presets.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        } else if ("highCal".equals(sortBy)) {
            presets = presets.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        model.addAttribute("presetFoods", presets);
        model.addAttribute("searchPreset", searchPreset);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("maxCal", maxCal);
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

    @SuppressWarnings("unchecked")
    private List<String> getFoodLog(HttpSession session) {
        List<String> log = (List<String>) session.getAttribute("foodLog");
        if (log == null) {
            log = new ArrayList<>();
            session.setAttribute("foodLog", log);
        }
        return log;
    }

    @SuppressWarnings("unchecked")
    private List<String> getWorkoutLog(HttpSession session) {
        List<String> log = (List<String>) session.getAttribute("workoutLog");
        if (log == null) {
            log = new ArrayList<>();
            session.setAttribute("workoutLog", log);
        }
        return log;
    }

    @SuppressWarnings("unchecked")
    private List<String> getDailyKcalLog(HttpSession session) {
        List<String> log = (List<String>) session.getAttribute("dailyKcalLog");
        if (log == null) {
            log = new ArrayList<>();
            session.setAttribute("dailyKcalLog", log);
        }
        return log;
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

    private String getWorkoutTitle(String key) {
        return Managercontroller.getSharedWorkoutPlans().stream()
                .filter(p -> p.getKey().equals(key))
                .map(ManagedWorkoutPlan::getTitle)
                .findFirst().orElse("Workout");
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
        String password = (String) session.getAttribute("password");
        String userImage = (String) session.getAttribute("userImage");
        Double weight = (Double) session.getAttribute("userWeight");
        Double goalWeight = (Double) session.getAttribute("goalWeight");
        Double height = (Double) session.getAttribute("userHeight");
        Integer age = (Integer) session.getAttribute("userAge");
        String gender = (String) session.getAttribute("userGender");
        Double activity = (Double) session.getAttribute("userActivity");
        String bmiStatus = (String) session.getAttribute("userStatus");
        Boolean hasSetup = (Boolean) session.getAttribute("hasSetup");
        Integer points = (Integer) session.getAttribute("points");
        int totalEaten = getMealTotal(session, "breakfast") + getMealTotal(session, "lunch") + getMealTotal(session, "dinner");
        
        Integer bTotal = getMealTotal(session, "breakfast");
        Integer lTotal = getMealTotal(session, "lunch");
        Integer dTotal = getMealTotal(session, "dinner");
        List<String> bItems = getMealItems(session, "breakfast");
        List<String> lItems = getMealItems(session, "lunch");
        List<String> dItems = getMealItems(session, "dinner");
        List<String> history = getWeightHistory(session);
        List<String> foodLog = getFoodLog(session);
        List<String> workoutLog = getWorkoutLog(session);

        // อัปเดตประวัติแคลอรี่รวมรายวัน (Aggregation)
        String dateStr = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        List<String> dailyKcalLog = getDailyKcalLog(session);
        boolean found = false;
        for (int i = 0; i < dailyKcalLog.size(); i++) {
            if (dailyKcalLog.get(i).startsWith(dateStr)) {
                dailyKcalLog.set(i, dateStr + " - Total: " + totalEaten + " kcal");
                found = true;
                break;
            }
        }
        if (!found) {
            dailyKcalLog.add(0, dateStr + " - Total: " + totalEaten + " kcal");
        }
        session.setAttribute("dailyKcalLog", dailyKcalLog);

        
        Map<String, Boolean> workouts = new LinkedHashMap<>();
        Managercontroller.getSharedWorkoutPlans().forEach(p -> {
            workouts.put(p.getKey(), isWorkoutClaimed(session, p.getKey()));
        });

        Managercontroller.syncUser(username, password, userImage, targetKcal, weight, goalWeight, height, age, gender, activity, bmiStatus, 
                                   hasSetup, points, totalEaten,
                                   bTotal, lTotal, dTotal, bItems, lItems, dItems, history, foodLog, workoutLog, dailyKcalLog, workouts);
    }
}