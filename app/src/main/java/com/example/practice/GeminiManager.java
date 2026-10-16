package com.example.practice;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;
import com.google.genai.types.Type;
import com.google.genai.types.GoogleSearch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import android.os.SystemClock;

public class GeminiManager {

    private final Client client;

    private final PlanManager planManager;
    private final ContextManager contextManager;
    private static final String DEBUG_TAG = "GeminiDebug";

    public GeminiManager(
            String apiKey,
            android.content.Context context
    ) {

        client = Client.builder()
                .apiKey(apiKey)
                .build();

        planManager = new PlanManager(context);
        contextManager = new ContextManager(context);
    }

    public String checkUpcomingPlan(
            String date,
            String time,
            String title
    ) {

        String savedContext =
                contextManager.getContextForGemini();

        String prompt =
                "[선제 일정 점검]\n"
                        + "현재 사용자의 일정이 시작되기 1시간 전이다.\n\n"

                        + "[일정]\n"
                        + "날짜: "
                        + date
                        + "\n"
                        + "시간: "
                        + time
                        + "\n"
                        + "내용: "
                        + title
                        + "\n\n"

                        + "[판단 원칙]\n"
                        + "1. 사용자가 이 일정을 그대로 진행해도 괜찮은지 판단한다.\n"
                        + "2. 일정에 영향을 줄 수 있는 외부 정보가 필요하다면 "
                        + "날씨 또는 Google 검색 도구를 사용하여 확인한다.\n"
                        + "3. 확인하지 않은 정보를 사실이라고 가정하지 않는다.\n"
                        + "4. 문제가 없다면 문제가 없다고 간단하게 알려준다.\n"
                        + "5. 문제가 예상된다면 무엇이 문제인지 설명한다.\n"
                        + "6. 문제의 심각성을 과장하지 않는다.\n"
                        + "7. 일정을 임의로 변경하거나 취소하지 않는다.\n"
                        + "8. 판단하기에 정보가 부족하다면 그 사실을 알려준다.\n"
                        + "9. 사용자에게 보여줄 알림이므로 너무 길게 작성하지 않는다.\n\n";

        if (savedContext != null
                && !savedContext.isEmpty()) {

            prompt +=
                    savedContext
                            + "\n\n";
        }

        prompt +=
                "위 일정을 그대로 진행해도 괜찮은지 판단하고 "
                        + "사용자에게 보여줄 짧고 명확한 알림 문장을 작성해라.";

        return sendMessage(prompt);
    }
    //*
    public String sendMessage(String message) {
        String savedContext =
                contextManager.getContextForGemini();

        String aiInstructions =
                "[AI 응답 원칙]\n"
                        + "1. 사용자의 요청에 필요한 정보가 부족하거나 여러 가지로 해석될 수 있다면 추측하지 말고 먼저 되묻는다.\n"
                        + "2. 사용자가 무엇을 원하는지 충분히 명확해질 때까지 필요한 질문을 한다.\n"
                        + "3. 질문에 필요한 정보가 이미 충분하다면 불필요하게 되묻지 않는다.\n"
                        + "4. 사용자의 말에 모순되거나 논리적으로 문제가 있는 부분이 있으면 이를 지적하고 확인한다.\n"
                        + "5. 사용자의 전제를 무조건 사실이라고 가정하지 않는다. 사실 여부가 중요한 경우 적절한 도구를 사용하여 확인한다.\n"
                        + "6. 확실하지 않은 정보를 사실처럼 단정하지 않는다.\n"
                        + "7. 일정 관련 요청에서 날짜가 명확하지 않다면 날짜를 먼저 확인한다.\n"
                        + "8. 일정 추가 요청에서 날짜, 시간, 내용 중 필요한 정보가 부족하다면 부족한 정보를 질문한다.\n"
                        + "9. 사용자가 단순히 질문한 경우에도 필요한 경우 이유와 근거를 확인하여 논리적으로 답한다.\n"
                        + "10. 사용자가 제공한 정보와 이전에 저장된 정보가 서로 충돌한다면 그 사실을 알려주고 어느 정보가 맞는지 확인한다.\n";

        String finalMessage =
                aiInstructions
                        + "\n";

        if (!savedContext.isEmpty()) {
            finalMessage +=
                    savedContext
                            + "\n";
        }

        finalMessage +=
                "\n[현재 사용자 질문]\n"
                        + message;

        Tool appTool = Tool.builder()
                .functionDeclarations(
                        Arrays.asList(
                                createWeatherFunction(),
                                createAddPlanFunction(),
                                createGetPlansFunction(),
                                createSaveContextFunction()
                        )
                )
                .build();

        // Google 검색 도구
        Tool googleSearchTool = Tool.builder()
                .googleSearch(GoogleSearch.builder().build())
                .build();

        // 기존 함수 호출과 Google 검색을 함께 등록
        GenerateContentConfig config = GenerateContentConfig.builder()
                .tools(Arrays.asList(appTool))
                .build();


        long debugStart = SystemClock.elapsedRealtime();

        Log.i(DEBUG_TAG, "[1] 최초 Gemini 요청 시작");

        Log.e("GeminiDebug", "========== GEMINI API REQUEST #1 ==========");
        Log.e("GeminiDebug", "Model = gemini-3.6-flash");
        Log.e("GeminiDebug", "Time = " + System.currentTimeMillis());

        GenerateContentResponse response =
                client.models.generateContent(
                        "gemini-3.6-flash",
                        finalMessage,
                        config
                );

        Log.e("GeminiDebug", "========== GEMINI API RESPONSE #1 ==========");

        Log.i(
                DEBUG_TAG,
                "[2] 최초 Gemini 응답 완료. 소요 시간(ms)="
                        + (SystemClock.elapsedRealtime() - debugStart)
        );

        List<FunctionCall> functionCalls =
                response.functionCalls();

        // Gemini 최초 응답의 도구 호출 내역 기록
        if (functionCalls == null || functionCalls.isEmpty()) {

            Log.i(
                    "GeminiDebug",
                    "[ToolDebug] 도구 호출 없음 - 일반 응답"
            );

        } else {

            Log.i(
                    "GeminiDebug",
                    "[ToolDebug] 도구 호출 개수 = "
                            + functionCalls.size()
            );

            for (FunctionCall call : functionCalls) {

                Log.i(
                        "GeminiDebug",
                        "[ToolDebug] 호출 함수 = "
                                + call.name().orElse("unknown")
                );
            }
        }

        // 일반적인 질문
        if (functionCalls == null || functionCalls.isEmpty()) {

            String text = response.text();

            if (text == null) {
                return "응답을 받지 못했습니다.";
            }

            return text;
        }

        // Gemini가 함수를 호출한 경우
        for (FunctionCall functionCall : functionCalls) {

            String functionName =
                    functionCall.name().orElse("");

            // 날씨 함수
            if (functionName.equals("get_weather")) {

                Map<String, Object> args =
                        functionCall.args().orElse(
                                new HashMap<>()
                        );

                Object locationObject =
                        args.get("location");

                if (locationObject == null) {
                    return "날씨를 확인할 지역을 알 수 없습니다.";
                }

                String location =
                        locationObject.toString();

                return getWeatherAndAskGeminiAgain(
                        message,
                        response,
                        functionCall,
                        location,
                        config
                );
            }

            // 일정 추가 함수
            if (functionName.equals("add_plan")) {

                Map<String, Object> args =
                        functionCall.args().orElse(
                                new HashMap<>()
                        );

                String date =
                        String.valueOf(args.get("date"));

                String time =
                        String.valueOf(args.get("time"));

                String title =
                        String.valueOf(args.get("title"));

                Log.i(
                        DEBUG_TAG,
                        "[4] 일정 저장 시작: "
                                + date + " " + time + " / " + title
                );

                planManager.addPlan(date, time, title);

                Log.i(DEBUG_TAG, "[5] 일정 저장 완료");

                String result =
                        date + " " + time
                                + "에 '" + title
                                + "' 일정을 등록했습니다.";

                return executeFunctionAndAskGeminiAgain(
                        message,
                        response,
                        "add_plan",
                        result,
                        config
                );
            }

            // 중요 정보 저장 함수
            if (functionName.equals("save_context")) {

                Map<String, Object> args =
                        functionCall.args().orElse(
                                new HashMap<>()
                        );

                Object contextObject =
                        args.get("context");

                if (contextObject == null) {
                    return "저장할 정보를 찾지 못했습니다.";
                }

                String contextText =
                        contextObject.toString();

                Log.i(
                        DEBUG_TAG,
                        "[Context] 중요 정보 저장: "
                                + contextText
                );

                contextManager.addContext(contextText);

                String result =
                        "사용자의 중요 정보를 기억했습니다.";

                return executeFunctionAndAskGeminiAgain(
                        message,
                        response,
                        "save_context",
                        result,
                        config
                );
            }

            if (functionName.equals("get_plans")) {

                Map<String, Object> args =
                        functionCall.args().orElse(
                                new HashMap<>()
                        );

                Object dateObject =
                        args.get("date");

                if (dateObject == null) {
                    return "조회할 날짜를 알 수 없습니다.";
                }

                String date =
                        dateObject.toString();

                String result =
                        planManager.getPlansForAI(date);

                return executeFunctionAndAskGeminiAgain(
                        message,
                        response,
                        "get_plans",
                        result,
                        config
                );
            }
        }

        return response.text();
    }
    //*/
    /*
    public String sendMessage(String message) {

        long startTime = System.currentTimeMillis();

        android.util.Log.e(
                "GeminiDebug",
                "========================================"
        );

        android.util.Log.e(
                "GeminiDebug",
                "REQUEST START"
        );

        android.util.Log.e(
                "GeminiDebug",
                "Time = " + startTime
        );

        android.util.Log.e(
                "GeminiDebug",
                "Model = gemini-3.6-flash"
        );

        android.util.Log.e(
                "GeminiDebug",
                "Message length = " + message.length()
        );

        android.util.Log.e(
                "GeminiDebug",
                "========================================"
        );

        try {

            long apiStart = System.currentTimeMillis();

            android.util.Log.e(
                    "GeminiDebug",
                    "Calling generateContent()..."
            );

            // 여기부터는 네 기존 generateContent 코드 그대로
            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-3.6-flash",
                            message,
                            null
                    );

            long apiEnd = System.currentTimeMillis();

            android.util.Log.e(
                    "GeminiDebug",
                    "========================================"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "REQUEST SUCCESS"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "API time = " + (apiEnd - apiStart) + " ms"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Total time = "
                            + (apiEnd - startTime)
                            + " ms"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Response = "
                            + response.text()
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "========================================"
            );

            return response.text();

        } catch (Exception e) {

            long errorTime = System.currentTimeMillis();

            android.util.Log.e(
                    "GeminiDebug",
                    "========================================"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "REQUEST FAILED"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Error time = " + errorTime
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Elapsed = "
                            + (errorTime - startTime)
                            + " ms"
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Exception class = "
                            + e.getClass().getName()
            );

            android.util.Log.e(
                    "GeminiDebug",
                    "Exception message = "
                            + e.getMessage()
            );

            Throwable cause = e.getCause();

            if (cause != null) {
                android.util.Log.e(
                        "GeminiDebug",
                        "Cause class = "
                                + cause.getClass().getName()
                );

                android.util.Log.e(
                        "GeminiDebug",
                        "Cause message = "
                                + cause.getMessage()
                );
            }

            android.util.Log.e(
                    "GeminiDebug",
                    "========================================");

            throw e;
        }
    }
    */

