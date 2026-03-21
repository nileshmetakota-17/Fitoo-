package com.example.fitoo;

public final class AiPreferences {

    private AiPreferences() {
    }

    public static final String PREFS_NAME = "fitoo_prefs";
    public static final String KEY_OPENAI_API_KEY = "openai_api_key";
    public static final String KEY_OPENAI_MODEL = "openai_model";
    public static final String KEY_NO_KEY_MODEL = "no_key_model";
    public static final String KEY_SENSEI_CHAT_HISTORY = "sensei_chat_history";
    public static final String DEFAULT_MODEL = "gpt-4.1-mini";
    public static final String DEFAULT_NO_KEY_MODEL = "openai-fast";
}
