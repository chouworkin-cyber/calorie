package com.example.demo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.io.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    private static final String DATA_FILE = "app_data.ser";

    
    static {
        loadData();
        if (foodDB.isEmpty()) seedDefaultFood();
        if (workoutDB.isEmpty()) seedDefaultWorkouts();
    }

    private static synchronized void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            Map<String, Object> allData = new HashMap<>();
            allData.put("food", new HashMap<>(foodDB));
            allData.put("workout", new HashMap<>(workoutDB));
            allData.put("user", new HashMap<>(userDB));
            allData.put("foodSeq", foodIdSeq.get());
            allData.put("workoutSeq", workoutIdSeq.get());
            allData.put("userSeq", userIdSeq.get());
            oos.writeObject(allData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, Object> allData = (Map<String, Object>) ois.readObject();
            foodDB.putAll((Map<Integer, FoodItem>) allData.get("food"));
            workoutDB.putAll((Map<Integer, ManagedWorkoutPlan>) allData.get("workout"));
            userDB.putAll((Map<String, UserRecord>) allData.get("user"));
            userDB.remove("manager"); // ล้างชื่อ manager ออกหากมีค้างอยู่ในไฟล์ข้อมูลเดิม
            foodIdSeq.set((Integer) allData.get("foodSeq"));
            workoutIdSeq.set((Integer) allData.get("workoutSeq"));
            userIdSeq.set((Integer) allData.get("userSeq"));
        } catch (Exception e) {
            System.err.println("Could not load saved data: " + e.getMessage());
        }
    }

   
    @GetMapping("/home")
    public String managerHome(
            @RequestParam(required = false) String searchUser,
            Model model) {
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

        // เพิ่มระบบค้นหาผู้ใช้ในหน้าสรุปผล (Dashboard)
        List<UserRecord> users = new ArrayList<>(userDB.values());
        // จัดลำดับตามคะแนนจากมากไปน้อยเป็นค่าเริ่มต้น
        users.sort(Comparator.comparingInt(UserRecord::getPoints).reversed());

        if (searchUser != null && !searchUser.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(searchUser.toLowerCase()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("recentFoods",    recentFoods);
        model.addAttribute("recentWorkouts", recentWorkouts);
        model.addAttribute("allUsers",       users);
        model.addAttribute("searchUser",     searchUser);

        return "managerHome";
    }


    @GetMapping("/food")
    public String foodPage(
            @RequestParam(required = false) Integer editId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String searchName,
            @RequestParam(required = false) String searchCategory,
            Model model) {
        
        List<FoodItem> items = new ArrayList<>(foodDB.values());

        // ค้นหาแยกตามชื่อ
        if (searchName != null && !searchName.isBlank()) {
            items = items.stream()
                    .filter(i -> i.getName().toLowerCase().contains(searchName.toLowerCase()))
                    .collect(Collectors.toList());
        }
        // ค้นหาแยกตามหมวดหมู่
        if (searchCategory != null && !searchCategory.isBlank()) {
            items = items.stream()
                    .filter(i -> i.getCategory().toLowerCase().contains(searchCategory.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // เรียงลำดับข้อมูล
        if ("calories".equals(sortBy)) {
            items.sort(Comparator.comparingInt(FoodItem::getCalories));
        } else if ("name".equals(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getName));
        } else if ("category".equals(sortBy)) {
            items.sort(Comparator.comparing(FoodItem::getCategory));
        } else if ("protein".equals(sortBy)) {
            items.sort(Comparator.comparingDouble(FoodItem::getProtein).reversed());
        } else if ("carbs".equals(sortBy)) {
            items.sort(Comparator.comparingDouble(FoodItem::getCarbs).reversed());
        } else if ("fat".equals(sortBy)) {
            items.sort(Comparator.comparingDouble(FoodItem::getFat).reversed());
        }

        model.addAttribute("foodItems", items);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchCategory", searchCategory);

        if (editId != null && foodDB.containsKey(editId)) {
            model.addAttribute("editFood", foodDB.get(editId));
        }
        return "Managefood"; 
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
        saveData();
        ra.addFlashAttribute("successMsg", "✓ Added: " + name);
        return "redirect:/manager/food";
    }

    @GetMapping("/food/edit")
    public String editFoodPage(@RequestParam int id, Model model) {
        model.addAttribute("foodItems", new ArrayList<>(foodDB.values()));
        if (foodDB.containsKey(id)) {
            model.addAttribute("editFood", foodDB.get(id));
        }
        return "Managefood";
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
            saveData();
        }
        return "redirect:/manager/food";
    }

    @PostMapping("/food/delete")
    public String deleteFood(@RequestParam int id, RedirectAttributes ra) {
        FoodItem removed = foodDB.remove(id);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted: " + removed.getName());
            saveData();
        }
        return "redirect:/manager/food";
    }

    @PostMapping("/food/upload")
    public String uploadFoodFile(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "กรุณาเลือกไฟล์ก่อนอัปโหลด");
            return "redirect:/manager/food";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 3) { // ตรวจสอบข้อมูลพื้นฐาน (ชื่อ, แคลอรี่, หมวดหมู่)
                    int id = foodIdSeq.getAndIncrement();
                    String name = d[0].trim();
                    int calories = Integer.parseInt(d[1].trim());
                    String category = d[2].trim();
                    double protein = (d.length > 3) ? Double.parseDouble(d[3].trim()) : 0;
                    double carbs   = (d.length > 4) ? Double.parseDouble(d[4].trim()) : 0;
                    double fat     = (d.length > 5) ? Double.parseDouble(d[5].trim()) : 0;

                    foodDB.put(id, new FoodItem(id, name, calories, category, protein, carbs, fat));
                    count++;
                }
            }
            saveData();
            ra.addFlashAttribute("successMsg", "✓ นำเข้าข้อมูลอาหารสำเร็จ " + count + " รายการ");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "เกิดข้อผิดพลาดในการอ่านไฟล์: " + e.getMessage());
        }
        return "redirect:/manager/food";
    }

    public static Map<String, Integer> getSharedPresetFoods() {
        Map<String, Integer> preset = new LinkedHashMap<>();
        foodDB.values().forEach(f -> preset.put(f.getName(), f.getCalories()));
        return preset;
    }

    @GetMapping("/workout")
    public String workoutPage(
            @RequestParam(required = false) Integer editId,
            @RequestParam(required = false) String searchTitle,
            @RequestParam(required = false) String searchLevel,
            @RequestParam(required = false) String sortBy,
            Model model) {
        
        List<ManagedWorkoutPlan> plans = new ArrayList<>(workoutDB.values());

        // ค้นหาแยกตามชื่อท่า
        if (searchTitle != null && !searchTitle.isBlank()) {
            plans = plans.stream()
                    .filter(p -> p.getTitle().toLowerCase().contains(searchTitle.toLowerCase()))
                    .collect(Collectors.toList());
        }
        // ค้นหาแยกตามระดับความยาก
        if (searchLevel != null && !searchLevel.isBlank()) {
            plans = plans.stream()
                    .filter(p -> p.getLevel().toLowerCase().contains(searchLevel.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // เรียงลำดับตามแต้ม, เวลา หรือชื่อ
        if ("points".equals(sortBy)) {
            plans.sort(Comparator.comparingInt(ManagedWorkoutPlan::getPoints));
        } else if ("duration".equals(sortBy)) {
            plans.sort(Comparator.comparingInt(ManagedWorkoutPlan::getDurationMinutes));
        } else if ("title".equals(sortBy)) {
            plans.sort(Comparator.comparing(ManagedWorkoutPlan::getTitle));
        } else if ("level".equals(sortBy)) {
            plans.sort(Comparator.comparing(ManagedWorkoutPlan::getLevel));
        }

        model.addAttribute("workoutPlans", plans);
        model.addAttribute("searchTitle", searchTitle);
        model.addAttribute("searchLevel", searchLevel);
        model.addAttribute("sortBy", sortBy);

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
        saveData();
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
            ra.addFlashAttribute("successMsg", "✓ Updated: " + title);
            saveData();
        }
        return "redirect:/manager/workout";
    }

    @PostMapping("/workout/delete")
    public String deleteWorkout(@RequestParam int id, RedirectAttributes ra) {
        ManagedWorkoutPlan removed = workoutDB.remove(id);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted: " + removed.getTitle());
            saveData();
        }
        return "redirect:/manager/workout";
    }

    @PostMapping("/workout/upload")
    public String uploadWorkoutFile(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("errorMsg", "กรุณาเลือกไฟล์ก่อนอัปโหลด");
            return "redirect:/manager/workout";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 6) { // ชื่อท่า, คำอธิบาย, ระดับ, แต้ม, เวลา, รายการท่า(แยกด้วย |)
                    int id = workoutIdSeq.getAndIncrement();
                    String title = d[0].trim();
                    String description = d[1].trim();
                    String level = d[2].trim();
                    int points = Integer.parseInt(d[3].trim());
                    int duration = Integer.parseInt(d[4].trim());
                    List<String> exercises = Arrays.stream(d[5].split("\\|"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    ManagedWorkoutPlan plan = new ManagedWorkoutPlan(id, title, level, points, description, exercises, duration);
                    workoutDB.put(id, plan);
                    count++;
                }
            }
            saveData();
            ra.addFlashAttribute("successMsg", "✓ นำเข้าข้อมูลท่าออกกำลังกายสำเร็จ " + count + " รายการ");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "เกิดข้อผิดพลาดในการอ่านไฟล์: " + e.getMessage());
        }
        return "redirect:/manager/workout";
    }

    
    public static List<ManagedWorkoutPlan> getSharedWorkoutPlans() {
        return new ArrayList<>(workoutDB.values());
    }

    
    @GetMapping("/users")
    public String usersPage(
            @RequestParam(required = false) String searchUsername,
            @RequestParam(required = false) String searchBmi,
            @RequestParam(required = false) String sortBy,
            Model model) {
        
        List<UserRecord> users = new ArrayList<>(userDB.values());

        // ค้นหาแยกตามชื่อผู้ใช้
        if (searchUsername != null && !searchUsername.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(searchUsername.toLowerCase()))
                    .collect(Collectors.toList());
        }
        // ค้นหาแยกตามสถานะ BMI
        if (searchBmi != null && !searchBmi.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getBmiStatus() != null && u.getBmiStatus().toLowerCase().contains(searchBmi.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // เรียงลำดับผู้ใช้ (เช่น แต้มมากที่สุด หรือเรียงตามชื่อ)
        if ("points".equals(sortBy)) {
            users.sort(Comparator.comparingInt(UserRecord::getPoints).reversed());
        } else if ("username".equals(sortBy)) {
            users.sort(Comparator.comparing(UserRecord::getUsername));
        } else if ("weight".equals(sortBy)) {
            users.sort(Comparator.comparingDouble(UserRecord::getWeight).reversed());
        } else if ("height".equals(sortBy)) {
            users.sort(Comparator.comparingDouble(UserRecord::getHeight).reversed());
        } else if ("totalEaten".equals(sortBy)) {
            users.sort(Comparator.comparingInt(UserRecord::getTotalEaten).reversed());
        }

        int totalPoints = users.stream().mapToInt(UserRecord::getPoints).sum();
        int avgPts = users.isEmpty() ? 0 : totalPoints / users.size();

        model.addAttribute("allUsers",           users);
        model.addAttribute("avgPoints",          avgPts);
        model.addAttribute("searchUsername",     searchUsername);
        model.addAttribute("searchBmi",          searchBmi);
        model.addAttribute("sortBy",             sortBy);
        return "Manageuser";
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
            saveData();
        }
        return "redirect:/manager/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam String username, RedirectAttributes ra) {
        UserRecord removed = userDB.remove(username);
        if (removed != null) {
            ra.addFlashAttribute("successMsg", "✓ Deleted user: " + username);
            saveData();
        }
        return "redirect:/manager/users";
    }

    
    public static void syncUser(String username, String password, Integer targetKcal, Double weight, Double goalWeight, Double height,
                            String bmiStatus, Integer points, Integer totalEaten,
                            Integer bTotal, Integer lTotal, Integer dTotal,
                            List<String> bItems, List<String> lItems, List<String> dItems,
                            List<String> history, Map<String, Boolean> workouts) {

        // ไม่บันทึกผู้ใช้งานที่ใช้ชื่อ "manager" หรือ "admin" ลงในฐานข้อมูลกลาง เพื่อไม่ให้ปรากฏในส่วนจัดการผู้ใช้
        if (username == null || username.isBlank() || "manager".equalsIgnoreCase(username)) {
            return;
        }

        UserRecord record = userDB.computeIfAbsent(username,
                k -> new UserRecord(userIdSeq.getAndIncrement(), k));
        record.syncFromSession(username, password, targetKcal, weight, goalWeight, height, bmiStatus, points, totalEaten,
                               bTotal, lTotal, dTotal, bItems, lItems, dItems, history, workouts);
        saveData();
    }

    public static UserRecord getUserRecord(String username) {
        return userDB.get(username);
    }

    // ดึงรายชื่อผู้ใช้ทั้งหมดที่จัดลำดับคะแนนแล้ว
    public static List<UserRecord> getAllUsersSorted() {
        List<UserRecord> users = new ArrayList<>(userDB.values());
        users.sort(Comparator.comparingInt(UserRecord::getPoints).reversed());
        return users;
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
            System.err.println("Could not load food data from file, seeding default settings.");
            
            // กำหนดรายการอาหารมาตรฐาน (Settings) เป็นค่าเริ่มต้นของระบบ
            Object[][] defaultFoods = {
                {"rice", 160, "Carbohydrate", 3.0, 35.0, 0.0},
                {"chicken breast", 120, "Protein", 25.0, 0.0, 2.0},
                {"boiled egg", 75, "Protein", 7.0, 1.1, 5.0},
                {"stir-fried vegetables", 150, "Vegetable", 3.0, 10.0, 12.0},
                {"apple", 52, "Fruit", 0.3, 14.0, 0.2},
                {"plain milk (200ml)", 120, "Beverage", 8.0, 12.0, 5.0},
                {"หมาล่า", 120, "Protein", 25.0, 0.0, 2.0},
                {"น้ำปั่น", 75, "Protein", 7.0, 1.1, 5.0},
                {"สลัดผัก", 150, "Vegetable", 3.0, 10.0, 12.0},
                {"มะม่วง", 52, "Fruit", 0.3, 14.0, 0.2},
                {"นมจืด (200ml)", 120, "Beverage", 8.0, 12.0, 5.0}
            };
            for (Object[] f : defaultFoods) {
                int id = foodIdSeq.getAndIncrement();
                foodDB.put(id, new FoodItem(id, (String)f[0], (int)f[1], (String)f[2], (double)f[3], (double)f[4], (double)f[5]));
            }
            saveData(); // บันทึกค่าเซทติ้งลงไฟล์ถาวรทันที
        }
    }

    private static void seedDefaultWorkouts() {
        Object[][] defaults = {
            {1, "Beginner", "Level 1", 50,
             "basic exercises for those new to fitness",
             List.of("walking 15 minutes", "Squat 10 times", "Stretching 5 minutes"), 25},
            {2, "Normal",   "Level 2", 80,
             "moderate intensity, suitable for those who have some workout experience",
             List.of("Jogging 20 minutes", "Push-up 15 times", "Lunge 12 times/side"), 35},
            {3, "Advanced", "Level 3", 120,
             "high intensity, suitable for those with experience",
             List.of("Burpee 15 times", "Mountain climber 30 seconds x 3", "Plank 60 seconds"), 45},
        };
        for (Object[] d : defaults) {
            int id = workoutIdSeq.getAndIncrement();
            @SuppressWarnings("unchecked")
            ManagedWorkoutPlan p = new ManagedWorkoutPlan(
                    id, (String) d[1], (String) d[2], (int) d[3],
                    (String) d[4], (List<String>) d[5], (int) d[6]);
            workoutDB.put(id, p);
        }
    }
}