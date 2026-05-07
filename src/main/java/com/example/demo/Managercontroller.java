package com.example.demo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.io.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/manager")
public class Managercontroller {

    
    private static final Map<Integer, FoodItem>           foodDB    = new LinkedHashMap<>();
    private static final Map<Integer, ManagedWorkoutPlan> workoutDB = new LinkedHashMap<>();
    private static final Map<String, UserRecord>          userDB    = new LinkedHashMap<>();

    private static final AtomicInteger foodIdSeq    = new AtomicInteger(1);
    private static final AtomicInteger workoutIdSeq = new AtomicInteger(1);
    private static final AtomicInteger userIdSeq    = new AtomicInteger(1);

    
    static {
        seedDefaultFood();
        seedDefaultWorkouts();
    }

   
    @GetMapping("/home")
    public String managerHome(Model model) {
        int totalPoints = userDB.values().stream().mapToInt(UserRecord::getPoints).sum();
        int avgPts = userDB.isEmpty() ? 0 : totalPoints / userDB.size();

        model.addAttribute("totalUsers",        userDB.size());
        model.addAttribute("totalFoodItems",    foodDB.size());
        model.addAttribute("totalWorkouts",     workoutDB.size());
        model.addAttribute("totalPointsClaimed", totalPoints);
        model.addAttribute("avgPoints",         avgPts);

        List<FoodItem> recentFoods = new ArrayList<>(foodDB.values());
        if (recentFoods.size() > 5) recentFoods = recentFoods.subList(recentFoods.size() - 5, recentFoods.size());
        Collections.reverse(recentFoods);

        List<ManagedWorkoutPlan> recentWorkouts = new ArrayList<>(workoutDB.values());

        model.addAttribute("recentFoods",    recentFoods);
        model.addAttribute("recentWorkouts", recentWorkouts);
        model.addAttribute("allUsers",       new ArrayList<>(userDB.values()));

        return "managerHome";
    }


    @GetMapping("/food")
    public String foodPage(
            @RequestParam(required = false) Integer editId,
            @RequestParam(required = false) String sortBy, // สำหรับ Data Sorting
            Model model) {
        
        List<FoodItem> items = new ArrayList<>(foodDB.values());

        // ประยุกต์ใช้ Data Sorting
        if ("calories".equals(sortBy)) {
            items.sort(Comparator.comparingInt(FoodItem::getCalories));
        } else if ("name".equals(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getName));
        }

        model.addAttribute("foodItems", items);
        model.addAttribute("sortBy", sortBy);