    private String getWeatherAndAskGeminiAgain(
            String originalMessage,
            GenerateContentResponse firstResponse,
            FunctionCall functionCall,
            String location,
            GenerateContentConfig config
    ) {

        final String[] result = new String[1];
        final Exception[] error = new Exception[1];

        // WeatherManager가 비동기로 동작하므로
        // 여기서는 완료될 때까지 기다린다.
        final Object lock = new Object();

        WeatherManager.getWeatherForAI(
                location,
                new WeatherManager.WeatherCallback() {

                    @Override
                    public void onSuccess(String weatherResult) {

                        synchronized (lock) {

                            try {

                                Part functionResponsePart =
                                        Part.fromFunctionResponse(
                                                "get_weather",
                                                Map.of(
                                                        "output",
                                                        weatherResult
                                                )
                                        );

                                Content functionResponseContent =
                                        Content.fromParts(
                                                functionResponsePart
                                        );
                                Log.e("GeminiDebug", "========== GEMINI API REQUEST #2 ==========");
                                Log.e("GeminiDebug", "Model = gemini-3.6-flash");
                                Log.e("GeminiDebug", "Time = " + System.currentTimeMillis());

                                GenerateContentResponse finalResponse =
                                        client.models.generateContent(
                                                "gemini-3.6-flash",
                                                Arrays.asList(
                                                        Content.fromParts(
                                                                Part.fromText(
                                                                        originalMessage
                                                                )
                                                        ),
                                                        Content.builder()
                                                                .role("model")
                                                                .parts(
                                                                        firstResponse.parts()
                                                                )
                                                                .build(),
                                                        functionResponseContent
                                                ),
                                                config
                                        );
                                Log.e("GeminiDebug", "========== GEMINI API RESPONSE #2 ==========");

                                result[0] =
                                        finalResponse.text();

                            } catch (Exception e) {
                                error[0] = e;

                                Log.e("GeminiDebug", "========== GEMINI EXCEPTION #2 ==========");
                                Log.e("GeminiDebug", "Class: " + e.getClass().getName());
                                Log.e("GeminiDebug", "Message: " + e.getMessage());
                                Log.e("GeminiDebug", "Full exception:", e);
                            }

                            lock.notify();
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {

                        synchronized (lock) {

                            result[0] =
                                    "날씨 정보를 가져오지 못했습니다.\n"
                                            + errorMessage;

                            lock.notify();
                        }
                    }
                }
        );

        synchronized (lock) {

            try {

                while (result[0] == null && error[0] == null) {
                    lock.wait();
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return "날씨 정보를 가져오는 중 오류가 발생했습니다.";
            }
        }

        if (error[0] != null) {

            Log.e("GeminiDebug", "========== ERROR RETURN ==========");
            Log.e("GeminiDebug", "Class: " + error[0].getClass().getName());
            Log.e("GeminiDebug", "Message: " + error[0].getMessage());
            Log.e("GeminiDebug", "Full exception:", error[0]);

            return "Gemini 처리 중 오류가 발생했습니다.\n"
                    + error[0].getMessage();
        }

        return result[0];
    }

    private String executeFunctionAndAskGeminiAgain(
            String message,
            GenerateContentResponse response,
            String functionName,
            String functionResult,
            GenerateContentConfig config
    ) {
        try {
            // 최초 사용자 메시지
            Content userContent =
                    Content.fromParts(
                            Part.fromText(message)
                    );

            // Gemini가 함수를 호출했던 응답
            Content modelContent =
                    response.candidates()
                            .orElseThrow(
                                    () -> new Exception("Gemini 응답이 없습니다.")
                            )
                            .get(0)
                            .content()
                            .orElseThrow(
                                    () -> new Exception("응답 내용이 없습니다.")
                            );

            // Android에서 실행한 함수의 결과
            Content functionResponseContent =
                    Content.fromParts(
                            Part.fromFunctionResponse(
                                    functionName,
                                    Map.of("output", functionResult)
                            )
                    );

            // 대화와 함수 실행 결과를 Gemini에 다시 전달
            List<Content> contents =
                    Arrays.asList(
                            userContent,
                            modelContent,
                            functionResponseContent
                    );

            long debugStart2 = SystemClock.elapsedRealtime();

            Log.i(
                    DEBUG_TAG,
                    "[6] 함수 결과를 Gemini에 재전달 시작: "
                            + functionName
            );
            Log.e("GeminiDebug", "========== GEMINI API REQUEST #3 ==========");
            Log.e("GeminiDebug", "Model = gemini-3.6-flash");
            Log.e("GeminiDebug", "Time = " + System.currentTimeMillis());

            GenerateContentResponse finalResponse =
                    client.models.generateContent(
                            "gemini-3.6-flash",
                            contents,
                            config
                    );
            Log.e("GeminiDebug", "========== GEMINI API RESPONSE #3 ==========");

            Log.i(
                    DEBUG_TAG,
                    "[7] Gemini 최종 응답 완료. 소요 시간(ms)="
                            + (SystemClock.elapsedRealtime() - debugStart2)
            );

            return finalResponse.text() != null
                    ? finalResponse.text()
                    : "함수 실행 결과를 바탕으로 답변을 생성하지 못했습니다.";

        } catch (Exception e) {
            Log.e("GeminiDebug", "========== GEMINI EXCEPTION ==========");
            Log.e("GeminiDebug", "Exception class: " + e.getClass().getName());
            Log.e("GeminiDebug", "Exception message: " + e.getMessage());

            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);

            Log.e("GeminiDebug", "FULL STACK TRACE:");
            Log.e("GeminiDebug", sw.toString());

            return "함수 실행 결과를 전달하는 중 오류가 발생했습니다.";
        }
    }

    private FunctionDeclaration createWeatherFunction() {

        return FunctionDeclaration.builder()
                .name("get_weather")
                .description(
                        "사용자가 특정 지역의 날씨 정보를 필요로 할 때 현재 날씨를 가져온다. "
                                + "location에는 영어 도시명을 사용한다. 예: Seoul, Busan, Daejeon"
                )
                .parameters(
                        Schema.builder()
                                .type(Type.Known.OBJECT)
                                .properties(
                                        Map.of(
                                                "location",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description("날씨를 확인할 지역 이름. 예: Seoul, Busan, Daejeon")
                                                        .build()
                                        )
                                )
                                .required(
                                        Arrays.asList("location")
                                )
                                .build()
                )
                .build();
    }

    private FunctionDeclaration createAddPlanFunction() {

        return FunctionDeclaration.builder()
                .name("add_plan")
                .description(
                        "사용자가 새로운 일정을 등록하고 싶을 때 사용한다. "
                                + "날짜, 시간, 일정 내용을 받아 일정으로 저장한다. "
                                + "날짜, 시간 또는 일정 내용 중 필요한 정보가 부족하면 "
                                + "함수를 호출하지 말고 먼저 부족한 정보를 질문한다."
                )
                .parameters(
                        Schema.builder()
                                .type(Type.Known.OBJECT)
                                .properties(
                                        Map.of(
                                                "date",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description(
                                                                "일정 날짜. yyyy-MM-dd 형식. "
                                                                        + "예: 2026-10-15"
                                                        )
                                                        .build(),

                                                "time",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description(
                                                                "일정 시간. HH:mm 형식. "
                                                                        + "예: 15:00"
                                                        )
                                                        .build(),

                                                "title",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description(
                                                                "일정의 내용. 예: 친구 만나기"
                                                        )
                                                        .build()
                                        )
                                )
                                .required(
                                        Arrays.asList(
                                                "date",
                                                "time",
                                                "title"
                                        )
                                )
                                .build()
                )
                .build();
    }

    private FunctionDeclaration createGetPlansFunction() {

        return FunctionDeclaration.builder()
                .name("get_plans")
                .description(
                        "사용자의 일정이 필요할 때 특정 날짜의 일정을 가져온다. "
                                + "오늘 일정, 내일 일정, 특정 날짜의 일정 등을 확인할 때 사용한다. "
                                + "사용자가 어떤 날짜의 일정인지 명확하게 말하지 않았다면 "
                                + "함수를 호출하지 말고 먼저 어느 날짜의 일정인지 질문한다."
                )
                .parameters(
                        Schema.builder()
                                .type(Type.Known.OBJECT)
                                .properties(
                                        Map.of(
                                                "date",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description(
                                                                "조회할 날짜. yyyy-MM-dd 형식. "
                                                                        + "예: 2026-10-15"
                                                        )
                                                        .build()
                                        )
                                )
                                .required(
                                        Arrays.asList("date")
                                )
                                .build()
                )
                .build();
    }

    private FunctionDeclaration createSaveContextFunction() {

        return FunctionDeclaration.builder()
                .name("save_context")
                .description(
                        "사용자가 앞으로의 대화에서도 기억할 가치가 있는 "
                                + "개인적인 정보, 선호, 관심사, 목표 등을 알려주었을 때 "
                                + "그 정보를 장기 기억에 저장한다. "
                                + "단순한 질문, 일시적인 정보, 현재 대화에서만 필요한 정보는 저장하지 않는다. "
                                + "일정 정보는 저장하지 않는다. 일정은 add_plan 함수를 사용한다."
                )
                .parameters(
                        Schema.builder()
                                .type(Type.Known.OBJECT)
                                .properties(
                                        Map.of(
                                                "context",
                                                Schema.builder()
                                                        .type(Type.Known.STRING)
                                                        .description(
                                                                "앞으로의 대화에서 기억해야 할 중요한 사용자 정보"
                                                        )
                                                        .build()
                                        )
                                )
                                .required(
                                        Arrays.asList("context")
                                )
                                .build()
                )
                .build();
    }
}