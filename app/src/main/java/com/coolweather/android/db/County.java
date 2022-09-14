package com.coolweather.android.db;

import org.litepal.crud.LitePalSupport;

//记录县区的表
public class County extends LitePalSupport {
    private int id;
    private String countyName;
    private int weatherId;
    private int cityId;//记录当前县所属市的Id值

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCountyName() {
        return countyName;
    }

    public void setCountyName(String countyName) {
        this.countyName = countyName;
    }

    public int getWeatherId() {
        return weatherId;
    }

    public void setWeatherId(int weatherId) {
        this.weatherId = weatherId;
    }

    public int getCityId() {
        return cityId;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }
}
