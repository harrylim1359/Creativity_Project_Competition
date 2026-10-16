package com.example.practice;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ContextManager {

    private static final String PREF_NAME = "ai_context";
    private static final String KEY_CONTEXTS = "contexts";

    private final SharedPreferences preferences;

    public ContextManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // 문맥 저장
    public void addContext(String contextText) {

        if (contextText == null || contextText.trim().isEmpty()) {
            return;
        }

        try {
            JSONArray contexts = getContextArray();

            // 중복 저장 방지
            for (int i = 0; i < contexts.length(); i++) {
                JSONObject item = contexts.getJSONObject(i);

                if (contextText.equals(item.getString("text"))) {
                    return;
                }
            }

            JSONObject newContext = new JSONObject();

            newContext.put("text", contextText);
            newContext.put(
                    "timestamp",
                    System.currentTimeMillis()
            );

            contexts.put(newContext);

            saveContextArray(contexts);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 모든 문맥 가져오기
    public List<String> getAllContexts() {

        List<String> result = new ArrayList<>();

        try {
            JSONArray contexts = getContextArray();

            for (int i = 0; i < contexts.length(); i++) {

                JSONObject item = contexts.getJSONObject(i);

                result.add(
                        item.getString("text")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // Gemini에게 전달하기 좋은 하나의 문자열로 반환
    public String getContextForGemini() {

        List<String> contexts = getAllContexts();

        if (contexts.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        result.append("[사용자가 이전 대화에서 알려준 중요 정보]\n");

        for (String context : contexts) {
            result.append("- ");
            result.append(context);
            result.append("\n");
        }

        return result.toString();
    }

    // 특정 문맥 삭제
    public void removeContext(String contextText) {

        try {
            JSONArray contexts = getContextArray();
            JSONArray newContexts = new JSONArray();

            for (int i = 0; i < contexts.length(); i++) {

                JSONObject item = contexts.getJSONObject(i);

                if (!contextText.equals(
                        item.getString("text"))) {

                    newContexts.put(item);
                }
            }

            saveContextArray(newContexts);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 모든 문맥 삭제
    public void clearContexts() {
        preferences.edit()
                .remove(KEY_CONTEXTS)
                .apply();
    }

    private JSONArray getContextArray() {

        String json = preferences.getString(
                KEY_CONTEXTS,
                "[]"
        );

        try {
            return new JSONArray(json);

        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void saveContextArray(JSONArray contexts) {

        preferences.edit()
                .putString(
                        KEY_CONTEXTS,
                        contexts.toString()
                )
                .apply();
    }
}