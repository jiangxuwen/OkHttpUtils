package com.example.administrator.okhttpdemo;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by Administrator on 2018/9/13.
 *
 * 1.OkhttpClient为网络请求的一个中心，它会管理连接池、缓存、SocketFactory、代理
 *   、各种超时时间、DNS、请求执行结果的分发等许多内容。
 * 2.Request：Request是一个HTTP请求体，比如请求方法GET/POST、URL、Header、Body
 *   请求的换粗策略等。
 * 3.Call：通过OkhttpClient和Request来创建Call，Call是一个Task，它会执行网络请求
 *   并且获得响应。这个Task可以通过execute()同步执行，阻塞至请求成功。也可以通过
 *   enqueue()异步执行，会将Call放入一个异步执行队列，由ExecutorService后台执行。
 */

public class OkHttpUtils {

    private volatile static OkHttpUtils mOkHttpUtils = null;
    private OkHttpClient mOkHttpClient;
    private Handler mHandler;
    private OkHttpUtils(Context context){
        //缓存的文件夹
        File fileCache = new File(context.getExternalCacheDir(),"response");
        int cacheSize = 10*1024*1024;//缓存大小为10M
        Cache cache = new Cache(fileCache, cacheSize);
        //进行OkHttpClient的一些设置
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10,TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)//设置缓存
                .cache(cache)
                .build();
        mHandler = new Handler();
    }

    public static OkHttpUtils getInstance(Context context){
        if(mOkHttpUtils==null){
            synchronized (OkHttpUtils.class){
                if(mOkHttpUtils==null){
                    mOkHttpUtils = new OkHttpUtils(context);
                }
            }
        }
        return mOkHttpUtils;
    }

    //GET同步请求
    public String get_Sync(String url){
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        Call call = mOkHttpClient.newCall(request);
        try {
            Response response = call.execute();
            if(response.isSuccessful()){
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //GET异步请求
    public void get_Async(String url, final OkHttpCallback callback){
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                //在UI线程中执行回调
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                //在UI线程中执行回调
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(response);
                    }
                });
            }
        });
    }

    //POST同步JSON
    public String post_SyncJSON(String url,String json){
        MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        RequestBody body = RequestBody.create(mediaType,json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = mOkHttpClient.newCall(request);
        try {
            Response response = call.execute();
            if(response.isSuccessful()){
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    //POST同步FORM
    public String pots_SyncForm(String url, Map<String,String> params){

        RequestBody body = buildParams(params);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = mOkHttpClient.newCall(request);
        try {
            Response response = call.execute();
            if(response.isSuccessful()){
                return response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //POST异步JSON
    public void post_AsyncJSON(String url, String json, final OkHttpCallback callback){

        MediaType mediaType = MediaType.parse("application/json;charset=utf-8");
        RequestBody body = RequestBody.create(mediaType,json);

        final Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e);
                    }
                });
            }
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(response);
                    }
                });
            }
        });
    }

    //POST异步FORM
    public void post_AsyncForm(String url, Map<String,String> params, final OkHttpCallback callback){

        RequestBody body = buildParams(params);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(response);
                    }
                });
            }
        });
    }

    //异步加载文件
    public void async_LoadFile(String url, final OkHttpCallback callback){
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e);
                    }
                });

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //下载文件进度条可以直接在onResponse中实现
                        InputStream is = response.body().byteStream();
                        //文件的总大小(单位字节)
                        final long contentLength = response.body().contentLength();

                        long sum = 0;//当前下载到的字节量
                        File file  = new File(Environment.getExternalStorageDirectory(), "girl.png");
                        FileOutputStream fos;
                        try {
                            fos = new FileOutputStream(file);
                            //数组越小进度的密度越高
                            byte[] bytes = new byte[128];
                            int len = 0;
                            while ((len = is.read(bytes))!=-1){
                                fos.write(bytes,0,len);
                                sum+=len;
                                Log.i("progress","progress="+(float)sum/contentLength);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步文件参数混合上传
     * @param url
     * @param params
     * @param file
     * @param callback 响应回调
     * @param progressListener 进度回调
     */
    public void async_uploadFileAndParams(String url, Map<String,String> params, File file, final OkHttpCallback callback, ProgressRequestBody.ProgressListener progressListener){
        RequestBody requestBody = RequestBody.//表示任意二进制流
                create(MediaType.parse("application/octet-stream"), file);
        //因为是文件参数混合上传，所以要分开构建
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        if(params!=null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        RequestBody multipartBody = builder
                //key需要服务器提供，相当于键值对的键
                .addFormDataPart("image",file.getName(),requestBody)
                .build();
        ProgressRequestBody countingRequestBody
                = new ProgressRequestBody(multipartBody, progressListener);
        Request request = new Request.Builder()
                .url(url)
                .post(countingRequestBody)
                .build();
        Call call = mOkHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResponse(response);
                    }
                });
            }
        });

    }

    //参数添加到表单中
    public RequestBody buildParams(Map<String,String> params){
        if(params==null){
            params = new HashMap<>();
        }
        FormBody.Builder builder = new FormBody.Builder();
        for(Map.Entry<String,String> entry:params.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            if (value==null){
                value = "";
            }
            builder.add(key,value);
        }
        return builder.build();
    }

    //根据tag取消单个请求
    //最终的取消时通过拦截器RetryAndFollowUpInterceptor进行的
    public void cancel(Call call){
        //queuedCalls()代表所有准备运行的异步任务
        for(Call dispatcherCal1:mOkHttpClient.dispatcher().queuedCalls()){
            if(call.request().tag().equals(call.request().tag())){
                call.cancel();
            }
        }
        //runningCalls()代表所有正在运行的任务(包括同步和异步)
        for(Call dispatcherCal1:mOkHttpClient.dispatcher().runningCalls()){
            if(call.request().tag().equals(call.request().tag())){
                call.cancel();
            }
        }
    }

    //取消全部请求
    public void cancelAll(){
        mOkHttpClient.dispatcher().cancelAll();
    }

    public interface OkHttpCallback{
        void onResponse(Response response);
        void onError(IOException e);
    }

}
