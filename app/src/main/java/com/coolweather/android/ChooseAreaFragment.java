package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView textView;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;

    //选中的省份
    private Province selectedProvince;

    //选中的城市
    private City selectedCity;

    //当前选中的等级
    private int currentLevel;

    //进行初始化操作，获取控件实例，并设置适配器，和对碎片的布局进行加载
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        textView = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    //用这个方法替代onActivityCreated
    //这里用于判断当前界面等级，并显示出省市或者县在listview上。
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        //因为onActivityCreated被弃用所以使用这种方法去获得created事件
        requireActivity().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if(event.getTargetState() == Lifecycle.State.CREATED){
                    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position,
                                                long id) {
                            if(currentLevel == LEVEL_PROVINCE)
                            {
                                //因为provinceList和dataList是一一对应的，所以position可以共用
                                selectedProvince = provinceList.get(position);
                                queryCities();
                            }else if (currentLevel == LEVEL_CITY)
                            {
                                selectedCity = cityList.get(position);
                                queryCounties();
                            }else if(currentLevel == LEVEL_COUNTY){
                                String weatherId = countyList.get(position).getWeatherId();
                                Intent intent = new Intent(getActivity(),WeatherActivity.class);
                                intent.putExtra("weather_id",weatherId);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        }
                    });

                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(currentLevel == LEVEL_COUNTY) {
                                queryCities();
                            }else if(currentLevel == LEVEL_CITY) {
                                queryProvinces();
                            }
                        }
                    });

                    queryProvinces();
                }
                getLifecycle().removeObserver(this);
            }

        });

    }

    //查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器查询
    private void queryProvinces()
    {
        textView.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = LitePal.findAll(Province.class);
        if(provinceList.size()>0)
        {
            dataList.clear();
            for (Province province :provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else
        {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    //遍历市
    private void queryCities()
    {
        textView.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0)
        {
            dataList.clear();
            for (City city :cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else
        {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }

    }
    //遍历县
    private void queryCounties()
    {
        textView.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0)
        {
            dataList.clear();
            for (County county:countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else
        {
           int provinceCode = selectedProvince.getProvinceCode();
           int cityCode = selectedCity.getCityCode();
           String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
           queryFromServer(address,"county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县的数据
    private void queryFromServer(String address,final String type)
    {
        //显示进度条
        showProgressDialog();
        //发送okhttp请求
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                //这里Toast也需要在主线程去使用。
                getActivity().runOnUiThread(()->{
                    //关闭进度框
                    closeProgressDialog();
                    //弹出提示
                    Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type))
                {
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type))
                {
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type))
                {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if(result)
                {   //这里只是碎片类，不是主线程，所以需要你转换到主线程才能进行UI的操作
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type))
                            {
                                queryProvinces();
                            }else if ("city".equals(type))
                            {
                                queryCities();
                            }else if ("county".equals(type))
                            {
                                queryCounties();
                            }
                        }
                    });

                }
            }
        });
    }

    //打开进度条
    private void showProgressDialog()
    {
        if(progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度条
    private void closeProgressDialog()
    {
        if(progressDialog!=null)
        {
            progressDialog.dismiss();
        }
    }
}
