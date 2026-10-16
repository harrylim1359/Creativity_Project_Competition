package com.example.practice;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button chatButton = findViewById(R.id.chatButton);
        Button planButton = findViewById(R.id.planButton);
        Button weatherButton = findViewById(R.id.weatherButton);
        Button voiceChatButton = findViewById(R.id.voiceChatButton);

        // 채팅
        chatButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // 일정
        planButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PlanActivity.class);
            startActivity(intent);
        });

        // 날씨
        weatherButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WeatherActivity.class);
            startActivity(intent);
        });

        // 음성 채팅
        voiceChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, VoiceChatActivity.class);
            startActivity(intent);
        });
    }
}