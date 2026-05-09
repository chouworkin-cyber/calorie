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

        // ตรวจสอบว่าเป็นกลุ่มผู้จัดการหรือไม่ (manager, admin, admin1)
        boolean isManager = "manager".equalsIgnoreCase(username) || 
                            "admin".equalsIgnoreCase(username) || 
                            "admin1".equalsIgnoreCase(username);

        // ตรวจสอบความยาวรหัสผ่านเฉพาะผู้ใช้ทั่วไป (ต้องไม่ต่ำกว่า 8 ตัว)
        if (!isManager && (password == null || password.length() < 8)) {
            ra.addFlashAttribute("loginError", "รหัสผ่านต้องมีความยาวอย่างน้อย 8 ตัวอักษร");
            return "redirect:/login";
        }

        // ล้างข้อมูลเซสชันเก่าทั้งหมดทิ้ง (รวมถึงข้อมูลคนก่อนหน้า) เพื่อเริ่มเซสชันใหม่ที่สะอาด 100%
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true); 
        
        session.setAttribute("username", username); // ต้องตั้งค่า username ในเซสชันใหม่ทันที

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
            session.setAttribute("userStatus", existingUser.getBmiStatus());
            session.setAttribute("points", existingUser.getPoints());
            session.setAttribute("breakfastTotal", existingUser.getBreakfastTotal());
            session.setAttribute("lunchTotal", existingUser.getLunchTotal());
            session.setAttribute("dinnerTotal", existingUser.getDinnerTotal());
            session.setAttribute("breakfastItems", new ArrayList<>(existingUser.getBreakfastItems()));
            session.setAttribute("lunchItems", new ArrayList<>(existingUser.getLunchItems()));
            session.setAttribute("dinnerItems", new ArrayList<>(existingUser.getDinnerItems()));
            session.setAttribute("weightHistory", new ArrayList<>(existingUser.getWeightHistory()));
            
            existingUser.getClaimedWorkouts().forEach((key, val) -> {
                session.setAttribute(key + "Claimed", val);
            });
        } else {
            // กำหนดค่าเริ่มต้นสำหรับผู้ใช้ใหม่
            // Username และ Display Name คือชื่อเดียวกันที่ได้จากการเข้าสู่ระบบครั้งแรก
            session.setAttribute("password", password);
            session.setAttribute("userWeight", 0.0);
            session.setAttribute("goalWeight", 0.0);
            session.setAttribute("userHeight", 0.0);
            session.setAttribute("userStatus", "normal");
            session.setAttribute("breakfastTotal", 0);
            session.setAttribute("lunchTotal", 0);
            session.setAttribute("dinnerTotal", 0);
            session.setAttribute("breakfastItems", new ArrayList<String>());
            session.setAttribute("lunchItems", new ArrayList<String>());
            session.setAttribute("dinnerItems", new ArrayList<String>());
            session.setAttribute("points", 0);
            session.setAttribute("weightHistory", new ArrayList<String>());
            session.setAttribute("targetKcal", 2000);

            Managercontroller.getSharedWorkoutPlans().forEach(plan -> {
                // ตรวจสอบให้แน่ใจว่า plan.getKey() ไม่เป็น null ก่อนใช้งาน
                session.setAttribute(plan.getKey() + "Claimed", false);
            });
        }

        syncWithManager(session); 
        return "redirect:/home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // ทำลายเซสชันเมื่อผู้ใช้กดออกจากระบบ
        session.invalidate();
        return "redirect:/login";
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

        // บันทึกน้ำหนักลงในประวัติโดยอัตโนมัติเมื่อมีการคำนวณใหม่
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

        // ระบบกรองตามระดับความยาก (Level Filter)
        if (level != null && !level.isBlank()) {
            plans = plans.stream()
                    .filter(p -> ((String) p.get("level")).equalsIgnoreCase(level))
                    .collect(Collectors.toList());
        }

        // เพิ่มระบบค้นหาท่าออกกำลังกายสำหรับผู้ใช้
        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase();
            plans = plans.stream()
                    .filter(p -> ((String) p.get("title")).toLowerCase().contains(query) ||
                                 ((String) p.get("level")).toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }

        // เพิ่มระบบเรียงลำดับท่าออกกำลังกายสำหรับผู้ใช้
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
        // ตรวจสอบสถานะการรับคะแนนโดยใช้ key (ID) แทน level
        if (key != null && !key.isBlank() && !isWorkoutClaimed(session, key)) { 
            int rewardPoints = getWorkoutReward(key);
            session.setAttribute(key + "Claimed", true); // บันทึกสถานะแยกตาม key
            session.setAttribute("points", getPoints(session) + rewardPoints);
            syncWithManager(session);
        }
        return "redirect:/workout";
    }

    @GetMapping("/home")
    public String showHome(HttpSession session, Model model) {
        syncWithManager(session);
        String username = (String) session.getAttribute("username");

        // คำนวณอันดับของผู้ใช้จากคะแนนทั้งหมด
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
        return "home";
    }

    @GetMapping("/person")
    public String showProfile(
            @RequestParam(required = false) String sortHistory,
            HttpSession session, Model model) {
        syncWithManager(session);
        String username = (String) session.getAttribute("username");
        if (username == null) username = "Guest";

        // คำนวณอันดับของผู้ใช้
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
        
        // วิธีแก้: หากหาใน DB ไม่เจอ (กรณี manager) ให้สร้าง object จำลองขึ้นมาแสดงผลจาก Session แทน
        if (user == null) {
            user = new UserRecord(-1, username); // ใช้ ID ติดลบเพื่อระบุว่าเป็นตัวชั่วคราว
            user.setPoints(getPoints(session));
            user.setWeight(getCurrentWeight(session));
            user.setGoalWeight(getGoalWeight(session));
            user.setBmiStatus((String) session.getAttribute("userStatus"));
            user.setProfileImage((String) session.getAttribute("userImage"));
            
            Double h = (Double) session.getAttribute("userHeight");
            user.setHeight(h != null ? h : 0.0);
            Integer tk = (Integer) session.getAttribute("targetKcal");
            user.setTargetKcal(tk != null ? tk : 2000);
            user.getWeightHistory().addAll(rawHistory); // เพิ่มประวัติจาก session เข้าไปใน user object ชั่วคราว
        }
        
        List<String> history = new ArrayList<>(rawHistory);
        // เรียงลำดับประวัติน้ำหนัก (จากเก่าไปใหม่)
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
        
        syncWithManager(session); // บันทึกการเปลี่ยนแปลงชื่อและรูปภาพลงฐานข้อมูลทันที
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

        // เพิ่มระบบค้นหาเมนูอาหารสำเร็จรูป
        Map<String, Integer> presets = getPresetFoods();
        
        // 1. กรองตามระดับแคลอรี่ที่ต้องการ (Filter by calorie level)
        if (maxCal != null && maxCal > 0) {
            presets = presets.entrySet().stream()
                    .filter(e -> e.getValue() <= maxCal)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        }

        // 2. หากไม่มีคำค้นหาชื่อ และไม่มีการกรองแคลอรี่ ให้แสดงเพียงบางเมนู (6 รายการ)
        if ((searchPreset == null || searchPreset.isBlank()) && (maxCal == null || maxCal <= 0)) {
            presets = presets.entrySet().stream()
                    .limit(0) // จำกัดจำนวนเมนูที่แสดงเมื่อยังไม่มีการค้นหา
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        } else {
            // หากมีคำค้นหา ให้กรองตามคำค้นหาทั้งหมด
            String query = searchPreset.toLowerCase();
            presets = presets.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase().contains(query))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                            (v1, v2) -> v1, LinkedHashMap::new));
        }

        // ระบบเรียงลำดับแคลอรี่ (น้อยไปมาก / มากไปน้อย)
        // การเรียงลำดับจะถูกนำไปใช้กับรายการที่ถูกจำกัดหรือกรองแล้ว
        // หากต้องการให้เรียงลำดับก่อนจำกัด/กรอง ต้องย้ายบล็อกนี้ขึ้นไปก่อน
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
        String password = (String) session.getAttribute("password");
        String userImage = (String) session.getAttribute("userImage");
        Double weight = (Double) session.getAttribute("userWeight");
        Double goalWeight = (Double) session.getAttribute("goalWeight");
        Double height = (Double) session.getAttribute("userHeight");
        String bmiStatus = (String) session.getAttribute("userStatus");
        Integer points = (Integer) session.getAttribute("points");
        int totalEaten = getMealTotal(session, "breakfast") + getMealTotal(session, "lunch") + getMealTotal(session, "dinner");
        
        Integer bTotal = getMealTotal(session, "breakfast");
        Integer lTotal = getMealTotal(session, "lunch");
        Integer dTotal = getMealTotal(session, "dinner");
        List<String> bItems = getMealItems(session, "breakfast");
        List<String> lItems = getMealItems(session, "lunch");
        List<String> dItems = getMealItems(session, "dinner");
        List<String> history = getWeightHistory(session);
        
        Map<String, Boolean> workouts = new LinkedHashMap<>();
        Managercontroller.getSharedWorkoutPlans().forEach(p -> {
            workouts.put(p.getKey(), isWorkoutClaimed(session, p.getKey()));
        });

        Managercontroller.syncUser(username, password, userImage, targetKcal, weight, goalWeight, height, bmiStatus, points, totalEaten,
                                   bTotal, lTotal, dTotal, bItems, lItems, dItems, history, workouts);
    }
}