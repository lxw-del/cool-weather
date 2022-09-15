package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;
//去引用创建的天气具体情况的类
public class Weather {
    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastsList;

}
