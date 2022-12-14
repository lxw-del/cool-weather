package com.coolweather.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    private String UrlData;

    public SwipeRefreshLayout swipeRefreshLayout;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button nav_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //DecorView ???????????????????????????View
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            //???????????????????????????????????????????????????????????????android:fitsSystemWindows?????????????????????????????????????????????
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            //????????????????????????
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        //?????????????????????
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        nav_button = (Button) findViewById(R.id.nav_button);

        //???????????????????????????
        swipeRefreshLayout.setColorSchemeResources(R.color.teal_200);

        //????????????????????????
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = pref.getString("weather",null);

        String bingPic = pref.getString("bing_pic",null);

        if(bingPic!=null)
        {
            Glide.with(this).load(bingPic).into(bingPicImg);

        }else
        {
            loadBingPic();
        }

        if(weatherString != null) {
            //????????????????????????????????????????????????
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            ShowWeatherInfo(weather);

        }else{
            //???????????????????????????????????????????????????
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        nav_button.setOnClickListener(v->{
            drawerLayout.openDrawer(GravityCompat.START);
        });
    }
    //?????????????????????
    public void requestWeather(String weatherId)
    {

        loadBingPic();

        //??????URL
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=7a7203d4d6174f13993a8879c7735d90";
        //??????????????????
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //?????????Toast?????????????????????????????????
                        Toast.makeText(WeatherActivity.this,"????????????????????????",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                //???????????????????????????????????????Weather???
                final String responseBody = response.body().string();
                Weather weather = Utility.handleWeatherResponse(responseBody);

                //????????????????????????????????????????????????????????????????????????????????????
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null & "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseBody);
                            editor.apply();
                            //???????????????weatherId
                            mWeatherId = weather.basic.weatherId;
                            ShowWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this,"????????????????????????",Toast.LENGTH_SHORT).show();
                        }
                        //??????????????????????????????????????????????????????
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
                loadBingPic();

            }
        });
    }

    public void ShowWeatherInfo(Weather weather)
    {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime;
        String degree = weather.now.temperature+"???";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        //??????forecast???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastsList) {
            //????????????????????????????????????
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            //????????????????????????
            TextView dateText = (TextView) view.findViewById(R.id.data_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }

        //??????aqi
        if(weather.aqi!=null)
        {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "????????????"+weather.suggestion.comfort.info;
        String carWash = "???????????????"+weather.suggestion.carWash.info;
        String sport = "????????????: "+weather.suggestion.sport.info;

        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);

        weatherLayout.setVisibility(View.VISIBLE);
        //????????????????????????
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void loadBingPic(){
        String requestBingPic = "https://cn.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1";

        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bingPic = response.body().string();
                UrlData = Utility.handleBingImage(bingPic);

                Log.d(TAG, bingPic);
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",UrlData);
                editor.apply();
                showResponse(UrlData);
            }
        });
    }
    //??????????????????
    private void showResponse(final String response)
    {
        runOnUiThread(()->{
            Glide.with(WeatherActivity.this).load(response).into(bingPicImg);
            Log.d(TAG,response);
        });
    }
}