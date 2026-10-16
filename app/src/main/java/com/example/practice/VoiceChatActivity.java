package com.example.practice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.ImageButton;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class VoiceChatActivity extends AppCompatActivity {

    private static final int RECORD_AUDIO_REQUEST = 1001;
    private boolean isListening = false;
    private boolean isStarting = false;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private TextView voiceStatus;
    private Button microphoneButton;
    private TextToSpeech textToSpeech;
    private GeminiManager geminiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);

        voiceStatus = findViewById(R.id.voiceStatus);
        microphoneButton = findViewById(R.id.microphoneButton);

        ImageButton menuButton = findViewById(R.id.menuButton);

        menuButton.setOnClickListener(v -> showMenu());

        setupSpeechRecognizer();

        geminiManager = new GeminiManager(
                GeminiConfig.API_KEY,
                this
        );

        textToSpeech = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                textToSpeech.setLanguage(Locale.KOREAN);

            }
        });

        microphoneButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startListening();
                    return true;

                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    android.util.Log.d(
                            "VoiceChat",
                            "마이크 버튼 터치 종료"
                    );

                    // 여기서는 stopListening()을 호출하지 않음.
                    // 음성 인식 서비스가 발화 종료를 감지하고
                    // onResults() 또는 onError()를 호출하도록 둠.
                    return true;
            }

            return true;
        });
    }

    private void setupSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceStatus.setText("음성 인식을 사용할 수 없습니다.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ko-KR"
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
        );

        // 디버깅: 음성 인식 서비스 확인
        android.content.pm.PackageManager pm = getPackageManager();

        Intent serviceIntent = new Intent(
                android.speech.RecognitionService.SERVICE_INTERFACE
        );

        java.util.List<android.content.pm.ResolveInfo> services =
                pm.queryIntentServices(
                        serviceIntent,
                        android.content.pm.PackageManager.MATCH_ALL
                );

        android.util.Log.d(
                "VoiceChat",
                "음성 인식 서비스 개수 = "
                        + (services == null ? 0 : services.size())
        );

        if (services != null) {
            for (android.content.pm.ResolveInfo service : services) {
                android.util.Log.d(
                        "VoiceChat",
                        "인식 서비스 = "
                                + service.serviceInfo.packageName
                                + " / "
                                + service.serviceInfo.name
                );
            }
        }

        android.util.Log.d(
                "VoiceChat",
                "요청 언어 = "
                        + speechIntent.getStringExtra(
                        RecognizerIntent.EXTRA_LANGUAGE
                )
        );

        speechIntent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "ko-KR"
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                isStarting = false;
                isListening = true;

                android.util.Log.d(
                        "VoiceChat",
                        "SpeechRecognizer 준비 완료"
                );

                voiceStatus.setText("듣고 있습니다...");
            }

            @Override
            public void onBeginningOfSpeech() {

                android.util.Log.d(
                        "VoiceChat",
                        "사용자가 말하기 시작함"
                );

                voiceStatus.setText("말씀해주세요...");
            }

            @Override
            public void onEndOfSpeech() {

                android.util.Log.d(
                        "VoiceChat",
                        "발화 종료 콜백 수신"
                );

                voiceStatus.setText("생각 중...");
            }

            @Override
            public void onResults(Bundle results) {
                isStarting = false;
                isListening = false;

                android.util.Log.d("VoiceChat", "onResults() 호출됨");
                ArrayList<String> result =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                android.util.Log.d(
                        "VoiceChat",
                        "최종 인식 결과 = " + result
                );

                if (result != null && !result.isEmpty()) {
                    String text = result.get(0);

                    voiceStatus.setText(text);
                    sendToGemini(text);
                } else {
                    voiceStatus.setText("인식 결과가 없습니다.");
                }
            }
            @Override
            public void onError(int error) {
                isStarting = false;
                isListening = false;

                android.util.Log.e("VoiceChat", "onError() 호출됨: " + error);

                if (error == SpeechRecognizer.ERROR_NO_MATCH) {
                    voiceStatus.setText(
                            "음성을 인식하지 못했어요. 조금 더 또렷하게 말해 주세요."
                    );
                    return;
                }

                String errorMessage;

                switch (error) {

                    case SpeechRecognizer.ERROR_AUDIO:
                        errorMessage = "ERROR_AUDIO";
                        break;

                    case SpeechRecognizer.ERROR_CLIENT:
                        errorMessage = "ERROR_CLIENT";
                        break;

                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        errorMessage = "ERROR_INSUFFICIENT_PERMISSIONS";
                        break;

                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = "ERROR_NETWORK";
                        break;

                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        errorMessage = "ERROR_NETWORK_TIMEOUT";
                        break;

                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = "ERROR_NO_MATCH";
                        runOnUiThread(() ->
                                voiceStatus.setText(
                                        "음성을 인식하지 못했어요.\n" +
                                                "조금 더 또렷하게 다시 말해 주세요."
                                )
                        );
                        return;

                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        errorMessage = "ERROR_RECOGNIZER_BUSY";
                        break;

                    case SpeechRecognizer.ERROR_SERVER:
                        errorMessage = "ERROR_SERVER";
                        break;

                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        errorMessage = "ERROR_SPEECH_TIMEOUT";
                        break;

                    default:
                        errorMessage = "UNKNOWN_ERROR";
                        break;
                }

                android.util.Log.e(
                        "VoiceChat",
                        "SpeechRecognizer error = "
                                + error
                                + " ("
                                + errorMessage
                                + ")"
                );

                runOnUiThread(() -> {

                    voiceStatus.setText(
                            "음성 인식 오류\n"
                                    + error
                                    + "\n"
                                    + errorMessage
                    );

                });
            }

            @Override
            public void onRmsChanged(float rmsdB) {

                android.util.Log.d(
                        "VoiceChat",
                        "음성 입력 레벨 = " + rmsdB
                );
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> results =
                        partialResults.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                android.util.Log.d(
                        "VoiceChat",
                        "중간 인식 결과 = " + results
                );

                if (results != null && !results.isEmpty()) {
                    runOnUiThread(() ->
                            voiceStatus.setText(results.get(0))
                    );
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private void startListening() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer();
        }

        if (speechRecognizer == null) {
            voiceStatus.setText("음성 인식 서비스를 사용할 수 없습니다.");
            return;
        }

        if (checkSelfPermission(
                android.Manifest.permission.RECORD_AUDIO
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            android.Manifest.permission.RECORD_AUDIO
                    },
                    RECORD_AUDIO_REQUEST
            );
            return;
        }

        try {
            if (isListening || isStarting) {
                android.util.Log.d(
                        "VoiceChat",
                        "인식 요청 중복 방지: isListening="
                                + isListening
                                + ", isStarting="
                                + isStarting
                );
                return;
            }

            try {
                isStarting = true;

                voiceStatus.setText("말씀해주세요...");
                android.util.Log.d("VoiceChat", "startListening() 호출");

                speechRecognizer.startListening(speechIntent);

            } catch (Exception e) {
                isStarting = false;
                isListening = false;

                android.util.Log.e(
                        "VoiceChat",
                        "startListening 실패",
                        e
                );

                voiceStatus.setText("음성 인식을 시작하지 못했습니다.");
            }
        } catch (Exception e) {
            android.util.Log.e(
                    "VoiceChat",
                    "startListening 실패",
                    e
            );
            voiceStatus.setText("음성 인식을 시작하지 못했습니다.");
        }
    }

    private void stopListening() {

        android.util.Log.d(
                "VoiceChat",
                "버튼에서 손을 뗌 → stopListening 호출"
        );

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    private void sendToGemini(String text) {

        voiceStatus.setText("생각 중...");

        new Thread(() -> {

            try {

                String answer = geminiManager.sendMessage(text);

                speakAnswer(answer);

            } catch (Exception e) {

                runOnUiThread(() -> {

                    voiceStatus.setText(
                            "오류가 발생했습니다: " + e.getMessage()
                    );

                });
            }

        }).start();
    }

    private void speakAnswer(String answer) {

        runOnUiThread(() -> {

            voiceStatus.setText("말하는 중...");

            textToSpeech.speak(
                    answer,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "gemini_answer"
            );
        });
    }

    private void showMenu() {

        String[] menuItems = {
                "홈",
                "채팅",
                "일정",
                "날씨"
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("메뉴")
                .setItems(menuItems, (dialogInterface, which) -> {

                    switch (which) {

                        // 홈
                        case 0:
                            startActivity(
                                    new Intent(
                                            VoiceChatActivity.this,
                                            HomeActivity.class
                                    )
                            );
                            finish();
                            break;

                        // 채팅
                        case 1:
                            startActivity(
                                    new Intent(
                                            VoiceChatActivity.this,
                                            MainActivity.class
                                    )
                            );
                            finish();
                            break;

                        // 일정
                        case 2:
                            startActivity(
                                    new Intent(
                                            VoiceChatActivity.this,
                                            PlanActivity.class
                                    )
                            );
                            finish();
                            break;

                        // 날씨
                        case 3:
                            startActivity(
                                    new Intent(
                                            VoiceChatActivity.this,
                                            WeatherActivity.class
                                    )
                            );
                            finish();
                            break;
                    }
                })
                .create();

        dialog.show();
    }

    @Override
    protected void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        super.onDestroy();
    }
}