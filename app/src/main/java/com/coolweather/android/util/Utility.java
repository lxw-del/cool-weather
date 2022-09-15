package com.coolweather.android.util;

import android.text.TextUtils;

//import com.google.gson.JsonArray;

import androidx.annotation.Nullable;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {

    //解析和处理服务器返回的省级数据
    public static boolean handleProvinceResponse(String response)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                JSONArray allProvince = new JSONArray(response);
                for (int i = 0; i < allProvince.length(); i++) {
                    JSONObject provinceObject = allProvince.getJSONObject(i);
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();//将数据保存，这是LitepalSupport的方法，保存表数据
                }

                return true;
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }
    //解析处理市数据
    public static boolean handleCityResponse(String response,int provinceId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                JSONArray allCity = new JSONArray(response);
                for (int i = 0; i < allCity.length(); i++) {
                    JSONObject cityObject = allCity.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();
                }
                return true;

            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }
    //解析处理县数据
    public static boolean handleCountyResponse(String response,int cityId)
    {
        if(!TextUtils.isEmpty(response))
        {
            try {
                //注意:创建JSONArray的时候请，记得把response放入
                JSONArray allCounty = new JSONArray(response);
                for (int i = 0; i < allCounty.length(); i++) {
                    JSONObject countyObject = allCounty.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    //这里设置的是CityId，表示属于哪个城市
                    county.setCityId(cityId);
                    county.save();
                }
                return true;

            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return false;
    }

    //将返回的数据解析成Weather实体类

    public static Weather handleWeatherResponse(String response)
    {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.class);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    //处理必应图片的数据
    public static String handleBingImage(String response){
        try {
            JSONArray jsonArray = new JSONObject(response).getJSONArray("images");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String url = jsonObject.getString("url");
                String url1 = "http://cn.bing.com"+url;
                return url1;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


}
