package com.example.practice;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.content.Intent;

public class PlanManager {

    private static final String PREF_NAME = "plans";
    private static final String KEY_PLANS = "plan_list";

    private final SharedPreferences preferences;
    private final Context context;

    public PlanManager(Context context) {

        this.context = context.getApplicationContext();

        preferences = this.context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // 일정 추가
    public void addPlan(
            String date,
            String time,
            String title
    ) {

        List<Plan> plans = getAllPlans();

        String id = UUID.randomUUID().toString();

        Plan plan = new Plan(
                id,
                date,
                time,
                title
        );

        plans.add(plan);

        savePlans(plans);

        scheduleNotification(
                plan
        );
    }

    // 모든 일정 가져오기
    public List<Plan> getAllPlans() {

        List<Plan> plans = new ArrayList<>();

        String jsonString =
                preferences.getString(KEY_PLANS, "[]");

        try {

            JSONArray jsonArray =
                    new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject object =
                        jsonArray.getJSONObject(i);

                String id =
                        object.getString("id");

                String date =
                        object.getString("date");

                String time =
                        object.getString("time");

                String title =
                        object.getString("title");

                Plan plan =
                        new Plan(
                                id,
                                date,
                                time,
                                title
                        );

                plans.add(plan);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return plans;
    }

    // 특정 날짜의 일정 가져오기
    public List<Plan> getPlansByDate(String date) {

        List<Plan> result = new ArrayList<>();

        List<Plan> plans = getAllPlans();

        for (Plan plan : plans) {

            if (plan.getDate().equals(date)) {
                result.add(plan);
            }
        }

        return result;
    }

    // 일정 삭제
    public void deletePlan(String id) {

        List<Plan> plans = getAllPlans();

        for (int i = plans.size() - 1; i >= 0; i--) {

            if (plans.get(i).getId().equals(id)) {

                cancelNotifications(
                        plans.get(i)
                );

                plans.remove(i);
            }
        }

        savePlans(plans);
    }

    // 일정 전체 저장
    private void savePlans(List<Plan> plans) {

        JSONArray jsonArray =
                new JSONArray();

        try {

            for (Plan plan : plans) {

                JSONObject object =
                        new JSONObject();

                object.put(
                        "id",
                        plan.getId()
                );

                object.put(
                        "date",
                        plan.getDate()
                );

                object.put(
                        "time",
                        plan.getTime()
                );

                object.put(
                        "title",
                        plan.getTitle()
                );

                jsonArray.put(object);
            }

        } catch (Exception e) {

            e.printStackTrace();
            return;
        }

        preferences.edit()
                .putString(
                        KEY_PLANS,
                        jsonArray.toString()
                )
                .apply();
    }

    // 특정 날짜의 일정을 AI가 읽을 수 있는 문자열로 반환
    public String getPlansForAI(String date) {

        List<Plan> plans = getPlansByDate(date);

        if (plans.isEmpty()) {
            return date + "에는 등록된 일정이 없습니다.";
        }

        StringBuilder result = new StringBuilder();

        result.append(date).append("의 일정:\n");

        for (Plan plan : plans) {

            result.append("- ")
                    .append(plan.getTime())
                    .append(" : ")
                    .append(plan.getTitle())
                    .append("\n");
        }

        return result.toString();
    }
    private void scheduleNotification(Plan plan) {

        try {

            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                    );

            java.util.Date date =
                    format.parse(
                            plan.getDate()
                                    + " "
                                    + plan.getTime()
                    );

            if (date == null) {
                return;
            }

            long triggerTime =
                    date.getTime();

            android.app.AlarmManager alarmManager =
                    (android.app.AlarmManager)
                            context.getSystemService(
                                    Context.ALARM_SERVICE
                            );

            /*
             * ========================================
             * 1. 기존 일정 시간 알림
             * ========================================
             */

            if (triggerTime > System.currentTimeMillis()) {

                Intent intent =
                        new Intent(
                                context,
                                PlanNotificationReceiver.class
                        );

                intent.putExtra(
                        "title",
                        plan.getTitle()
                );

                intent.putExtra(
                        "time",
                        plan.getTime()
                );

                android.app.PendingIntent pendingIntent =
                        android.app.PendingIntent.getBroadcast(
                                context,
                                plan.getId().hashCode(),
                                intent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                        | android.app.PendingIntent.FLAG_IMMUTABLE
                        );

                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            /*
             * ========================================
             * 2. 일정 1시간 전 AI 점검 알림
             * ========================================
             */

            long aiTriggerTime =
                    triggerTime
                            - (60L * 60L * 1000L);

            if (aiTriggerTime > System.currentTimeMillis()) {

                Intent aiIntent =
                        new Intent(
                                context,
                                AIPlanCheckReceiver.class
                        );

                aiIntent.putExtra(
                        "id",
                        plan.getId()
                );

                aiIntent.putExtra(
                        "date",
                        plan.getDate()
                );

                aiIntent.putExtra(
                        "time",
                        plan.getTime()
                );

                aiIntent.putExtra(
                        "title",
                        plan.getTitle()
                );

                android.app.PendingIntent aiPendingIntent =
                        android.app.PendingIntent.getBroadcast(
                                context,
                                (plan.getId() + "_ai").hashCode(),
                                aiIntent,
                                android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                        | android.app.PendingIntent.FLAG_IMMUTABLE
                        );

                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        aiTriggerTime,
                        aiPendingIntent
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    private void cancelNotifications(
            Plan plan
    ) {

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager)
                        context.getSystemService(
                                Context.ALARM_SERVICE
                        );

        /*
         * 기존 일정 알림 취소
         */

        Intent normalIntent =
                new Intent(
                        context,
                        PlanNotificationReceiver.class
                );

        android.app.PendingIntent normalPendingIntent =
                android.app.PendingIntent.getBroadcast(
                        context,
                        plan.getId().hashCode(),
                        normalIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.cancel(
                normalPendingIntent
        );

        /*
         * AI 점검 알림 취소
         */

        Intent aiIntent =
                new Intent(
                        context,
                        AIPlanCheckReceiver.class
                );

        android.app.PendingIntent aiPendingIntent =
                android.app.PendingIntent.getBroadcast(
                        context,
                        (plan.getId() + "_ai").hashCode(),
                        aiIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

        alarmManager.cancel(
                aiPendingIntent
        );
    }
}