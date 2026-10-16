package com.example.practice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class WeatherActivity extends AppCompatActivity {

    private TextView weatherText;
    private TextView weatherAdviceText;

    private Button refreshButton;
    private ImageButton menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_weather);

        // UI 연결
        weatherText = findViewById(R.id.weatherText);
        weatherAdviceText = findViewById(R.id.weatherAdviceText);

        refreshButton = findViewById(R.id.weatherRefreshButton);
        menuButton = findViewById(R.id.menuButton);

        // 처음 날씨 불러오기
        loadWeather();

        // 새로고침
        refreshButton.setOnClickListener(v -> {
            loadWeather();
        });

        // 메뉴
        menuButton.setOnClickListener(v -> {
            showMenu();
        });
    }

    private void showMenu() {

        final String[] items = {
                "홈",
                "채팅",
                "일정",
                "음성채팅"
        };

        new AlertDialog.Builder(this)
                .setTitle("메뉴")
                .setItems(items, (dialog, which) -> {

                    switch (which) {

                        // 홈
                        case 0:
                            Intent homeIntent =
                                    new Intent(
                                            WeatherActivity.this,
                                            HomeActivity.class
                                    );

                            startActivity(homeIntent);
                            finish();
                            break;

                        // 채팅
                        case 1:
                            Intent chatIntent =
                                    new Intent(
                                            WeatherActivity.this,
                                            MainActivity.class
                                    );

                            startActivity(chatIntent);
                            finish();
                            break;

                        // 일정
                        case 2:
                            Intent planIntent =
                                    new Intent(
                                            WeatherActivity.this,
                                            PlanActivity.class
                                    );

                            startActivity(planIntent);
                            finish();
                            break;

                        // 음성채팅
                        case 3:
                            Intent voiceIntent =
                                    new Intent(
                                            WeatherActivity.this,
                                            VoiceChatActivity.class
                                    );

                            startActivity(voiceIntent);
                            finish();
                            break;
                    }
                })
                .show();
    }

    private void loadWeather() {

        weatherText.setText("날씨 정보를 불러오는 중...");
        weatherAdviceText.setText("날씨 정보를 확인하고 있습니다.");

        // 서울 좌표
        double latitude = 37.5665;
        double longitude = 126.9780;

        WeatherManager.getWeather(
                latitude,
                longitude,
                new WeatherManager.WeatherCallback() {

                    @Override
                    public void onSuccess(String result) {

                        runOnUiThread(() -> {

                            // 현재 날씨 정보
                            weatherText.setText(result);

                            // AI 비서 영역
                            weatherAdviceText.setText(
                                    "현재 서울의 날씨 정보를 확인했습니다.\n"
                                            + "자세한 날씨 정보는 위에서 확인할 수 있습니다."
                            );
                        });
                    }

                    @Override
                    public void onError(String error) {

                        runOnUiThread(() -> {

                            weatherText.setText("날씨 정보를 불러오지 못했습니다.");

                            weatherAdviceText.setText(
                                    "날씨 정보를 가져오는 중 문제가 발생했습니다.\n"
                                            + "새로고침을 눌러 다시 시도해 주세요."
                            );
                        });
                    }
                }
        );
    }
}