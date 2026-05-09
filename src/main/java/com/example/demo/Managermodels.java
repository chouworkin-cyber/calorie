package com.example.demo;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.Serializable;
import java.util.HashMap;


abstract class NamedItem implements Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected String name;

    public NamedItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public abstract String getItemType();

    @Override
    public String toString() {
        return getItemType() + "[" + id + "]: " + name;
    }
}


class BaseFood extends NamedItem {
    protected int calories;

    public BaseFood(int id, String name, int calories) {
        super(id, name);
        this.calories = calories;
    }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    @Override
    public String getItemType() { return "Food"; }
}

class CategorizedFood extends BaseFood {
    protected String category;

    public CategorizedFood(int id, String name, int calories, String category) {
        super(id, name, calories);
        this.category = category;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String getItemType() { return "CategorizedFood[" + category + "]"; }
}

class FoodItem extends CategorizedFood {
    private double protein;
    private double carbs;
    private double fat;

    public FoodItem(int id, String name, int calories, String category,
                    double protein, double carbs, double fat) {
        super(id, name, calories, category);
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public FoodItem(int id, String name, int calories, String category) {
        this(id, name, calories, category, 0, 0, 0);
    }

    public double getProtein() { return protein; }
    public double getCarbs()   { return carbs; }
    public double getFat()     { return fat; }
    public void setProtein(double p) { this.protein = p; }
    public void setCarbs(double c)   { this.carbs = c; }
    public void setFat(double f)     { this.fat = f; }

    @Override
    public String getItemType() { return "FoodItem"; }

    
    public String toPresetString() {
        return name + "|" + calories;
    }
}

class DrinkItem extends FoodItem {
    private int volumeMl;

    public DrinkItem(int id, String name, int calories, String category, 
                     double protein, double carbs, double fat, int volumeMl) {
        super(id, name, calories, category, protein, carbs, fat);
        this.volumeMl = volumeMl;
    }

    public int getVolumeMl() { return volumeMl; }
    public void setVolumeMl(int ml) { this.volumeMl = ml; }

    @Override
    public String getItemType() { return "DrinkItem (" + volumeMl + "ml)"; }
}


class BaseWorkout extends NamedItem {
    protected String level;
    protected int points;

    public BaseWorkout(int id, String name, String level, int points) {
        super(id, name);
        this.level = level;
        this.points = points;
    }

    public String getLevel()  { return level; }
    public int    getPoints() { return points; }
    public void   setLevel(String level)   { this.level = level; }
    public void   setPoints(int points)    { this.points = points; }

    @Override
    public String getItemType() { return "Workout"; }
}

class WorkoutPlan extends BaseWorkout {
    protected String description;
    protected List<String> exercises;

    public WorkoutPlan(int id, String name, String level, int points,
                    String description, List<String> exercises) {
        super(id, name, level, points);
        this.description = description;
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }

    public String       getDescription() { return description; }
    public List<String> getExercises()   { return exercises; }
    public void setDescription(String d) { this.description = d; }
    public void setExercises(List<String> e) { this.exercises = e; }


    public String getTitle() { return name; }
    public void   setTitle(String title) { this.name = title; }

    @Override
    public String getItemType() { return "WorkoutPlan"; }
}


class ManagedWorkoutPlan extends WorkoutPlan {
    private int     durationMinutes;
    private boolean completed;
    private String  key;

    public ManagedWorkoutPlan(int id, String name, String level, int points,
                            String description, List<String> exercises,
                            int durationMinutes) {
        super(id, name, level, points, description, exercises);
        this.durationMinutes = durationMinutes;
        this.completed = false;
        this.key = String.valueOf(id); // ใช้ ID เป็น Key เพื่อให้แยกจากกันแม้ Level จะซ้ำ
    }

    public int     getDurationMinutes() { return durationMinutes; }
    public boolean isCompleted()        { return completed; }
    public String  getKey()             { return key; }
    public void    setDurationMinutes(int d) { this.durationMinutes = d; }
    public void    setCompleted(boolean c)   { this.completed = c; }
    public void    setKey(String key)        { this.key = key; }

    @Override
    public String getItemType() { return "ManagedWorkoutPlan"; }
}


class RegisteredUser extends NamedItem {
    protected int points;
    protected String password;
    protected String profileImage;

    public RegisteredUser(int id, String username) {
        super(id, username);
        this.points = 0;
    }

    public String getUsername() { return name; }
    public int    getPoints()   { return points; }
    public String getPassword() { return password; }
    public String getProfileImage() { return profileImage; }
    public void   setPoints(int p)   { this.points = p; }
    public void   setPassword(String p) { this.password = p; }
    public void   setProfileImage(String img) { this.profileImage = img; }
    public void   addPoints(int p)   { this.points += p; }

   
    public String getMotivationMessage() {
        String[] titles = { 
            "keep going!", "You're great!", "Doing amazing!", "Superstar status!"
            , "Legendary effort!", "Ultimate Master!"
        };
        int idx = Math.min(points / 100, titles.length - 1);
        return titles[idx];
    }

