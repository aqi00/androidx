package com.example.network.task;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.network.http.HttpRequestUtil;
import com.example.network.http.tool.HttpReqData;
import com.example.network.http.tool.HttpRespData;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

// 根据经纬度获取详细地址的线程
public class GetAddressTask extends AsyncTask<Location, Void, String> {
    private final static String TAG = "GetAddressTask";
    // 谷歌地图从2019年开始必须传入密钥才能根据经纬度获取地址，所以把查询接口改成了国内的天地图
    //private String mAddressUrl = "http://maps.google.cn/maps/api/geocode/json?latlng={0},{1}&sensor=true&language=zh-CN";
    private String mQueryUrl = "https://api.tianditu.gov.cn/geocoder?postStr=%s&type=geocode&tk=253b3bd69713d4bdfdc116255f379841";

    public GetAddressTask() {
        super();
    }

    // 线程正在后台处理
    protected String doInBackground(Location... params) {
        Location location = params[0];
        // 把经度和纬度代入到URL地址
        String param = String.format("{'lon':%f,'lat':%f,'ver':1}", location.getLongitude(), location.getLatitude());
        try {
            param = URLEncoder.encode(param, "utf8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(mQueryUrl, param);
        // 创建一个HTTP请求对象
        HttpReqData req_data = new HttpReqData(url);
        // 发送HTTP请求信息，并获得HTTP应答对象
        HttpRespData resp_data = HttpRequestUtil.getData(req_data);
        Log.d(TAG, "return json = " + resp_data.content);
        String address = "未知";
        // 下面从json串中逐级解析formatted_address字段获得详细地址描述
        if (resp_data.err_msg.length() <= 0) {
            try {
                JSONObject obj = new JSONObject(resp_data.content);
//                JSONArray resultArray = obj.getJSONArray("results");
//                if (resultArray.length() > 0) {
//                    JSONObject resultObj = resultArray.getJSONObject(0);
//                    address = resultObj.getString("formatted_address");
//                }
                JSONObject result = obj.getJSONObject("result");
                address = result.getString("formatted_address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "address = " + address);
        return address; // 返回HTTP应答内容中的详细地址
    }

    // 线程已经完成处理
    protected void onPostExecute(String address) {
        // HTTP调用完毕，触发监听器的找到地址事件
        mListener.onFindAddress(address);
    }

    private OnAddressListener mListener; // 声明一个查询详细地址的监听器对象
    // 设置查询详细地址的监听器
    public void setOnAddressListener(OnAddressListener listener) {
        mListener = listener;
    }

    // 定义一个查询详细地址的监听器接口
    public interface OnAddressListener {
        void onFindAddress(String address);
    }

}
