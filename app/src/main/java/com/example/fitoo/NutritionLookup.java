package com.example.fitoo;

/**
 * USDA-style nutrition estimates. Use count for items like eggs (e.g. 6),
 * weight in grams for items like chicken (e.g. 250g).
 */
public class NutritionLookup {

    /** @param quantity count (e.g. 6 eggs) or grams (e.g. 250) when isWeightGrams is true */
    public static MacroInfo estimate(String name, float quantity, boolean isWeightGrams) {
        if (name == null) name = "";
        String n = name.toLowerCase().trim();
        if (n.isEmpty()) return null;

        MacroInfo info = new MacroInfo();

        // --- Count-based (per piece / per item) ---
        if (!isWeightGrams) {
            // Egg (1 large ~50g): USDA
            if (n.contains("egg")) {
                info.calories = 78 * quantity;
                info.protein = 6.3f * quantity;
                info.carbs = 0.6f * quantity;
                info.fats = 5.3f * quantity;
                info.fiber = 0f;
                return info;
            }
            // Bread (1 slice ~28g)
            if (n.contains("bread") || n.contains("toast")) {
                info.calories = 79 * quantity;
                info.protein = 2.6f * quantity;
                info.carbs = 15 * quantity;
                info.fats = 1 * quantity;
                info.fiber = 1.1f * quantity;
                return info;
            }
            // Banana (1 medium ~118g)
            if (n.contains("banana")) {
                info.calories = 105 * quantity;
                info.protein = 1.3f * quantity;
                info.carbs = 27 * quantity;
                info.fats = 0.4f * quantity;
                info.fiber = 3.1f * quantity;
                return info;
            }
            // Apple (1 medium ~182g)
            if (n.contains("apple")) {
                info.calories = 95 * quantity;
                info.protein = 0.5f * quantity;
                info.carbs = 25 * quantity;
                info.fats = 0.3f * quantity;
                info.fiber = 4.4f * quantity;
                return info;
            }
            // Orange
            if (n.contains("orange")) {
                info.calories = 62 * quantity;
                info.protein = 1.2f * quantity;
                info.carbs = 15 * quantity;
                info.fats = 0.2f * quantity;
                info.fiber = 3.1f * quantity;
                return info;
            }
        }

        // --- Weight-based (per 100g) ---
        float factor = isWeightGrams ? (quantity / 100f) : quantity;

        if (n.contains("chicken") || n.contains("poultry")) {
            // Chicken breast, cooked (USDA per 100g)
            info.calories = 165 * factor;
            info.protein = 31 * factor;
            info.carbs = 0;
            info.fats = 3.6f * factor;
            info.fiber = 0f;
            return info;
        }
        if (n.contains("rice")) {
            // White rice cooked
            info.calories = 130 * factor;
            info.protein = 2.7f * factor;
            info.carbs = 28 * factor;
            info.fats = 0.3f * factor;
            info.fiber = 0.4f * factor;
            return info;
        }
        if (n.contains("oat")) {
            // Oats dry
            info.calories = 389 * factor;
            info.protein = 16.9f * factor;
            info.carbs = 66 * factor;
            info.fats = 6.9f * factor;
            info.fiber = 10.6f * factor;
            return info;
        }
        if (n.contains("beef") || n.contains("steak") || n.contains("mince")) {
            info.calories = 250 * factor;
            info.protein = 26 * factor;
            info.carbs = 0;
            info.fats = 15 * factor;
            info.fiber = 0f;
            return info;
        }
        if (n.contains("fish") || n.contains("salmon") || n.contains("tilapia") || n.contains("tuna")) {
            info.calories = 208 * factor; // salmon approx
            info.protein = 20 * factor;
            info.carbs = 0;
            info.fats = 13 * factor;
            info.fiber = 0f;
            return info;
        }
        if (n.contains("milk")) {
            info.calories = 61 * factor;
            info.protein = 3.2f * factor;
            info.carbs = 4.8f * factor;
            info.fats = 3.3f * factor;
            info.fiber = 0f;
            return info;
        }
        if (n.contains("potato") || n.contains("potatoes")) {
            info.calories = 87 * factor;
            info.protein = 1.9f * factor;
            info.carbs = 20 * factor;
            info.fats = 0.1f * factor;
            info.fiber = 2.2f * factor;
            return info;
        }
        if (n.contains("broccoli") || n.contains("vegetable")) {
            info.calories = 35 * factor;
            info.protein = 2.4f * factor;
            info.carbs = 7 * factor;
            info.fats = 0.4f * factor;
            info.fiber = 2.6f * factor;
            return info;
        }
        if (n.contains("pasta") || n.contains("noodle")) {
            info.calories = 131 * factor;
            info.protein = 5 * factor;
            info.carbs = 25 * factor;
            info.fats = 1.1f * factor;
            info.fiber = 1.8f * factor;
            return info;
        }
        if (n.contains("cheese")) {
            info.calories = 403 * factor;
            info.protein = 25 * factor;
            info.carbs = 1.3f * factor;
            info.fats = 33 * factor;
            info.fiber = 0f;
            return info;
        }
        if (n.contains("egg") && isWeightGrams) {
            // 100g whole egg ~155 cal
            info.calories = 155 * factor;
            info.protein = 13 * factor;
            info.carbs = 1.1f * factor;
            info.fats = 11 * factor;
            info.fiber = 0f;
            return info;
        }

        // Unknown food: return null so caller can show error and reject
        return null;
    }

    public static class MacroInfo {
        public float calories;
        public float protein;
        public float carbs;
        public float fats;
        public float fiber;
    }
}
