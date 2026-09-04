package com.example.practice;

import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    CalendarView calendarView;

    TextView dateText;
    LinearLayout planContainer;
    Button addButton;

    // 날짜별 계획
    HashMap<String, ArrayList<Plan>> plans =
            new HashMap<>();

    // 현재 선택한 날짜
    String selectedDate;


    // =====================================================
    // 계획 데이터
    // =====================================================

    class Plan {

        String name;

        int startTime;
        int endTime;

        Plan(
                String name,
                int startTime,
                int endTime) {

            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }


    // =====================================================
    // 타임라인 변경 리스너
    // =====================================================

    interface OnTimeChangedListener {

        void onTimeChanged(
                int start,
                int end);
    }


    // =====================================================
    // 타임라인 View
    // =====================================================

    class TimelineView extends View {

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        int startTime;
        int endTime;

        int selectedHandle = 0;

        OnTimeChangedListener listener;


        TimelineView(
                int startTime,
                int endTime,
                OnTimeChangedListener listener) {

            super(MainActivity.this);

            this.startTime = startTime;
            this.endTime = endTime;
            this.listener = listener;

            setBackgroundColor(Color.WHITE);
        }


        int hourToX(int hour) {
            return dp(hour * 10) + dp(20);
        }


        int xToHour(float x) {

            int hour = Math.round(
                    (x - dp(20)) / dp(10)
            );

            if (hour < 0)
                hour = 0;

            if (hour > 24)
                hour = 24;

            return hour;
        }

        @Override
        protected void onMeasure(
                int widthMeasureSpec,
                int heightMeasureSpec) {

            int width = dp(24*10 + 40);
            int height = dp(100);

            setMeasuredDimension(
                    width,
                    height
            );
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            float centerY = dp(35);


            // =========================
            // 전체 선
            // =========================

            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(dp(5));

            canvas.drawLine(
                    dp(20),
                    centerY,
                    dp(24 * 10 + 20),
                    centerY,
                    paint
            );


            // =========================
            // 선택된 구간
            // =========================

            float startX =
                    hourToX(startTime);

            float endX =
                    hourToX(endTime);

            paint.setColor(
                    Color.rgb(50, 120, 220)
            );

            paint.setStrokeWidth(dp(12));

            canvas.drawLine(
                    startX,
                    centerY,
                    endX,
                    centerY,
                    paint
            );


            // =========================
            // 시간 눈금
            // =========================


            // =========================
            // 시작점
            // =========================

            paint.setColor(
                    Color.rgb(20, 80, 170)
            );

            canvas.drawCircle(
                    startX,
                    centerY,
                    dp(13),
                    paint
            );


            // =========================
            // 끝점
            // =========================

            canvas.drawCircle(
                    endX,
                    centerY,
                    dp(13),
                    paint
            );


            // 점 가운데
            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    startX,
                    centerY,
                    dp(5),
                    paint
            );

            canvas.drawCircle(
                    endX,
                    centerY,
                    dp(5),
                    paint
            );
        }


        @Override
        public boolean onTouchEvent(
                MotionEvent event) {

            float x = event.getX();


            if (event.getAction() ==
                    MotionEvent.ACTION_DOWN) {

                getParent()
                        .requestDisallowInterceptTouchEvent(
                                true
                        );

                float startX =
                        hourToX(startTime);

                float endX =
                        hourToX(endTime);


                if (Math.abs(x - startX)
                        <= Math.abs(x - endX)) {

                    selectedHandle = 1;

                } else {

                    selectedHandle = 2;
                }


                updateTime(x);

                return true;
            }


            if (event.getAction() ==
                    MotionEvent.ACTION_MOVE) {

                updateTime(x);

                return true;
            }


            if (event.getAction() ==
                    MotionEvent.ACTION_UP) {

                updateTime(x);

                getParent()
                        .requestDisallowInterceptTouchEvent(
                                false
                        );

                return true;
            }


            return true;
        }


        void updateTime(float x) {

            int hour = xToHour(x);


            if (selectedHandle == 1) {

                if (hour >= endTime)
                    hour = endTime - 1;

                if (hour < 0)
                    hour = 0;

                startTime = hour;
            }


            if (selectedHandle == 2) {

                if (hour <= startTime)
                    hour = startTime + 1;

                if (hour > 24)
                    hour = 24;

                endTime = hour;
            }


            invalidate();


            if (listener != null) {

                listener.onTimeChanged(
                        startTime,
                        endTime
                );
            }
        }


        int dp(int value) {

            return (int)(
                    value *
                            getResources()
                                    .getDisplayMetrics()
                                    .density
            );
        }
    }


    // =====================================================
    // 앱 시작
    // =====================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState
        );


        setContentView(
                R.layout.activity_main
        );


        calendarView =
                findViewById(
                        R.id.calendarView
                );


        dateText =
                findViewById(
                        R.id.dateText
                );


        planContainer =
                findViewById(
                        R.id.planContainer
                );


        addButton =
                findViewById(
                        R.id.addButton
                );


        // =============================================
        // 오늘 날짜
        // =============================================

        Calendar calendar =
                Calendar.getInstance();


        int year =
                calendar.get(
                        Calendar.YEAR
                );


        int month =
                calendar.get(
                        Calendar.MONTH
                ) + 1;


        int day =
                calendar.get(
                        Calendar.DAY_OF_MONTH
                );


        selectedDate =
                makeDate(
                        year,
                        month,
                        day
                );


        showPlans();


        // =============================================
        // 날짜 클릭
        // =============================================

        calendarView.setOnDateChangeListener(
                (view,
                 year1,
                 month1,
                 dayOfMonth) -> {

                    selectedDate =
                            makeDate(
                                    year1,
                                    month1 + 1,
                                    dayOfMonth
                            );


                    showPlans();
                }
        );


        // =============================================
        // 계획 추가
        // =============================================

        addButton.setOnClickListener(
                v -> showAddDialog()
        );
    }


    // =====================================================
    // 날짜 만들기
    // =====================================================

    String makeDate(
            int year,
            int month,
            int day) {

        return year
                + "-"
                + String.format(
                "%02d",
                month
        )
                + "-"
                + String.format(
                "%02d",
                day
        );
    }


    // =====================================================
    // 계획 추가
    // =====================================================

    void showAddDialog() {

        LinearLayout layout =
                new LinearLayout(this);


        layout.setOrientation(
                LinearLayout.VERTICAL
        );


        layout.setPadding(
                30,
                10,
                30,
                10
        );


        // =============================================
        // 현재 시간 표시
        // =============================================

        TextView timeText =
                new TextView(this);


        timeText.setText(
                "12 AM ~ 1 AM"
        );


        timeText.setTextSize(
                18
        );


        timeText.setGravity(
                Gravity.CENTER
        );


        timeText.setPadding(
                10,
                10,
                10,
                15
        );


        layout.addView(
                timeText
        );


        // =============================================
        // 가로 스크롤
        // =============================================

        HorizontalScrollView scroll =
                new HorizontalScrollView(
                        this
                );
        scroll.post(() -> {
            scroll.scrollTo(dp(12 * 70), 0);
        });


        scroll.setHorizontalScrollBarEnabled(
                true
        );


        // 선택된 시간
        final int[] selectedStart =
                {0};


        final int[] selectedEnd =
                {1};


        // =============================================
        // 타임라인
        // =============================================

        TimelineView timeline =
                new TimelineView(
                        0,
                        1,
                        (start, end) -> {

                            selectedStart[0] =
                                    start;


                            selectedEnd[0] =
                                    end;


                            timeText.setText(
                                    timeToString(start)
                                            + " ~ "
                                            + timeToString(end)
                            );
                        }
                );
        timeline.setBackgroundColor(Color.WHITE);

        timeline.post(() -> {
            android.util.Log.d(
                    "TIMELINE_DEBUG",
                    "Timeline 크기: "
                            + timeline.getWidth()
                            + " x "
                            + timeline.getHeight()
            );
        });


        scroll.addView(
                timeline,
                new LinearLayout.LayoutParams(
                        dp(24 * 10),
                        dp(100)
                )
        );


        layout.addView(
                scroll
        );


        // =============================================
        // 계획 입력
        // =============================================

        EditText input =
                new EditText(this);


        input.setHint(
                "계획을 입력하세요"
        );


        input.setTextSize(
                18
        );


        input.setSingleLine(
                true
        );


        input.setPadding(
                10,
                20,
                10,
                10
        );


        layout.addView(
                input
        );


        // =============================================
        // 다이얼로그
        // =============================================

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "계획 추가"
                        )
                        .setView(
                                layout
                        )
                        .setPositiveButton(
                                "추가",
                                null
                        )
                        .setNegativeButton(
                                "취소",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                d -> {

                    Button positive =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );


                    positive.setOnClickListener(
                            v -> {

                                String name =
                                        input.getText()
                                                .toString()
                                                .trim();


                                if (name.isEmpty()) {

                                    return;
                                }


                                if (selectedStart[0]
                                        >= selectedEnd[0]) {

                                    return;
                                }


                                if (!plans.containsKey(
                                        selectedDate)) {

                                    plans.put(
                                            selectedDate,
                                            new ArrayList<>()
                                    );
                                }


                                plans.get(
                                        selectedDate
                                ).add(
                                        new Plan(
                                                name,
                                                selectedStart[0],
                                                selectedEnd[0]
                                        )
                                );


                                showPlans();


                                dialog.dismiss();
                            }
                    );
                }
        );


        dialog.show();
    }


    // =====================================================
    // 계획 표시
    // =====================================================

    void showPlans() {

        planContainer.removeAllViews();


        String[] date =
                selectedDate.split("-");


        String displayDate =
                date[0]
                        + "년 "
                        + Integer.parseInt(
                        date[1]
                )
                        + "월 "
                        + Integer.parseInt(
                        date[2]
                )
                        + "일";


        dateText.setText(
                displayDate
                        + "의 계획"
        );


        ArrayList<Plan> currentPlans =
                plans.get(
                        selectedDate
                );


        // =============================================
        // 계획 없음
        // =============================================

        if (currentPlans == null ||
                currentPlans.isEmpty()) {

            TextView empty =
                    new TextView(this);


            empty.setText(
                    "계획이 없습니다."
            );


            empty.setTextSize(
                    18
            );


            empty.setPadding(
                    10,
                    20,
                    10,
                    20
            );


            planContainer.addView(
                    empty
            );


            return;
        }


        // =============================================
        // 계획 하나씩 표시
        // =============================================

        for (int i = 0;
             i < currentPlans.size();
             i++) {

            int index = i;


            Plan plan =
                    currentPlans.get(i);


            LinearLayout box =
                    new LinearLayout(this);


            box.setOrientation(
                    LinearLayout.VERTICAL
            );


            box.setPadding(
                    5,
                    15,
                    5,
                    15
            );


            // =========================================
            // 계획 이름
            // =========================================

            TextView name =
                    new TextView(this);


            name.setText(
                    (i + 1)
                            + ". "
                            + plan.name
            );


            name.setTextSize(
                    19
            );


            name.setTextColor(
                    Color.BLACK
            );


            box.addView(
                    name
            );


            // =========================================
            // 시간
            // =========================================

            TextView time =
                    new TextView(this);


            time.setText(
                    timeToString(
                            plan.startTime
                    )
                            + " ~ "
                            + timeToString(
                            plan.endTime
                    )
            );


            time.setTextSize(
                    15
            );


            time.setGravity(
                    Gravity.CENTER
            );


            time.setPadding(
                    5,
                    5,
                    5,
                    5
            );


            box.addView(
                    time
            );


            // =========================================
            // 타임라인
            // =========================================

            HorizontalScrollView timelineScroll =
                    new HorizontalScrollView(
                            this
                    );


            TimelineView timeline =
                    new TimelineView(
                            plan.startTime,
                            plan.endTime,
                            null
                    );


            timelineScroll.addView(
                    timeline,
                    new LinearLayout.LayoutParams(
                            dp(24 * 10),
                            dp(100)
                    )
            );


            box.addView(
                    timelineScroll
            );


            // =========================================
            // 수정 / 삭제 버튼
            // =========================================

            LinearLayout buttons =
                    new LinearLayout(this);


            buttons.setGravity(
                    Gravity.RIGHT
            );


            Button edit =
                    new Button(this);


            edit.setText(
                    "수정"
            );


            Button delete =
                    new Button(this);


            delete.setText(
                    "삭제"
            );


            buttons.addView(
                    edit
            );


            buttons.addView(
                    delete
            );


            box.addView(
                    buttons
            );


            planContainer.addView(
                    box
            );


            // =========================================
            // 삭제
            // =========================================

            delete.setOnClickListener(
                    v -> {

                        currentPlans.remove(
                                index
                        );


                        showPlans();
                    }
            );


            // =========================================
            // 수정
            // =========================================

            edit.setOnClickListener(
                    v -> {

                        showEditDialog(
                                plan
                        );
                    }
            );
        }
    }


    // =====================================================
    // 계획 수정
    // =====================================================

    void showEditDialog(
            Plan plan) {

        LinearLayout layout =
                new LinearLayout(this);


        layout.setOrientation(
                LinearLayout.VERTICAL
        );


        layout.setPadding(
                30,
                10,
                30,
                10
        );


        // =============================================
        // 시간 표시
        // =============================================

        TextView timeText =
                new TextView(this);


        timeText.setText(
                timeToString(
                        plan.startTime
                )
                        + " ~ "
                        + timeToString(
                        plan.endTime
                )
        );


        timeText.setTextSize(
                18
        );


        timeText.setGravity(
                Gravity.CENTER
        );


        timeText.setPadding(
                10,
                10,
                10,
                15
        );


        layout.addView(
                timeText
        );


        // =============================================
        // 가로 스크롤
        // =============================================

        HorizontalScrollView scroll =
                new HorizontalScrollView(
                        this
                );


        final int[] selectedStart =
                {plan.startTime};


        final int[] selectedEnd =
                {plan.endTime};


        // =============================================
        // 타임라인
        // =============================================

        TimelineView timeline =
                new TimelineView(
                        plan.startTime,
                        plan.endTime,
                        (start, end) -> {

                            selectedStart[0] =
                                    start;


                            selectedEnd[0] =
                                    end;


                            timeText.setText(
                                    timeToString(start)
                                            + " ~ "
                                            + timeToString(end)
                            );
                        }
                );


        scroll.addView(
                timeline,
                new HorizontalScrollView.LayoutParams(
                        dp(24 * 10),
                        dp(100)
                )
        );


        layout.addView(
                scroll
        );


        // =============================================
        // 계획 이름
        // =============================================

        EditText input =
                new EditText(this);


        input.setText(
                plan.name
        );


        input.setSingleLine(
                true
        );


        input.setTextSize(
                18
        );


        input.setPadding(
                10,
                20,
                10,
                10
        );


        layout.addView(
                input
        );


        // =============================================
        // 다이얼로그
        // =============================================

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "계획 수정"
                        )
                        .setView(
                                layout
                        )
                        .setPositiveButton(
                                "수정",
                                null
                        )
                        .setNegativeButton(
                                "취소",
                                null
                        )
                        .create();


        dialog.setOnShowListener(
                d -> {

                    Button positive =
                            dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                            );


                    positive.setOnClickListener(
                            v -> {

                                String newName =
                                        input.getText()
                                                .toString()
                                                .trim();


                                if (newName.isEmpty()) {

                                    return;
                                }


                                plan.name =
                                        newName;


                                plan.startTime =
                                        selectedStart[0];


                                plan.endTime =
                                        selectedEnd[0];


                                showPlans();


                                dialog.dismiss();
                            }
                    );
                }
        );


        dialog.show();
    }


    // =====================================================
    // 시간 문자열
    // =====================================================

    String timeToString(
            int hour) {

        if (hour == 0) {

            return "12 AM";
        }


        if (hour < 12) {

            return hour + " AM";
        }


        if (hour == 12) {

            return "12 PM";
        }


        if (hour < 24) {

            return (hour - 12)
                    + " PM";
        }


        return "12 AM";
    }


    // =====================================================
    // dp
    // =====================================================

    int dp(int value) {

        return (int)
                (
                        value *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );
    }
}
