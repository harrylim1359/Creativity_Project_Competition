package com.example.practice;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class WeatherManager {

    public interface WeatherCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    public static void getWeather(
            double latitude,
            double longitude,
            WeatherCallback callback
    ) {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String urlString =
                        "https://api.open-meteo.com/v1/forecast"
                                + "?latitude=" + latitude
                                + "&longitude=" + longitude
                                + "&current="
                                + "temperature_2m,"
                                + "relative_humidity_2m,"
                                + "apparent_temperature,"
                                + "weather_code,"
                                + "wind_speed_10m"
                                + "&timezone=Asia%2FTokyo";

                URL url = new URL(urlString);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    callback.onError("날씨 서버 오류: " + responseCode);
                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json = new JSONObject(response.toString());

                JSONObject current = json.getJSONObject("current");

                double temperature =
                        current.getDouble("temperature_2m");

                int humidity =
                        current.getInt("relative_humidity_2m");

                double apparentTemperature =
                        current.getDouble("apparent_temperature");

                int weatherCode =
                        current.getInt("weather_code");

                double windSpeed =
                        current.getDouble("wind_speed_10m");

                String weatherDescription =
                        getWeatherDescription(weatherCode);

                String result =
                        "현재 날씨: " + weatherDescription
                                + "\n기온: " + temperature + "°C"
                                + "\n체감온도: " + apparentTemperature + "°C"
                                + "\n습도: " + humidity + "%"
                                + "\n풍속: " + windSpeed + " km/h";

                callback.onSuccess(result);

            } catch (Exception e) {

                callback.onError(
                        "날씨 정보를 가져오지 못했습니다.\n"
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    private static String getWeatherDescription(int code) {

        if (code == 0) {
            return "맑음";
        }

        if (code == 1 || code == 2) {
            return "대체로 맑음";
        }

        if (code == 3) {
            return "흐림";
        }

        if (code == 45 || code == 48) {
            return "안개";
        }

        if (code >= 51 && code <= 57) {
            return "이슬비";
        }

        if (code >= 61 && code <= 67) {
            return "비";
        }

        if (code >= 71 && code <= 77) {
            return "눈";
        }

        if (code >= 80 && code <= 82) {
            return "소나기";
        }

        if (code >= 95 && code <= 99) {
            return "뇌우";
        }

        return "알 수 없음";
    }
    public static void getWeatherForAI(
            String location,
            WeatherCallback callback
    ) {
        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                String encodedLocation =
                        URLEncoder.encode(location, "UTF-8");

                String urlString =
                        "https://geocoding-api.open-meteo.com/v1/search"
                                + "?name=" + encodedLocation
                                + "&count=1"
                                + "&format=json";

                URL url = new URL(urlString);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    callback.onError("지역 검색 오류: " + responseCode);
                    return;
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        connection.getInputStream()
                                )
                        );

                StringBuilder response = new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                JSONObject json =
                        new JSONObject(response.toString());

                System.out.println("GEOCODING RESPONSE = " + json);

                System.out.println("GEOCODING URL = " + urlString);

                if (!json.has("results")) {
                    callback.onError(
                            "'" + location + "' 지역을 찾을 수 없습니다."
                    );
                    return;
                }

                JSONArray results =
                        json.getJSONArray("results");

                if (results.length() == 0) {
                    callback.onError(
                            "'" + location + "' 지역을 찾을 수 없습니다."
                    );
                    return;
                }

                JSONObject place = results.getJSONObject(0);

                double latitude =
                        place.getDouble("latitude");

                double longitude =
                        place.getDouble("longitude");

                getWeather(
                        latitude,
                        longitude,
                        callback
                );

            } catch (Exception e) {

                callback.onError(
                        "지역 정보를 가져오지 못했습니다.\n"
                                + e.getMessage()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }
}