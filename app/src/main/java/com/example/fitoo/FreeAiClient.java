package com.example.fitoo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FreeAiClient {

    private static final String MODELS_ENDPOINT = "https://text.pollinations.ai/openai/models";
    private static final String CHAT_ENDPOINT = "https://text.pollinations.ai/openai/chat/completions";

    private FreeAiClient() {
    }

    public static List<String> fetchModels() throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(MODELS_ENDPOINT).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            int code = connection.getResponseCode();
            String response = readAll(code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Model list request failed: " + response);
            }

            JSONObject json = new JSONObject(response);
            JSONArray data = json.optJSONArray("data");
            List<String> models = new ArrayList<>();
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String id = item.optString("id", "").trim();
                    if (!id.isEmpty()) {
                        models.add(id);
                    }
                }
            }
            if (models.isEmpty()) {
                models.add(AiPreferences.DEFAULT_NO_KEY_MODEL);
            }
            return models;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String fetchReply(String model, List<OpenAiClient.Message> messages) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(CHAT_ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(45000);
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("model", model);

            JSONArray messageArray = new JSONArray();
            for (OpenAiClient.Message message : messages) {
                JSONObject item = new JSONObject();
                item.put("role", message.role);
                item.put("content", message.content);
                messageArray.put(item);
            }
            body.put("messages", messageArray);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload);
            }

            int code = connection.getResponseCode();
            String response = readAll(code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());

            JSONObject json = new JSONObject(response);
            if (json.has("error")) {
                throw new IllegalStateException(json.optString("error", "Unknown AI error."));
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("AI request failed: " + response);
            }

            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw new IllegalStateException("AI service returned no choices.");
            }
            JSONObject choice = choices.optJSONObject(0);
            JSONObject message = choice != null ? choice.optJSONObject("message") : null;
            String content = message != null ? message.optString("content", "").trim() : "";
            if (content.isEmpty()) {
                throw new IllegalStateException("AI service returned an empty reply.");
            }
            return content;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
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