    @Override
    public String getItemType() { return "User"; }
}

class HealthUser extends RegisteredUser {
    protected double weight;
    protected double height;
    protected int    targetKcal;
    protected double goalWeight;
    protected String bmiStatus; 

    public HealthUser(int id, String username) {
        super(id, username);
        this.weight = 0;
        this.height = 0;
        this.targetKcal = 2000;
        this.goalWeight = 0;
        this.bmiStatus = null;
    }

    public double getWeight()     { return weight; }
    public double getHeight()     { return height; }
    public int    getTargetKcal() { return targetKcal; }
    public double getGoalWeight() { return goalWeight; }
    public String getBmiStatus()  { return bmiStatus; }

    public void setWeight(double w)      { this.weight = w; }
    public void setHeight(double h)      { this.height = h; }
    public void setGoalWeight(double g)  { this.goalWeight = g; }
    public void setTargetKcal(int k)     { this.targetKcal = k; }
    public void setBmiStatus(String s)   { this.bmiStatus = s; }

    public double getBmi() {
        if (height <= 0 || weight <= 0) return 0;
        double heightMeters = height / 100.0;
        return weight / (heightMeters * heightMeters);
    }

    @Override
    public String getItemType() { return "HealthUser"; }
}


class UserRecord extends HealthUser {
    private int totalEaten;
    private int breakfastTotal, lunchTotal, dinnerTotal;
    private List<String> breakfastItems = new ArrayList<>();
    private List<String> lunchItems = new ArrayList<>();
    private List<String> dinnerItems = new ArrayList<>();
    private List<String> weightHistory = new ArrayList<>();
    private Map<String, Boolean> claimedWorkouts = new HashMap<>();

    public UserRecord(int id, String username) {
        super(id, username);
        this.totalEaten = 0;
    }

    public int  getTotalEaten() { return totalEaten; }
    public void setTotalEaten(int t) { this.totalEaten = t; }

    public int getBreakfastTotal() { return breakfastTotal; }
    public int getLunchTotal() { return lunchTotal; }
    public int getDinnerTotal() { return dinnerTotal; }
    public List<String> getBreakfastItems() { return breakfastItems; }
    public List<String> getLunchItems() { return lunchItems; }
    public List<String> getDinnerItems() { return dinnerItems; }
    public List<String> getWeightHistory() { return weightHistory; }
    public Map<String, Boolean> getClaimedWorkouts() { return claimedWorkouts; }

    public void syncFromSession(String username, String password, String profileImage, Integer targetKcal, Double weight, Double goalWeight,
                                Double height, String bmiStatus, Integer points, Integer totalEaten,
                                Integer bTotal, Integer lTotal, Integer dTotal,
                                List<String> bItems, List<String> lItems, List<String> dItems,
                                List<String> history, Map<String, Boolean> workouts) {
        this.name = username;
        if (password   != null) this.password   = password;
        if (profileImage != null) this.profileImage = profileImage;
        if (targetKcal != null) this.targetKcal = targetKcal;
        if (weight     != null) this.weight     = weight;
        if (goalWeight != null) this.goalWeight = goalWeight;
        if (height     != null) this.height     = height;
        if (bmiStatus  != null) this.bmiStatus  = bmiStatus;
        if (points     != null) this.points     = points;
        if (totalEaten != null) this.totalEaten = totalEaten;
        
        if (bTotal != null) this.breakfastTotal = bTotal;
        if (lTotal != null) this.lunchTotal = lTotal;
        if (dTotal != null) this.dinnerTotal = dTotal;
        if (bItems != null) this.breakfastItems = new ArrayList<>(bItems);
        if (lItems != null) this.lunchItems = new ArrayList<>(lItems);
        if (dItems != null) this.dinnerItems = new ArrayList<>(dItems);
        if (history != null) this.weightHistory = new ArrayList<>(history);
        if (workouts != null) this.claimedWorkouts = new HashMap<>(workouts);
    }

    @Override
    public String getItemType() { return "UserRecord"; }
}

class PremiumUserRecord extends UserRecord {
    private double pointsMultiplier;

    public PremiumUserRecord(int id, String username, double multiplier) {
        super(id, username);
        this.pointsMultiplier = multiplier;
    }

    @Override
    public void addPoints(int p) {
        super.addPoints((int) (p * pointsMultiplier));
    }

    @Override
    public String getMotivationMessage() {
        return "🌟 VIP: " + super.getMotivationMessage();
    }
}