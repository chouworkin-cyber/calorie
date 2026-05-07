package com.example.demo;

import java.util.List;
import java.util.ArrayList;


abstract class NamedItem {
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
        this.key = level;
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

    public RegisteredUser(int id, String username) {
        super(id, username);
        this.points = 0;
    }

    public String getUsername() { return name; }
    public int    getPoints()   { return points; }
    public void   setPoints(int p)   { this.points = p; }
    public void   addPoints(int p)   { this.points += p; }

   
    public String getMotivationMessage() {
        String[] levels = { 
            "Keep going!", "You're great!", "Doing amazing!", 
            "Superstar status!", "Legendary effort!", "Ultimate Master!" 
        };
        int idx = Math.min(points / 100, levels.length - 1);
        return levels[idx];
    }

    @Override
    public String getItemType() { return "User"; }
}

class HealthUser extends RegisteredUser {
    protected double weight;
    protected double height;
    protected int    targetKcal;
    protected String bmiStatus; 

    public HealthUser(int id, String username) {
        super(id, username);
        this.weight = 0;
        this.height = 0;
        this.targetKcal = 2000;
        this.bmiStatus = null;
    }

    public double getWeight()     { return weight; }
    public double getHeight()     { return height; }
    public int    getTargetKcal() { return targetKcal; }
    public String getBmiStatus()  { return bmiStatus; }

    public void setWeight(double w)      { this.weight = w; }
    public void setHeight(double h)      { this.height = h; }
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

    public UserRecord(int id, String username) {
        super(id, username);
        this.totalEaten = 0;
    }

    public int  getTotalEaten() { return totalEaten; }
    public void setTotalEaten(int t) { this.totalEaten = t; }

    public void syncFromSession(String username, Integer targetKcal, Double weight,
                                Double height, String bmiStatus, Integer points, Integer totalEaten) {
        this.name = username;
        if (targetKcal != null) this.targetKcal = targetKcal;
        if (weight     != null) this.weight     = weight;
        if (height     != null) this.height     = height;
        if (bmiStatus  != null) this.bmiStatus  = bmiStatus;
        if (points     != null) this.points     = points;
        if (totalEaten != null) this.totalEaten = totalEaten;
    }

    @Override
    public String getItemType() { return "UserRecord"; }
}