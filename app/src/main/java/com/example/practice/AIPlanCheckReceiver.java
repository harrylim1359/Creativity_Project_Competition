package com.example.practice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIPlanCheckReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID =
            "ai_plan_check";

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        PendingResult pendingResult =
                goAsync();

        Context appContext =
                context.getApplicationContext();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        executor.execute(() -> {

            try {

                String date =
                        intent.getStringExtra("date");

                String time =
                        intent.getStringExtra("time");

                String title =
                        intent.getStringExtra("title");

                if (date == null
                        || time == null
                        || title == null) {

                    return;
                }

                /*
                 * Gemini 생성
                 */

                GeminiManager geminiManager =
                        new GeminiManager(
                                GeminiConfig.API_KEY,
                                appContext
                        );

                /*
                 * AI에게 일정 점검 요청
                 */

                String result =
                        geminiManager.checkUpcomingPlan(
                                date,
                                time,
                                title
                        );

                if (result == null
                        || result.trim().isEmpty()) {

                    result =
                            "일정 점검 결과를 가져오지 못했습니다.";
                }

                /*
                 * 사용자에게 알림
                 */

                showNotification(
                        appContext,
                        title,
                        result
                );

            } catch (Exception e) {

                e.printStackTrace();

            } finally {

                pendingResult.finish();

                executor.shutdown();
            }
        });
    }

    private void showNotification(
            Context context,
            String planTitle,
            String message
    ) {

        NotificationManager manager =
                (NotificationManager)
                        context.getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        /*
         * Android 8.0 이상
         */

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "AI 일정 점검",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "일정 시작 전 AI가 일정을 점검합니다."
            );

            manager.createNotificationChannel(
                    channel
            );
        }

        /*
         * 알림을 눌렀을 때 일정 화면으로 이동
         */

        Intent openIntent =
                new Intent(
                        context,
                        PlanActivity.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        /*
         * 알림 생성
         */

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        context,
                        CHANNEL_ID
                )
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(
                                "AI 일정 점검"
                        )
                        .setContentText(
                                planTitle
                        )
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(message)
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_HIGH
                        )
                        .setAutoCancel(true)
                        .setContentIntent(
                                pendingIntent
                        );

        /*
         * Android 13 이상 알림 권한 확인
         */

        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            manager.notify(
                    (int) System.currentTimeMillis(),
                    builder.build()
            );
        }
    }
}