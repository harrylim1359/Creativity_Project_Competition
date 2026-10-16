package com.example.practice;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PlanActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView dateText;
    private LinearLayout planContainer;
    private Button addButton;
    private ImageButton menuButton;

    private PlanManager planManager;

    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan);

        calendarView = findViewById(R.id.calendarView);
        dateText = findViewById(R.id.dateText);
        planContainer = findViewById(R.id.planContainer);
        addButton = findViewById(R.id.addButton);
        menuButton = findViewById(R.id.menuButton);

        planManager = new PlanManager(this);

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            requestPermissions(
                    new String[]{
                            android.Manifest.permission.POST_NOTIFICATIONS
                    },
                    100
            );
        }

        selectedDate =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(new Date());

        updateDateText();
        loadPlans();

        calendarView.setOnDateChangeListener(
                (view, year, month, dayOfMonth) -> {

                    selectedDate = String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            dayOfMonth
                    );

                    updateDateText();
                    loadPlans();
                }
        );

        addButton.setOnClickListener(
                v -> showAddPlanDialog()
        );

        menuButton.setOnClickListener(
                v -> showMenu()
        );
    }

    private void updateDateText() {

        dateText.setText(
                selectedDate.replace("-", ".")
        );
    }

    private void loadPlans() {

        planContainer.removeAllViews();

        List<Plan> plans =
                planManager.getPlansByDate(selectedDate);

        if (plans.isEmpty()) {

            TextView emptyText =
                    new TextView(this);

            emptyText.setText(
                    "등록된 일정이 없습니다."
            );

            emptyText.setTextSize(16);
            emptyText.setPadding(
                    8,
                    20,
                    8,
                    20
            );

            planContainer.addView(emptyText);

            return;
        }

        for (Plan plan : plans) {
            addPlanView(plan);
        }
    }

    private void addPlanView(Plan plan) {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        layout.setGravity(
                Gravity.CENTER_VERTICAL
        );

        layout.setPadding(
                12,
                12,
                12,
                12
        );

        TextView text =
                new TextView(this);

        text.setText(
                plan.getTime()
                        + "  "
                        + plan.getTitle()
        );

        text.setTextSize(16);

        text.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button deleteButton =
                new Button(this);

        deleteButton.setText("삭제");

        deleteButton.setOnClickListener(
                v -> {

                    planManager.deletePlan(
                            plan.getId()
                    );

                    loadPlans();
                }
        );

        layout.addView(text);
        layout.addView(deleteButton);

        planContainer.addView(layout);
    }

    private void showAddPlanDialog() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                10,
                40,
                10
        );

        EditText titleInput =
                new EditText(this);

        titleInput.setHint("일정 이름");

        layout.addView(titleInput);

        TimePicker timePicker =
                new TimePicker(this);

        timePicker.setIs24HourView(true);

        layout.addView(timePicker);

        new AlertDialog.Builder(this)
                .setTitle("일정 추가")
                .setView(layout)
                .setPositiveButton(
                        "추가",
                        (dialog, which) -> {

                            String title =
                                    titleInput
                                            .getText()
                                            .toString()
                                            .trim();

                            if (title.isEmpty()) {
                                return;
                            }

                            int hour =
                                    timePicker
                                            .getHour();

                            int minute =
                                    timePicker
                                            .getMinute();

                            String time =
                                    String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            hour,
                                            minute
                                    );

                            planManager.addPlan(
                                    selectedDate,
                                    time,
                                    title
                            );

                            loadPlans();
                        }
                )
                .setNegativeButton(
                        "취소",
                        null
                )
                .show();
    }

    private void showMenu() {

        String[] items = {
                "홈",
                "채팅",
                "날씨",
                "음성채팅"
        };

        new AlertDialog.Builder(this)
                .setTitle("메뉴")
                .setItems(
                        items,
                        (dialog, which) -> {

                            switch (which) {

                                // 홈
                                case 0:
                                    Intent homeIntent =
                                            new Intent(
                                                    PlanActivity.this,
                                                    HomeActivity.class
                                            );

                                    startActivity(homeIntent);
                                    finish();
                                    break;

                                // 채팅
                                case 1:
                                    Intent chatIntent =
                                            new Intent(
                                                    PlanActivity.this,
                                                    MainActivity.class
                                            );

                                    startActivity(chatIntent);
                                    finish();
                                    break;

                                // 날씨
                                case 2:
                                    Intent weatherIntent =
                                            new Intent(
                                                    PlanActivity.this,
                                                    WeatherActivity.class
                                            );

                                    startActivity(weatherIntent);
                                    finish();
                                    break;

                                // 음성채팅
                                case 3:
                                    Intent voiceIntent =
                                            new Intent(
                                                    PlanActivity.this,
                                                    VoiceChatActivity.class
                                            );

                                    startActivity(voiceIntent);
                                    finish();
                                    break;
                            }
                        }
                )
                .show();
    }
}