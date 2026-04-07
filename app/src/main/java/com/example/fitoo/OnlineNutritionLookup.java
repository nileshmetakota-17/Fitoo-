package com.example.fitoo;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Online nutrition lookup via Open Food Facts (global product DB, many Indian items).
 * Fetches several search hits, scores by name match to your query, and accepts kcal or kJ.
 */
public final class OnlineNutritionLookup {

    private static final int CACHE_MAX = 64;
    private static final int SEARCH_PAGE_SIZE = 24;
    private static final java.util.LinkedHashMap<String, Per100g> CACHE =
            new java.util.LinkedHashMap<String, Per100g>(CACHE_MAX, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Per100g> eldest) {
                    return size() > CACHE_MAX;
                }
            };

    private static final class Per100g {
        float calories;
        float protein;
        float carbs;
        float fats;
        float fiber;
    }

    private OnlineNutritionLookup() {
    }

    @SuppressWarnings("SameParameterValue")
    public static NutritionLookup.MacroInfo lookupPerGrams(Context context, String query, float grams) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }
        if (query == null) {
            return null;
        }
        String q = query.trim();
        if (q.isEmpty() || grams <= 0f) {
            return null;
        }

        String cacheKey = q.toLowerCase(Locale.ROOT);
        synchronized (CACHE) {
            Per100g cached = CACHE.get(cacheKey);
            if (cached != null) {
                return scale(cached, grams);
            }
        }

        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_simple=1&action=process&json=1"
                + "&page_size=" + SEARCH_PAGE_SIZE
                + "&search_terms=" + encoded;

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");
            String appLabel = context.getApplicationContext().getString(R.string.app_name);
            connection.setRequestProperty("User-Agent", appLabel + "/1.0 (Android nutrition lookup)");

            int code = connection.getResponseCode();
            String response = readAll(code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Nutrition lookup failed: HTTP " + code);
            }

            JSONObject json = new JSONObject(response);
            JSONArray products = json.optJSONArray("products");
            if (products == null || products.length() == 0) {
                return null;
            }

            List<Scored> candidates = new ArrayList<>();
            for (int i = 0; i < products.length(); i++) {
                JSONObject product = products.optJSONObject(i);
                if (product == null) {
                    continue;
                }
                JSONObject nutriments = product.optJSONObject("nutriments");
                Per100g per = nutrimentsToPer100g(nutriments);
                if (per == null) {
                    continue;
                }
                String displayName = (product.optString("product_name", "")
                        + " " + product.optString("generic_name", "")
                        + " " + product.optString("brands", "")).trim();
                StringBuilder catJoin = new StringBuilder(product.optString("categories", ""));
                JSONArray catTags = product.optJSONArray("categories_tags");
                if (catTags != null) {
                    for (int t = 0; t < catTags.length(); t++) {
                        catJoin.append(' ').append(catTags.optString(t));
                    }
                }
                int score = nameMatchScore(q, displayName, catJoin.toString());
                candidates.add(new Scored(score, per));
            }
            if (candidates.isEmpty()) {
                return null;
            }
            Collections.sort(candidates, Comparator.comparingInt((Scored s) -> s.score).reversed());
            Per100g best = candidates.get(0).per100g;
            synchronized (CACHE) {
                CACHE.put(cacheKey, best);
            }
            return scale(best, grams);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static final class Scored {
        final int score;
        final Per100g per100g;

        Scored(int score, Per100g per100g) {
            this.score = score;
            this.per100g = per100g;
        }
    }

    private static final String[] JUNK_PRODUCT_HINTS = {
            "cake", "cookie", "candy", "dessert", "ice cream", "chocolate", "brownie",
            "muffin", "pastry", "snack", "cereal", "syrup", "jam", "spread", "juice drink",
            "instant", "fried", "chips", "cracker"
    };

    private static final String[] PRODUCE_QUERIES = {
            "carrot", "tomato", "onion", "potato", "lettuce", "spinach", "cucumber",
            "apple", "banana", "orange", "broccoli", "cabbage", "pepper", "garlic", "ginger"
    };

    /**
     * Higher = better match. Penalizes packaged desserts/snacks when the user typed a simple ingredient
     * (e.g. "carrot" vs "carrot cake").
     */
    static int nameMatchScore(String query, String productName, String categoriesBlob) {
        if (productName == null || productName.isEmpty()) {
            return 0;
        }
        String q = query.toLowerCase(Locale.ROOT).trim();
        String p = productName.toLowerCase(Locale.ROOT);
        String cats = categoriesBlob == null ? "" : categoriesBlob.toLowerCase(Locale.ROOT);
        String blob = p + " " + cats;

        int score = 0;
        if (p.contains(q)) {
            score += 80;
        }
        String[] words = q.split("\\s+");
        for (String w : words) {
            if (w.length() >= 2 && p.contains(w)) {
                score += 25;
            }
        }
        if (words.length == 1 && p.startsWith(q)) {
            score += 45;
        }

        if (isSimpleProduceStyleQuery(q)) {
            for (String junk : JUNK_PRODUCT_HINTS) {
                if (blob.contains(junk)) {
                    score -= 130;
                }
            }
            if (looksLikeProduceQuery(q)) {
                if (cats.contains("vegetable") || cats.contains("fruits") || cats.contains("fresh")
                        || cats.contains("plant")) {
                    score += 55;
                }
            }
        }

        return score;
    }

    private static boolean isSimpleProduceStyleQuery(String qLower) {
        return qLower.split("\\s+").length <= 3;
    }

    private static boolean looksLikeProduceQuery(String qLower) {
        for (String produce : PRODUCE_QUERIES) {
            if (qLower.equals(produce) || qLower.startsWith(produce + " ")) {
                return true;
            }
        }
        return false;
    }

    private static Per100g nutrimentsToPer100g(JSONObject nutriments) {
        if (nutriments == null) {
            return null;
        }
        float kcalPer100g = readFloat(nutriments, "energy-kcal_100g", Float.NaN);
        if (Float.isNaN(kcalPer100g) || kcalPer100g <= 0f) {
            float kj = readFloat(nutriments, "energy-kj_100g", Float.NaN);
            if (!Float.isNaN(kj) && kj > 0f) {
                kcalPer100g = kj / 4.184f;
            }
        }
        if (Float.isNaN(kcalPer100g) || kcalPer100g <= 0f) {
            return null;
        }
        Per100g per = new Per100g();
        per.calories = kcalPer100g;
        per.protein = Math.max(0f, readFloat(nutriments, "proteins_100g", 0f));
        per.carbs = Math.max(0f, readFloat(nutriments, "carbohydrates_100g", 0f));
        per.fats = Math.max(0f, readFloat(nutriments, "fat_100g", 0f));
        per.fiber = Math.max(0f, readFloat(nutriments, "fiber_100g", 0f));
        return per;
    }

    private static NutritionLookup.MacroInfo scale(Per100g per100g, float grams) {
        float factor = grams / 100f;
        NutritionLookup.MacroInfo info = new NutritionLookup.MacroInfo();
        info.calories = per100g.calories * factor;
        info.protein = per100g.protein * factor;
        info.carbs = per100g.carbs * factor;
        info.fats = per100g.fats * factor;
        info.fiber = per100g.fiber * factor;
        return info;
    }

    private static float readFloat(JSONObject obj, String key, float fallback) {
        if (obj == null) {
            return fallback;
        }
        Object raw = obj.opt(key);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number) {
            return ((Number) raw).floatValue();
        }
        try {
            String s = String.valueOf(raw).trim();
            if (s.isEmpty()) {
                return fallback;
            }
            return Float.parseFloat(s);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
