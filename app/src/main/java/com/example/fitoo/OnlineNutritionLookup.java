package com.example.fitoo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Online nutrition lookup for unknown foods.
 *
 * Uses Open Food Facts public search API and returns per-100g nutriments scaled to the requested grams.
 * This requires INTERNET permission and should be called off the main thread.
 */
public final class OnlineNutritionLookup {

    private static final int CACHE_MAX = 64;
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
    public static NutritionLookup.MacroInfo lookupPerGrams(String query, float grams) throws Exception {
        if (query == null) {
            return null;
        }
        String q = query.trim();
        if (q.isEmpty() || grams <= 0f) {
            return null;
        }

        String cacheKey = q.toLowerCase();
        synchronized (CACHE) {
            Per100g cached = CACHE.get(cacheKey);
            if (cached != null) {
                return scale(cached, grams);
            }
        }

        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
        String url = "https://world.openfoodfacts.org/cgi/search.pl?search_simple=1&action=process&json=1&page_size=1&search_terms=" + encoded;

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/json");

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
            JSONObject product = products.optJSONObject(0);
            if (product == null) {
                return null;
            }
            JSONObject nutriments = product.optJSONObject("nutriments");
            if (nutriments == null) {
                return null;
            }

            float kcalPer100g = readFloat(nutriments, "energy-kcal_100g", Float.NaN);
            if (Float.isNaN(kcalPer100g)) {
                // Some products only have kJ; we keep it simple and require kcal to avoid wrong conversions.
                return null;
            }

            Per100g per = new Per100g();
            per.calories = kcalPer100g;
            per.protein = readFloat(nutriments, "proteins_100g", 0f);
            per.carbs = readFloat(nutriments, "carbohydrates_100g", 0f);
            per.fats = readFloat(nutriments, "fat_100g", 0f);
            per.fiber = readFloat(nutriments, "fiber_100g", 0f);
            synchronized (CACHE) {
                CACHE.put(cacheKey, per);
            }
            return scale(per, grams);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

