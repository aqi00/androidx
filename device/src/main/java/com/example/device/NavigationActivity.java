package com.example.device;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.device.bean.Satellite;
import com.example.device.util.DateUtil;
import com.example.device.util.SwitchUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ouyangshen on 2018/1/27.
 */
public class NavigationActivity extends AppCompatActivity {
    private final static String TAG = "NavigationActivity";
    private TextView tv_navigation;
    private LocationManager mLocationMgr; // 声明一个定位管理器对象
    private Map<String, Boolean> mapNavigation = new HashMap<String, Boolean>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        TextView tv_device = findViewById(R.id.tv_device);
        tv_device.setText(String.format("当前设备型号为%s", Build.MODEL));
        tv_navigation = findViewById(R.id.tv_navigation);
        SwitchUtil.checkGpsIsOpen(this, "需要打开定位功能才能查看卫星导航信息");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从系统服务中获取定位管理器
        mLocationMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // 检查当前设备是否已经开启了定位功能
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请授予定位权限并开启定位功能", Toast.LENGTH_SHORT).show();
            return;
        }
        // 必须使用卫星定位才能找到天上的导航卫星
        String bestProvider = LocationManager.GPS_PROVIDER;
        // 设置定位管理器的位置变更监听器
        mLocationMgr.requestLocationUpdates(bestProvider, 300, 0, mLocationListener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 注册全球导航卫星系统的状态监听器
            mLocationMgr.registerGnssStatusCallback(mGnssStatusListener, null);
        } else {
            // 给定位管理器添加导航状态监听器
            mLocationMgr.addGpsStatusListener(mStatusListener);
        }
    }

    @Override
    protected void onDestroy() {
        if (mLocationMgr != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 注销全球导航卫星系统的状态监听器
                mLocationMgr.unregisterGnssStatusCallback(mGnssStatusListener);
            } else {
                // 移除定位管理器的导航状态监听器
                mLocationMgr.removeGpsStatusListener(mStatusListener);
            }
            // 移除定位管理器的位置变更监听器
            mLocationMgr.removeUpdates(mLocationListener);
        }
        super.onDestroy();
    }

    // 定义一个位置变更监听器
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onProviderDisabled(String arg0) {}

        @Override
        public void onProviderEnabled(String arg0) {}

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
    };

    private String[] mSystemArray = new String[] {"UNKNOWN", "GPS", "SBAS",
            "GLONASS", "QZSS", "BEIDOU", "GALILEO", "IRNSS"};
    @RequiresApi(api = Build.VERSION_CODES.N)
    // 定义一个GNSS状态监听器
    private GnssStatus.Callback mGnssStatusListener = new GnssStatus.Callback() {
        @Override
        public void onStarted() {}

        @Override
        public void onStopped() {}

        @Override
        public void onFirstFix(int ttffMillis) {}

        // 在卫星导航系统的状态变更时触发
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
            for (int i=0; i<status.getSatelliteCount(); i++) {
                Log.d(TAG, "i="+i+",getSvid="+status.getSvid(i)+",getConstellationType="+status.getConstellationType(i));
                Satellite item = new Satellite(); // 创建一个卫星信息对象
                item.signal = status.getCn0DbHz(i); // 获取卫星的信号
                item.elevation = status.getElevationDegrees(i); // 获取卫星的仰角
                item.azimuth = status.getAzimuthDegrees(i); // 获取卫星的方位角
                item.time = DateUtil.getNowDateTime(); // 获取当前时间
                int systemType = status.getConstellationType(i); // 获取卫星的类型
                item.name = mSystemArray[systemType];
                if (systemType==1 || systemType==2) { // 分给美国的
                    mapNavigation.put("GPS", true);
                } else if (systemType==5) { // 分给中国的
                    mapNavigation.put("北斗", true);
                } else if (systemType==3) { // 分给俄罗斯的
                    mapNavigation.put("格洛纳斯", true);
                } else if (systemType==6) { // 分给欧盟的
                    mapNavigation.put("伽利略", true);
                } else {
                    mapNavigation.put(item.name, true);
                }
            }
            // 显示设备支持的卫星导航系统信息
            showNavigationInfo();
        }
    };

    // 定义一个导航状态监听器
    private GpsStatus.Listener mStatusListener = new GpsStatus.Listener() {

        // 在卫星导航系统的状态变更时触发
        public void onGpsStatusChanged(int event) {
            Log.d(TAG, "onGpsStatusChanged event="+event);
            if (ActivityCompat.checkSelfPermission(NavigationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // 获取卫星定位的状态信息
                GpsStatus gpsStatus = mLocationMgr.getGpsStatus(null);
                switch (event) {
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS: // 周期的报告卫星状态
                        // 得到所有收到的卫星信息，包括卫星的高度角、方位角、信噪比和卫星编号
                        Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
                        for (GpsSatellite satellite : satellites) {
                            Satellite item = new Satellite();
                            item.seq = satellite.getPrn(); // 卫星的伪随机码，可以认为就是卫星的编号
                            item.signal = Math.round(satellite.getSnr()); // 卫星的信噪比
                            item.elevation = Math.round(satellite.getElevation()); // 卫星的仰角 (卫星的高度)
                            item.azimuth = Math.round(satellite.getAzimuth()); // 卫星的方位角
                            item.time = DateUtil.getNowDateTime();
                            if (item.seq <= 64 || (item.seq >= 120 && item.seq <= 138)) { // 分给美国的
                                mapNavigation.put("GPS", true);
                            } else if (item.seq >= 201 && item.seq <= 237) { // 分给中国的
                                mapNavigation.put("北斗", true);
                            } else if (item.seq >= 65 && item.seq <= 89) { // 分给俄罗斯的
                                mapNavigation.put("格洛纳斯", true);
                            } else if (item.seq != 193 && item.seq != 194) {
                                mapNavigation.put("未知", true);
                            }
                        }
                        // 显示设备支持的卫星导航系统信息
                        showNavigationInfo();
                    case GpsStatus.GPS_EVENT_FIRST_FIX: // 首次卫星定位
                    case GpsStatus.GPS_EVENT_STARTED: // 卫星导航服务开始
                    case GpsStatus.GPS_EVENT_STOPPED: // 卫星导航服务停止
                    default:
                        break;
                }
            }
        }
    };

    // 显示设备支持的卫星导航系统信息
    private void showNavigationInfo() {
        boolean isFirst = true;
        String desc = "支持的卫星导航系统包括：";
        for (Map.Entry<String, Boolean> item_map : mapNavigation.entrySet()) {
            if (!isFirst) {
                desc = String.format("%s、%s", desc, item_map.getKey());
            } else {
                desc = String.format("%s%s", desc, item_map.getKey());
                isFirst = false;
            }
        }
        tv_navigation.setText(desc);
    }

}