        if (editId != null && foodDB.containsKey(editId)) {
            model.addAttribute("editFood", foodDB.get(editId));
        }
        return "food"; 
    }

    @PostMapping("/food/add")
    public String addFood(
            @RequestParam String name,
            @RequestParam int calories,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") double protein,
            @RequestParam(defaultValue = "0") double carbs,
            @RequestParam(defaultValue = "0") double fat,
            RedirectAttributes ra) {

        int id = foodIdSeq.getAndIncrement();
        FoodItem item = new FoodItem(id, name.trim(), calories, category, protein, carbs, fat);
        foodDB.put(id, item);
        ra.addFlashAttribute("successMsg", "✓ Added: " + name);
        return "redirect:/manager/food";
    }

    @GetMapping("/food/edit")
    public String editFoodPage(@RequestParam int id, Model model) {
        model.addAttribute("foodItems", new ArrayList<>(foodDB.values()));
        if (foodDB.containsKey(id)) {
            model.addAttribute("editFood", foodDB.get(id));
        }
        return "food";
    }

    @PostMapping("/food/update")
    public String updateFood(
            @RequestParam int id,
            @RequestParam String name,
            @RequestParam int calories,
            @RequestParam String category,
            @RequestParam(defaultValue = "0") double protein,
            @RequestParam(defaultValue = "0") double carbs,
            @RequestParam(defaultValue = "0") double fat,
            RedirectAttributes ra) {

        FoodItem item = foodDB.get(id);
        if (item != null) {
            item.setName(name.trim());
            item.setCalories(calories);
            item.setCategory(category);
            item.setProtein(protein);
            item.setCarbs(carbs);
            item.setFat(fat);
            ra.addFlashAttribute("successMsg", "✓ Updated: " + name);
        }
        return "redirect:/manager/food";
    }

    @PostMapping("/food/delete")
    public String deleteFood(@RequestParam int id, RedirectAttributes ra) {
        FoodItem removed = foodDB.remove(id);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted: " + removed.getName());
        }
        return "redirect:/manager/food";
    }

    public static Map<String, Integer> getSharedPresetFoods() {
        Map<String, Integer> preset = new LinkedHashMap<>();
        foodDB.values().forEach(f -> preset.put(f.getName(), f.getCalories()));
        return preset;
    }

    @GetMapping("/workout")
    public String workoutPage(@RequestParam(required = false) Integer editId, Model model) {
        model.addAttribute("workoutPlans", new ArrayList<>(workoutDB.values()));
        if (editId != null && workoutDB.containsKey(editId)) {
            model.addAttribute("editPlan", workoutDB.get(editId));
        }
        return "Manageworkout";
    }

    @PostMapping("/workout/add")
    public String addWorkout(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String level,
            @RequestParam int points,
            @RequestParam String exercisesRaw,
            @RequestParam(defaultValue = "30") int durationMinutes,
            RedirectAttributes ra) {

        int id = workoutIdSeq.getAndIncrement();
        List<String> exercises = parseExercises(exercisesRaw);
        ManagedWorkoutPlan plan = new ManagedWorkoutPlan(id, title.trim(), level, points,
                description.trim(), exercises, durationMinutes);
        workoutDB.put(id, plan);
        ra.addFlashAttribute("successMsg", "✓ Added: " + title);
        return "redirect:/manager/workout";
    }

    @GetMapping("/workout/edit")
    public String editWorkoutPage(@RequestParam int id, Model model) {
        model.addAttribute("workoutPlans", new ArrayList<>(workoutDB.values()));
        if (workoutDB.containsKey(id)) {
            model.addAttribute("editPlan", workoutDB.get(id));
        }
        return "Manageworkout";
    }

    @PostMapping("/workout/update")
    public String updateWorkout(
            @RequestParam int id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String level,
            @RequestParam int points,
            @RequestParam String exercisesRaw,
            @RequestParam(defaultValue = "30") int durationMinutes,
            RedirectAttributes ra) {

        ManagedWorkoutPlan plan = workoutDB.get(id);
        if (plan != null) {
            plan.setTitle(title.trim());
            plan.setDescription(description.trim());
            plan.setLevel(level);
            plan.setPoints(points);
            plan.setExercises(parseExercises(exercisesRaw));
            plan.setDurationMinutes(durationMinutes);
            plan.setKey(level);
            ra.addFlashAttribute("successMsg", "✓ Updated: " + title);
        }
        return "redirect:/manager/workout";
    }

    @PostMapping("/workout/delete")
    public String deleteWorkout(@RequestParam int id, RedirectAttributes ra) {
        ManagedWorkoutPlan removed = workoutDB.remove(id);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted: " + removed.getTitle());
        }
        return "redirect:/manager/workout";
    }

    
    public static List<ManagedWorkoutPlan> getSharedWorkoutPlans() {
        return new ArrayList<>(workoutDB.values());
    }

    
    @GetMapping("/users")
    public String usersPage(
            @RequestParam(required = false) String search, // สำหรับ Data Searching
            Model model) {
        
        List<UserRecord> users = new ArrayList<>(userDB.values());

        // ประยุกต์ใช้ Data Searching
        if (search != null && !search.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        int totalPoints = users.stream().mapToInt(UserRecord::getPoints).sum();
        int avgPts = users.isEmpty() ? 0 : totalPoints / users.size();

        model.addAttribute("allUsers",           users);
        model.addAttribute("avgPoints",          avgPts);
        return "manageUser";
    }

    @PostMapping("/users/reset")
    public String resetUser(@RequestParam String username, RedirectAttributes ra) {
        UserRecord user = userDB.get(username);
        if (user != null) {
            user.setPoints(0);
            user.setTotalEaten(0);
            user.setWeight(0);
            user.setTargetKcal(2000);
            user.setBmiStatus(null);
            ra.addFlashAttribute("successMsg", "✓ Reset data for: " + username);
        }
        return "redirect:/manager/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam String username, RedirectAttributes ra) {
        UserRecord removed = userDB.remove(username);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted user: " + username);
        }
        return "redirect:/manager/users";
    }

    
    public static void syncUser(String username, Integer targetKcal, Double weight, Double height,
                            String bmiStatus, Integer points, Integer totalEaten) {
        if (username == null || username.isBlank()) return;
        UserRecord record = userDB.computeIfAbsent(username,
                k -> new UserRecord(userIdSeq.getAndIncrement(), k));
        record.syncFromSession(username, targetKcal, weight, height, bmiStatus, points, totalEaten);
    }

    public static UserRecord getUserRecord(String username) {
        return userDB.get(username);
    }



    private List<String> parseExercises(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        return Arrays.stream(raw.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // ประยุกต์ใช้ File Input
    private static void seedDefaultFood() {
        try {
            // อ่านข้อมูลจากไฟล์ food_data.txt ใน src/main/resources
            InputStream is = new ClassPathResource("food_data.txt").getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length == 6) {
                    int id = foodIdSeq.getAndIncrement();
                    foodDB.put(id, new FoodItem(id, d[0].trim(),
                            Integer.parseInt(d[1].trim()), d[2].trim(),
                            Double.parseDouble(d[3].trim()),
                            Double.parseDouble(d[4].trim()),
                            Double.parseDouble(d[5].trim())));
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Could not load food data from file, using fallback.");
            // Fallback ถ้าโหลดไฟล์ไม่ได้
        }
    }

    private static void seedDefaultWorkouts() {
        Object[][] defaults = {
            {1, "Beginner", "Level 1", 50,
             "เหมาะสำหรับผู้เริ่มต้น ออกกำลังกายเบาๆ",
             List.of("เดินเร็ว 15 นาที", "Squat 10 ครั้ง", "ยืดเหยียด 5 นาที"), 25},
            {2, "Normal",   "Level 2", 80,
             "ความเข้มข้นปานกลาง เหมาะสำหรับผู้ที่ออกกำลังกายบ้างแล้ว",
             List.of("Jogging 20 นาที", "Push-up 15 ครั้ง", "Lunge 12 ครั้ง/ข้าง"), 35},
            {3, "Advanced", "Level 3", 120,
             "ความเข้มข้นสูง สำหรับผู้ที่มีประสบการณ์",
             List.of("Burpee 15 ครั้ง", "Mountain climber 30 วินาที x 3", "Plank 60 วินาที"), 45},
        };
        for (Object[] d : defaults) {
            int id = workoutIdSeq.getAndIncrement();
            @SuppressWarnings("unchecked")
            ManagedWorkoutPlan p = new ManagedWorkoutPlan(
                    id, (String) d[1], (String) d[2], (int) d[3],
                    (String) d[4], (List<String>) d[5], (int) d[6]);
            p.setKey((String) d[2]);
            workoutDB.put(id, p);
        }
    }
}