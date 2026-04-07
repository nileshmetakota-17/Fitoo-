package com.example.fitoo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SenseiFragment extends Fragment {

    private static final int MAX_MESSAGES = 16;
    private static final int MAX_SESSIONS = 30;
    private static final long MIN_REPLY_DELAY_MS = 700L;
    private static final String WELCOME_TEXT =
            "Ask me about workouts, nutrition, diet, or fitness. Live AI works without API key here.";
    private static final String KEY_CHAT_SESSIONS = "sensei_chat_sessions_v2";
    private static final String KEY_ACTIVE_CHAT_ID = "sensei_active_chat_id_v2";

    private LinearLayout senseiMessages;
    private EditText senseiInput;
    private NestedScrollView senseiScroll;
    private Button sendBtn;
    private Button btnHistory;
    private Button btnNewChat;
    private Button btnClearHistory;
    private Spinner modelSpinner;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<OpenAiClient.Message> conversation = new ArrayList<>();
    private final List<String> noKeyModels = new ArrayList<>();
    private final List<ChatSession> chatSessions = new ArrayList<>();
    private volatile boolean awaitingReply;
    private String activeChatId;

    // Undo snapshots for Sensei-applied changes
    private List<DietPlanItem> lastDietPlanSnapshot;
    private List<WorkoutLogEntry> lastWorkoutSnapshot;
    private Long lastWorkoutSnapshotDay;
    private final List<Integer> lastInsertedMealIds = new ArrayList<>();

    private static class ChatSession {
        String id;
        String title;
        long updatedAt;
        final List<OpenAiClient.Message> messages = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sensei, container, false);

        senseiMessages = v.findViewById(R.id.senseiMessages);
        senseiInput = v.findViewById(R.id.senseiInput);
        senseiScroll = v.findViewById(R.id.senseiScroll);
        sendBtn = v.findViewById(R.id.senseiSend);
        btnHistory = v.findViewById(R.id.btnSenseiHistory);
        btnNewChat = v.findViewById(R.id.btnSenseiNewChat);
        btnClearHistory = v.findViewById(R.id.btnSenseiClearHistory);
        modelSpinner = v.findViewById(R.id.senseiModelSpinner);

        setupImeAwareComposer(v);
        initializeConversation();
        initializeModelPicker();
        loadNoKeyModels();
        loadChatHistory();

        sendBtn.setOnClickListener(view -> sendMessage());
        senseiInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage();
                return true;
            }
            return false;
        });
        btnHistory.setOnClickListener(view -> showHistoryDialog());
        btnNewChat.setOnClickListener(view -> {
            persistConversation();
            startNewChat(true);
        });
        btnClearHistory.setOnClickListener(view -> confirmDeleteHistory());

        return v;
    }

    private void setupImeAwareComposer(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            if (senseiScroll != null) {
                senseiScroll.post(this::scrollToBottom);
            }
            return insets;
        });
        if (senseiInput != null) {
            senseiInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && senseiScroll != null) {
                    senseiScroll.post(this::scrollToBottom);
                }
            });
        }
        ViewCompat.requestApplyInsets(root);
    }

    private int dpToPx(int dp) {
        Context context = getContext();
        if (context == null) {
            return dp;
        }
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        awaitingReply = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void initializeConversation() {
        conversation.clear();
        conversation.add(new OpenAiClient.Message(
                "system",
                "You are Sensei, a helpful fitness coach inside an Android app. " +
                        "Give practical workout and nutrition guidance. " +
                        "Keep replies concise and actionable. " +
                        "Do not use markdown tables."
        ));
    }

    private void sendMessage() {
        if (awaitingReply) {
            return;
        }
        String text = senseiInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        senseiInput.setText("");
        appendMessage("You", text);
        conversation.add(new OpenAiClient.Message("user", text));
        trimConversation();
        persistConversation();

        Context context = getContext();
        if (context == null) {
            appendMessage("Sensei", "Context unavailable. Please try again.");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        String apiKey = prefs.getString(AiPreferences.KEY_OPENAI_API_KEY, "");
        String openAiModel = prefs.getString(AiPreferences.KEY_OPENAI_MODEL, AiPreferences.DEFAULT_MODEL);
        String noKeyModel = prefs.getString(AiPreferences.KEY_NO_KEY_MODEL, AiPreferences.DEFAULT_NO_KEY_MODEL);

        String safeOpenAiModel = (openAiModel == null || openAiModel.trim().isEmpty())
                ? AiPreferences.DEFAULT_MODEL
                : openAiModel.trim();
        String safeNoKeyModel = resolveNoKeyModel(noKeyModel);
        boolean hasApiKey = apiKey != null && !apiKey.trim().isEmpty();

        awaitingReply = true;
        setChatControlsEnabled(false);

        long requestStart = SystemClock.elapsedRealtime();
        executor.execute(() -> {
            String reply;
            try {
                if (hasApiKey) {
                    reply = OpenAiClient.fetchReply(apiKey.trim(), safeOpenAiModel, new ArrayList<>(conversation));
                } else {
                    reply = FreeAiClient.fetchReply(safeNoKeyModel, new ArrayList<>(conversation));
                }
            } catch (Exception e) {
                if (hasApiKey) {
                    reply = "AI request failed: " + e.getMessage();
                } else {
                    reply = "Live AI unavailable right now. " + getNoKeyCoachReply(text);
                }
            }

            long elapsed = SystemClock.elapsedRealtime() - requestStart;
            if (elapsed < MIN_REPLY_DELAY_MS) {
                SystemClock.sleep(MIN_REPLY_DELAY_MS - elapsed);
            }

            final String formattedReply = formatReplyForDisplay(reply);
            conversation.add(new OpenAiClient.Message("assistant", formattedReply));
            trimConversation();
            persistConversation();

            runOnUiThreadSafe(() -> {
                appendMessage("Sensei", formattedReply);
                awaitingReply = false;
                setChatControlsEnabled(true);
                senseiInput.requestFocus();
            });
        });
    }

    private String formatReplyForDisplay(String rawReply) {
        if (rawReply == null) {
            return "";
        }
        String text = rawReply.replace("\r\n", "\n").replace('\t', ' ').trim();
        if (!text.contains("|")) {
            return text;
        }

        String[] lines = text.split("\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                String middle = trimmed.substring(1, trimmed.length() - 1).trim();
                if (middle.replace("-", "").replace(":", "").trim().isEmpty()) {
                    continue;
                }
                String[] cells = middle.split("\\|");
                StringBuilder rowBuilder = new StringBuilder();
                for (String cell : cells) {
                    String part = cell.trim();
                    if (part.isEmpty()) {
                        continue;
                    }
                    if (rowBuilder.length() > 0) {
                        rowBuilder.append(" | ");
                    }
                    rowBuilder.append(part);
                }
                if (rowBuilder.length() > 0) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(rowBuilder);
                }
            } else {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString().trim();
    }

    private void setChatControlsEnabled(boolean enabled) {
        sendBtn.setEnabled(enabled);
        senseiInput.setEnabled(enabled);
        modelSpinner.setEnabled(enabled);
        btnHistory.setEnabled(enabled);
        btnNewChat.setEnabled(enabled);
        btnClearHistory.setEnabled(enabled);
    }

    private void initializeModelPicker() {
        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Collections.singletonList("Loading models...")
        );
        loadingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(loadingAdapter);
        modelSpinner.setEnabled(false);
    }

    private void loadNoKeyModels() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE);
        String savedModel = prefs.getString(AiPreferences.KEY_NO_KEY_MODEL, AiPreferences.DEFAULT_NO_KEY_MODEL);

        executor.execute(() -> {
            List<String> fetchedModels;
            try {
                fetchedModels = FreeAiClient.fetchModels();
            } catch (Exception ignored) {
                fetchedModels = new ArrayList<>();
                fetchedModels.add(AiPreferences.DEFAULT_NO_KEY_MODEL);
            }

            final List<String> finalModels = fetchedModels;
            runOnUiThreadSafe(() -> {
                noKeyModels.clear();
                noKeyModels.addAll(finalModels);

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        noKeyModels
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                modelSpinner.setAdapter(adapter);

                int selectedIndex = 0;
                if (savedModel != null) {
                    int i = noKeyModels.indexOf(savedModel);
                    if (i >= 0) {
                        selectedIndex = i;
                    }
                }
                modelSpinner.setSelection(selectedIndex);
                modelSpinner.setEnabled(true);
                modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position < 0 || position >= noKeyModels.size()) {
                            return;
                        }
                        String chosen = noKeyModels.get(position);
                        Context current = getContext();
                        if (current == null) {
                            return;
                        }
                        current.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putString(AiPreferences.KEY_NO_KEY_MODEL, chosen)
                                .apply();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            });
        });
    }

    private String resolveNoKeyModel(@Nullable String configured) {
        String safe = configured == null || configured.trim().isEmpty()
                ? AiPreferences.DEFAULT_NO_KEY_MODEL
                : configured.trim();
        if (noKeyModels.isEmpty()) {
            return safe;
        }
        if (noKeyModels.contains(safe)) {
            return safe;
        }
        return noKeyModels.get(0);
    }

    private void loadChatHistory() {
        Context context = getContext();
        if (context == null) {
            appendMessage("Sensei", WELCOME_TEXT);
            return;
        }

        senseiMessages.removeAllViews();
        initializeConversation();
        loadSessionsFromPrefs(context);

        ChatSession active = getActiveSession();
        if (active == null) {
            active = createNewSession(false);
            saveSessionsToPrefs(context);
        }

        boolean hasAnyMessage = false;
        for (OpenAiClient.Message msg : active.messages) {
            if (!"user".equals(msg.role) && !"assistant".equals(msg.role)) {
                continue;
            }
            if (msg.content == null || msg.content.trim().isEmpty()) {
                continue;
            }
            conversation.add(new OpenAiClient.Message(msg.role, msg.content));
            appendMessage("user".equals(msg.role) ? "You" : "Sensei", msg.content);
            hasAnyMessage = true;
        }

        trimConversation();
        if (!hasAnyMessage) {
            appendMessage("Sensei", WELCOME_TEXT);
        }
    }

    private void startNewChat(boolean notifyUser) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        createNewSession(true);
        senseiMessages.removeAllViews();
        initializeConversation();
        appendMessage("Sensei", WELCOME_TEXT);
        persistConversation();
        if (notifyUser) {
            showToast("Started a new chat.");
        }
    }

    private ChatSession createNewSession(boolean makeActive) {
        ChatSession session = new ChatSession();
        session.id = String.valueOf(System.currentTimeMillis());
        session.updatedAt = System.currentTimeMillis();
        session.title = "New chat";
        chatSessions.add(0, session);

        if (chatSessions.size() > MAX_SESSIONS) {
            while (chatSessions.size() > MAX_SESSIONS) {
                chatSessions.remove(chatSessions.size() - 1);
            }
        }
        if (makeActive) {
            activeChatId = session.id;
        }
        return session;
    }

    private void showHistoryDialog() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        persistConversation();
        loadSessionsFromPrefs(context);

        if (chatSessions.isEmpty()) {
            showToast("No chat history.");
            return;
        }

        CharSequence[] items = new CharSequence[chatSessions.size()];
        int checked = 0;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault());
        for (int i = 0; i < chatSessions.size(); i++) {
            ChatSession session = chatSessions.get(i);
            String title = session.title == null || session.title.trim().isEmpty() ? "New chat" : session.title;
            items[i] = title + "  •  " + sdf.format(new java.util.Date(session.updatedAt));
            if (session.id != null && session.id.equals(activeChatId)) {
                checked = i;
            }
        }

        final int[] selected = {checked};
        new AlertDialog.Builder(context)
                .setTitle("Chat history")
                .setSingleChoiceItems(items, checked, (d, which) -> selected[0] = which)
                .setPositiveButton("Open", (d, w) -> {
                    if (selected[0] >= 0 && selected[0] < chatSessions.size()) {
                        ChatSession session = chatSessions.get(selected[0]);
                        activeChatId = session.id;
                        saveSessionsToPrefs(context);
                        loadChatHistory();
                        showToast("Opened chat.");
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmDeleteHistory() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("Delete chat history?")
                .setMessage("This clears all saved Sensei chats.")
                .setPositiveButton("Delete", (d, w) -> {
                    clearStoredHistory();
                    startNewChat(false);
                    showToast("Chat history deleted.");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearStoredHistory() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        chatSessions.clear();
        activeChatId = null;
        context.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(AiPreferences.KEY_SENSEI_CHAT_HISTORY)
                .remove(KEY_CHAT_SESSIONS)
                .remove(KEY_ACTIVE_CHAT_ID)
                .apply();
    }

    private void persistConversation() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (chatSessions.isEmpty()) {
            loadSessionsFromPrefs(context);
            if (chatSessions.isEmpty()) {
                createNewSession(true);
            }
        }

        ChatSession active = getActiveSession();
        if (active == null) {
            active = createNewSession(true);
        }

        active.messages.clear();
        for (int i = 1; i < conversation.size(); i++) {
            OpenAiClient.Message msg = conversation.get(i);
            if (!"user".equals(msg.role) && !"assistant".equals(msg.role)) {
                continue;
            }
            active.messages.add(new OpenAiClient.Message(msg.role, msg.content));
        }
        active.updatedAt = System.currentTimeMillis();
        active.title = buildSessionTitle(active.messages);

        saveSessionsToPrefs(context);
    }

    private void loadSessionsFromPrefs(@NonNull Context context) {
        chatSessions.clear();
        SharedPreferences prefs = context.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE);

        migrateLegacyHistoryIfNeeded(prefs);

        String raw = prefs.getString(KEY_CHAT_SESSIONS, "");
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                JSONArray sessions = new JSONArray(raw);
                for (int i = 0; i < sessions.length(); i++) {
                    JSONObject item = sessions.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    ChatSession session = new ChatSession();
                    session.id = item.optString("id", "").trim();
                    session.title = item.optString("title", "").trim();
                    session.updatedAt = item.optLong("updatedAt", System.currentTimeMillis());
                    JSONArray messages = item.optJSONArray("messages");
                    if (messages != null) {
                        for (int j = 0; j < messages.length(); j++) {
                            JSONObject msg = messages.optJSONObject(j);
                            if (msg == null) {
                                continue;
                            }
                            String role = msg.optString("role", "").trim();
                            String content = msg.optString("content", "").trim();
                            if (content.isEmpty()) {
                                continue;
                            }
                            if (!"user".equals(role) && !"assistant".equals(role)) {
                                continue;
                            }
                            session.messages.add(new OpenAiClient.Message(role, content));
                        }
                    }
                    if (session.id.isEmpty()) {
                        session.id = String.valueOf(System.currentTimeMillis() + i);
                    }
                    if (session.title.isEmpty()) {
                        session.title = buildSessionTitle(session.messages);
                    }
                    chatSessions.add(session);
                }
            } catch (Exception ignored) {
            }
        }

        activeChatId = prefs.getString(KEY_ACTIVE_CHAT_ID, "");
        if (activeChatId == null || activeChatId.trim().isEmpty()) {
            activeChatId = null;
        }

        if (chatSessions.isEmpty()) {
            createNewSession(true);
            saveSessionsToPrefs(context);
            return;
        }

        boolean activeFound = false;
        if (activeChatId != null) {
            for (ChatSession session : chatSessions) {
                if (activeChatId.equals(session.id)) {
                    activeFound = true;
                    break;
                }
            }
        }
        if (!activeFound) {
            activeChatId = chatSessions.get(0).id;
        }
    }

    private void saveSessionsToPrefs(@NonNull Context context) {
        JSONArray sessions = new JSONArray();
        try {
            for (ChatSession session : chatSessions) {
                JSONObject item = new JSONObject();
                item.put("id", session.id);
                item.put("title", session.title);
                item.put("updatedAt", session.updatedAt);

                JSONArray messages = new JSONArray();
                for (OpenAiClient.Message message : session.messages) {
                    if (!"user".equals(message.role) && !"assistant".equals(message.role)) {
                        continue;
                    }
                    JSONObject msg = new JSONObject();
                    msg.put("role", message.role);
                    msg.put("content", message.content);
                    messages.put(msg);
                }
                item.put("messages", messages);
                sessions.put(item);
            }

            context.getSharedPreferences(AiPreferences.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CHAT_SESSIONS, sessions.toString())
                    .putString(KEY_ACTIVE_CHAT_ID, activeChatId == null ? "" : activeChatId)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void migrateLegacyHistoryIfNeeded(@NonNull SharedPreferences prefs) {
        String existing = prefs.getString(KEY_CHAT_SESSIONS, "");
        if (existing != null && !existing.trim().isEmpty()) {
            return;
        }

        String legacy = prefs.getString(AiPreferences.KEY_SENSEI_CHAT_HISTORY, "");
        ChatSession session = new ChatSession();
        session.id = String.valueOf(System.currentTimeMillis());
        session.updatedAt = System.currentTimeMillis();
        session.title = "New chat";

        if (legacy != null && !legacy.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(legacy);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject msg = array.optJSONObject(i);
                    if (msg == null) {
                        continue;
                    }
                    String role = msg.optString("role", "").trim();
                    String content = msg.optString("content", "").trim();
                    if (content.isEmpty()) {
                        continue;
                    }
                    if (!"user".equals(role) && !"assistant".equals(role)) {
                        continue;
                    }
                    session.messages.add(new OpenAiClient.Message(role, content));
                }
                session.title = buildSessionTitle(session.messages);
            } catch (Exception ignored) {
            }
        }

        JSONArray sessions = new JSONArray();
        try {
            JSONObject item = new JSONObject();
            item.put("id", session.id);
            item.put("title", session.title);
            item.put("updatedAt", session.updatedAt);
            JSONArray messages = new JSONArray();
            for (OpenAiClient.Message message : session.messages) {
                JSONObject msg = new JSONObject();
                msg.put("role", message.role);
                msg.put("content", message.content);
                messages.put(msg);
            }
            item.put("messages", messages);
            sessions.put(item);
            prefs.edit()
                    .putString(KEY_CHAT_SESSIONS, sessions.toString())
                    .putString(KEY_ACTIVE_CHAT_ID, session.id)
                    .remove(AiPreferences.KEY_SENSEI_CHAT_HISTORY)
                    .apply();
        } catch (Exception ignored) {
        }
    }

    @Nullable
    private ChatSession getActiveSession() {
        if (chatSessions.isEmpty()) {
            return null;
        }
        if (activeChatId == null || activeChatId.trim().isEmpty()) {
            return chatSessions.get(0);
        }
        for (ChatSession session : chatSessions) {
            if (activeChatId.equals(session.id)) {
                return session;
            }
        }
        return chatSessions.get(0);
    }

    @NonNull
    private String buildSessionTitle(@NonNull List<OpenAiClient.Message> messages) {
        for (OpenAiClient.Message message : messages) {
            if ("user".equals(message.role) && message.content != null) {
                String clean = message.content.trim().replace('\n', ' ');
                if (clean.isEmpty()) {
                    continue;
                }
                if (clean.length() > 36) {
                    return clean.substring(0, 36) + "...";
                }
                return clean;
            }
        }
        return "New chat";
    }

    private String getNoKeyCoachReply(String question) {
        String q = question.toLowerCase();
        if (q.contains("lose weight") || q.contains("fat loss") || q.contains("cut")) {
            return "For fat loss: eat in a small calorie deficit, keep protein high, train 3-5x/week, and walk daily. If you share your age/weight/height, I can give exact calories.";
        }
        if (q.contains("muscle") || q.contains("bulk") || q.contains("gain")) {
            return "For muscle gain: use a small calorie surplus, progressive overload, and 1.6-2.2g protein/kg body weight. Focus on compounds and sleep 7-9 hours.";
        }
        if (q.contains("protein") || q.contains("carb") || q.contains("fat")) {
            return "Macro baseline: protein 1.6-2.2 g/kg, fats 0.6-1.0 g/kg, rest carbs. Adjust weekly based on weight trend and training performance.";
        }
        if (q.contains("workout") || q.contains("routine")) {
            return "Start with Upper/Lower 4-day split or Full-body 3-day split. Track sets/reps and add reps or weight each week.";
        }
        if (q.contains("meal") || q.contains("diet")) {
            return "Keep meals simple: protein source + veggie + carb source. Build 3-4 repeatable meals so tracking stays easy.";
        }
        return "Ask me your exact goal (fat loss, muscle gain, maintenance) plus age, height, weight, and activity. I will give a step-by-step plan.";
    }

    private void appendMessage(String label, String text) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        View row = LayoutInflater.from(context).inflate(R.layout.item_sensei_message, senseiMessages, false);
        TextView msgLabel = row.findViewById(R.id.msgLabel);
        TextView msgText = row.findViewById(R.id.msgText);
        LinearLayout actions = row.findViewById(R.id.msgActions);
        msgLabel.setText(label);
        msgText.setText(text);
        if ("You".equals(label)) {
            msgText.setBackgroundResource(R.drawable.sensei_message_user_bubble);
        } else {
            msgText.setBackgroundResource(R.drawable.sensei_message_card);
        }
        if ("Sensei".equals(label) && actions != null) {
            bindSenseiActions(actions, text);
        }
        senseiMessages.addView(row);
        senseiScroll.post(this::scrollToBottom);
    }

    private void scrollToBottom() {
        if (senseiScroll == null) {
            return;
        }
        senseiScroll.post(() -> {
            View content = senseiScroll.getChildAt(0);
            if (content == null) {
                return;
            }
            int scrollRange = content.getHeight() - senseiScroll.getHeight();
            int y = Math.max(0, scrollRange);
            senseiScroll.smoothScrollTo(0, y);
        });
    }

    private void bindSenseiActions(@NonNull LinearLayout actions, @NonNull String assistantText) {
        actions.removeAllViews();
        actions.setVisibility(View.GONE);

        String t = assistantText.toLowerCase();
        boolean wantsDietChange = t.contains("diet plan") || t.contains("change diet") || t.contains("modify diet") || t.contains("edit diet");
        boolean wantsWorkoutChange = t.contains("change workout") || t.contains("modify workout") || t.contains("edit workout") || t.contains("add workout") || t.contains("routine");
        boolean canUndo = lastDietPlanSnapshot != null
                || (lastWorkoutSnapshot != null && lastWorkoutSnapshotDay != null)
                || !lastInsertedMealIds.isEmpty();

        if (!wantsDietChange && !wantsWorkoutChange && !canUndo) {
            return;
        }

        actions.setVisibility(View.VISIBLE);

        if (wantsDietChange) {
            MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText("Apply Diet Plan Change");
            btn.setOnClickListener(v -> applySenseiDietPlanChange());
            actions.addView(btn);

            MaterialButton addMeals = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            addMeals.setText("Add plan to today's meals");
            addMeals.setOnClickListener(v -> applyDietPlanToToday());
            actions.addView(addMeals);
        }

        if (wantsWorkoutChange) {
            MaterialButton btn = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText("Apply Workout Change");
            btn.setOnClickListener(v -> applySenseiWorkoutChange());
            actions.addView(btn);
        }

        MaterialButton undo = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        undo.setText("Undo");
        undo.setEnabled(canUndo);
        undo.setOnClickListener(v -> undoSenseiChanges());
        actions.addView(undo);
    }

    private void applySenseiDietPlanChange() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FitooDatabase db = FitooDatabase.get(context);
        AppExecutors.get().io().execute(() -> {
            // Snapshot for undo
            lastDietPlanSnapshot = db.dietPlanDao().getAll();

            // Simple “Sensei change”: replace plan with a balanced template.
            // (Keeps it deterministic and avoids fragile parsing.)
            db.dietPlanDao().deleteAll();
            insertDietPlanTemplate(db, "Breakfast", "Oats", 80f, "g");
            insertDietPlanTemplate(db, "Lunch", "Chicken", 200f, "g");
            insertDietPlanTemplate(db, "Snacks", "Banana", 1f, "count");
            insertDietPlanTemplate(db, "Dinner", "Rice", 250f, "g");

            AppExecutors.get().main().execute(() -> {
                showToast("Diet plan updated. Tap 'Add plan to today' to add meals.");
                appendMessage("Sensei", "Diet plan updated. You can add it to today's meals or Undo.");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNav.setSelectedItemId(R.id.nav_meals);
                }
            });
        });
    }

    private void applyDietPlanToToday() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FitooDatabase db = FitooDatabase.get(context);
        long startToday = startOfDay(System.currentTimeMillis());
        long endToday = endOfDay(System.currentTimeMillis());
        AppExecutors.get().io().execute(() -> {
            List<DietPlanItem> plan = db.dietPlanDao().getAll();
            lastInsertedMealIds.clear();
            for (DietPlanItem item : plan) {
                MealEntry meal = new MealEntry();
                meal.name = item.name;
                meal.category = item.category;
                meal.quantity = item.quantity;
                meal.quantityUnit = item.quantityUnit != null ? item.quantityUnit : "count";
                meal.calories = item.calories;
                meal.protein = item.protein;
                meal.carbs = item.carbs;
                meal.fats = item.fats;
                meal.fiber = item.fiber;
                // timestamp within today window, stable but unique-ish
                meal.timestamp = startToday + (meal.hashCode() & 0xFFFF);
                if (meal.timestamp > endToday) {
                    meal.timestamp = endToday;
                }
                meal.eaten = false;
                int id = (int) db.mealDao().insertMeal(meal);
                lastInsertedMealIds.add(id);
            }
            AppExecutors.get().main().execute(() -> {
                showToast("Added diet plan to today's meals. Tap Undo to revert.");
                appendMessage("Sensei", "Diet plan added to today's meals. Undo is available.");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNav.setSelectedItemId(R.id.nav_meals);
                }
            });
        });
    }

    private void insertDietPlanTemplate(@NonNull FitooDatabase db,
                                        @NonNull String category,
                                        @NonNull String name,
                                        float qty,
                                        @NonNull String unit) {
        NutritionLookup.MacroInfo info = NutritionLookup.estimate(name, qty, "g".equals(unit));
        if (info == null) {
            info = new NutritionLookup.MacroInfo();
        }
        DietPlanItem item = new DietPlanItem();
        item.category = category;
        item.name = name;
        item.quantity = qty;
        item.quantityUnit = unit;
        item.calories = info.calories;
        item.protein = info.protein;
        item.carbs = info.carbs;
        item.fats = info.fats;
        item.fiber = info.fiber;
        item.sortOrder = 0;
        db.dietPlanDao().insert(item);
    }

    private void applySenseiWorkoutChange() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FitooDatabase db = FitooDatabase.get(context);
        long today = startOfDay(System.currentTimeMillis());
        AppExecutors.get().io().execute(() -> {
            // Snapshot for undo
            lastWorkoutSnapshotDay = today;
            lastWorkoutSnapshot = db.workoutLogDao().getByDay(today);

            // Replace today’s plan with a simple template.
            db.workoutLogDao().deleteByDay(today);
            insertWorkoutTemplate(db, today, "Chest", "Push-ups", 3, 15);
            insertWorkoutTemplate(db, today, "Back", "Lat Pulldown", 4, 10);
            insertWorkoutTemplate(db, today, "Legs", "Barbell Squats", 4, 8);
            insertWorkoutTemplate(db, today, "Core", "Plank", 3, 1);

            AppExecutors.get().main().execute(() -> {
                showToast("Workout updated. Tap Undo to revert.");
                appendMessage("Sensei", "Workout updated. Undo is available.");
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNav.setSelectedItemId(R.id.nav_workouts);
                }
            });
        });
    }

    private void insertWorkoutTemplate(@NonNull FitooDatabase db,
                                       long day,
                                       @NonNull String group,
                                       @NonNull String name,
                                       int sets,
                                       int reps) {
        WorkoutLogEntry entry = new WorkoutLogEntry();
        entry.dateDay = day;
        entry.muscleGroup = group;
        entry.exerciseName = name;
        entry.sets = sets;
        entry.reps = reps;
        entry.completed = false;
        db.workoutLogDao().insert(entry);
    }

    private void undoSenseiChanges() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        FitooDatabase db = FitooDatabase.get(context);
        AppExecutors.get().io().execute(() -> {
            boolean didUndo = false;

            if (lastDietPlanSnapshot != null) {
                db.dietPlanDao().deleteAll();
                for (DietPlanItem item : lastDietPlanSnapshot) {
                    // Reinsert snapshot items (id ignored/auto)
                    DietPlanItem copy = new DietPlanItem();
                    copy.name = item.name;
                    copy.category = item.category;
                    copy.quantity = item.quantity;
                    copy.quantityUnit = item.quantityUnit;
                    copy.calories = item.calories;
                    copy.protein = item.protein;
                    copy.carbs = item.carbs;
                    copy.fats = item.fats;
                    copy.fiber = item.fiber;
                    copy.sortOrder = item.sortOrder;
                    db.dietPlanDao().insert(copy);
                }
                lastDietPlanSnapshot = null;
                didUndo = true;
            }

            if (lastWorkoutSnapshot != null && lastWorkoutSnapshotDay != null) {
                db.workoutLogDao().deleteByDay(lastWorkoutSnapshotDay);
                for (WorkoutLogEntry entry : lastWorkoutSnapshot) {
                    WorkoutLogEntry copy = new WorkoutLogEntry();
                    copy.dateDay = entry.dateDay;
                    copy.muscleGroup = entry.muscleGroup;
                    copy.exerciseName = entry.exerciseName;
                    copy.sets = entry.sets;
                    copy.reps = entry.reps;
                    copy.completed = entry.completed;
                    db.workoutLogDao().insert(copy);
                }
                lastWorkoutSnapshot = null;
                lastWorkoutSnapshotDay = null;
                didUndo = true;
            }

            if (!lastInsertedMealIds.isEmpty()) {
                for (Integer id : new ArrayList<>(lastInsertedMealIds)) {
                    if (id != null) {
                        db.mealDao().deleteById(id);
                    }
                }
                lastInsertedMealIds.clear();
                didUndo = true;
            }

            boolean finalDidUndo = didUndo;
            AppExecutors.get().main().execute(() -> {
                if (finalDidUndo) {
                    showToast("Reverted.");
                } else {
                    showToast("Nothing to undo yet.");
                }
            });
        });
    }

    private long startOfDay(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long endOfDay(long millis) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(java.util.Calendar.HOUR_OF_DAY, 23);
        c.set(java.util.Calendar.MINUTE, 59);
        c.set(java.util.Calendar.SECOND, 59);
        c.set(java.util.Calendar.MILLISECOND, 999);
        return c.getTimeInMillis();
    }

    private void trimConversation() {
        while (conversation.size() > MAX_MESSAGES) {
            if (conversation.size() > 1) {
                conversation.remove(1);
            } else {
                break;
            }
        }
    }

    private void showToast(String text) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void runOnUiThreadSafe(Runnable runnable) {
        if (!isAdded()) {
            return;
        }
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            runnable.run();
        });
    }
}
