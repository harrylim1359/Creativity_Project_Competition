package com.example.practice;

public class Plan {

    private String id;
    private String date;
    private String time;
    private String title;

    public Plan(String id, String date, String time, String title) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}