
package com.example.demo;

import java.util.HashMap;
import java.util.Map;

class UserProfile {
    protected double weight, height;
    protected int age;
    protected String gender;
    public UserProfile(double w, double h, int a, String g) {
        this.weight = w; this.height = h; this.age = a; this.gender = g;
    }
}


class PhysicalMetrics extends UserProfile {
    public PhysicalMetrics(double w, double h, int a, String g) { super(w, h, a, g); }
    public double getBMI() { return weight / Math.pow(height / 100, 2); }
}

class MetabolicSystem extends PhysicalMetrics {
    public MetabolicSystem(double w, double h, int a, String g) { super(w, h, a, g); }
    public double getBMR() {
        return "male".equalsIgnoreCase(gender) 
            ? 66 + (13.7 * weight) + (5 * height) - (6.8 * age)
            : 655 + (9.6 * weight) + (1.8 * height) - (4.7 * age);
    }
}

class EnergyCalculator extends MetabolicSystem {
    protected double multiplier;
    public EnergyCalculator(double w, double h, int a, String g, double m) {
        super(w, h, a, g); this.multiplier = m;
    }
    public double getTDEE() { return getBMR() * multiplier; }
}

class HealthSolution extends EnergyCalculator {
    public HealthSolution(double w, double h, int a, String g, double m) {
        super(w, h, a, g, m);
    }
    public String getStatus() {
        double bmi = getBMI();
        if (bmi < 18.5) return "lower";
        if (bmi < 23.0) return "normal";
        return "overweight";
        
    }
}

class DietaryRecommendation extends HealthSolution {
    public DietaryRecommendation(double w, double h, int a, String g, double m) {
        super(w, h, a, g, m);
    }

    public Map<String, Double> getRecommendedMacros() {
        double tdee = getTDEE();
        Map<String, Double> macros = new HashMap<>();
        // สูตรมาตรฐาน: Protein 30%, Carbs 40%, Fat 30%
        macros.put("Protein(g)", (tdee * 0.30) / 4);
        macros.put("Carbs(g)", (tdee * 0.40) / 4);
        macros.put("Fat(g)", (tdee * 0.30) / 9);
        return macros;
    }
}
