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
import java.util.List;

public class OpenAiClient {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public static String fetchReply(String apiKey, String model, List<Message> messages) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("temperature", 0.7);

            JSONArray messageArray = new JSONArray();
            for (Message message : messages) {
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
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readAll(stream);

            if (code < 200 || code >= 300) {
                String detail = response;
                try {
                    JSONObject errorJson = new JSONObject(response);
                    detail = errorJson.optJSONObject("error") != null
                            ? errorJson.optJSONObject("error").optString("message", response)
                            : response;
                } catch (Exception ignored) {
                }
                throw new IllegalStateException("OpenAI API error " + code + ": " + detail);
            }

            JSONObject json = new JSONObject(response);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                throw new IllegalStateException("OpenAI API returned no choices.");
            }
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.optJSONObject("message");
            if (message == null) {
                throw new IllegalStateException("OpenAI API returned invalid message payload.");
            }
            String content = message.optString("content", "").trim();
            if (content.isEmpty()) {
                throw new IllegalStateException("OpenAI API returned an empty reply.");
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

    public static class Message {
        public final String role;
        public final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
