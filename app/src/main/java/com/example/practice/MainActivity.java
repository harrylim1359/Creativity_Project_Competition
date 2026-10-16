package com.example.practice;

import com.example.practice.GeminiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.content.Intent;
import android.widget.Button;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText messageInput;
    private ImageButton sendButton;
    private LinearLayout chatContainer;
    private ScrollView chatScrollView;
    private GeminiManager geminiManager;

    private ChatManager chatManager;
    private ImageButton menuButton;
    private Button weatherButton;
    private ContextManager contextManager;
    private boolean isRequestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatContainer = findViewById(R.id.chatContainer);
        chatScrollView = findViewById(R.id.chatScrollView);

        chatManager = new ChatManager();
        geminiManager = new GeminiManager(
                GeminiConfig.API_KEY,
                this
        );

        sendButton.setOnClickListener(v -> sendMessage());

        menuButton = findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> showMenu());

        contextManager = new ContextManager(this);
        contextManager.addContext(
                "사용자는 Android AI 비서 앱을 개발하고 있다."
        );
        android.util.Log.d(
                "ContextDebug",
                contextManager.getContextForGemini()
        );
    }

    private void showMenu() {

        final String[] items = {
                "홈",
                "일정",
                "날씨",
                "음성채팅"
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("메뉴")
                .setItems(items, (dialog, which) -> {

                    switch (which) {

                        case 0:
                            Intent homeIntent =
                                    new Intent(MainActivity.this, HomeActivity.class);

                            startActivity(homeIntent);
                            finish();
                            break;

                        case 1:

                            Intent planIntent =
                                    new Intent(
                                            MainActivity.this,
                                            PlanActivity.class
                                    );

                            startActivity(planIntent);
                            finish();

                            break;

                        case 2:
                            Intent weatherIntent =
                                    new Intent(MainActivity.this, WeatherActivity.class);

                            startActivity(weatherIntent);
                            finish();
                            break;

                        case 3:
                            Intent intent = new Intent(MainActivity.this, VoiceChatActivity.class);
                            startActivity(intent);
                            break;
                    }

                })
                .show();
    }

    private void sendMessage() {

        String text = messageInput.getText().toString().trim();

        if (text.isEmpty()) {
            return;
        }

        // 요청이 진행 중이면 중복 전송 방지
        if (isRequestInProgress) {
            Log.w("GeminiDebug", "요청 진행 중 - 중복 전송 차단");
            return;
        }

        isRequestInProgress = true;
        sendButton.setEnabled(false);

        chatManager.addUserMessage(text);
        addUserMessageView(text);

        messageInput.setText("");

        // 현재는 테스트용 AI 응답
        //String response = geminiManager.sendMessage(text);
        new Thread(() -> {

            String response = "";

            try {
                response = geminiManager.sendMessage(text);

            } catch (Exception e) {

                Log.e("GeminiDebug", "========== ACTUAL GEMINI ERROR ==========");
                Log.e("GeminiDebug", "Exception class: " + e.getClass().getName());
                Log.e("GeminiDebug", "Exception message: " + e.getMessage());
                Log.e("GeminiDebug", "Full exception:", e);

                response =
                "에러가 발생했습니다.";
            }

            String finalResponse = response;

            runOnUiThread(() -> {
                chatManager.addAIMessage(finalResponse);
                addAIMessageView(finalResponse);
                scrollToBottom();

                // 요청 완료: 다음 메시지 전송 허용
                isRequestInProgress = false;
                sendButton.setEnabled(true);
            });

        }).start();

        //chatManager.addAIMessage(response);
        //addAIMessageView(response);

        scrollToBottom();
    }

    private void addUserMessageView(String text) {

        LinearLayout messageLayout = new LinearLayout(this);

        messageLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        messageLayout.setGravity(Gravity.END);

        TextView message = new TextView(this);

        message.setText(text);
        message.setTextSize(16);
        message.setTextColor(0xFFFFFFFF);
        message.setPadding(14, 10, 14, 10);
        message.setBackgroundColor(0xFF6C63FF);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);

        message.setLayoutParams(params);

        messageLayout.addView(message);
        chatContainer.addView(messageLayout);
    }

    private void addAIMessageView(String text) {

        LinearLayout messageLayout = new LinearLayout(this);

        messageLayout.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        messageLayout.setGravity(Gravity.START);

        TextView message = new TextView(this);

        message.setText(text);
        message.setTextSize(16);
        message.setTextColor(0xFF202124);
        message.setPadding(14, 10, 14, 10);
        message.setBackgroundColor(0xFFFFFFFF);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 0, 0, 12);

        message.setLayoutParams(params);

        messageLayout.addView(message);
        chatContainer.addView(messageLayout);
    }

    private void scrollToBottom() {

        chatScrollView.post(() ->
                chatScrollView.fullScroll(View.FOCUS_DOWN)
        );
    }


    // ==========================
    // ChatManager
    // ==========================

    private class ChatManager {

        private final java.util.ArrayList<Message> messages;

        public ChatManager() {
            messages = new java.util.ArrayList<>();
        }

        public void addUserMessage(String text) {
            messages.add(new Message(text, MessageType.USER));
        }

        public void addAIMessage(String text) {
            messages.add(new Message(text, MessageType.AI));
        }
    }


    // ==========================
    // Message
    // ==========================

    private static class Message {

        private final String text;
        private final MessageType type;

        public Message(String text, MessageType type) {
            this.text = text;
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public MessageType getType() {
            return type;
        }
    }


    // ==========================
    // MessageType
    // ==========================

    private enum MessageType {
        USER,
        AI
    }
}
