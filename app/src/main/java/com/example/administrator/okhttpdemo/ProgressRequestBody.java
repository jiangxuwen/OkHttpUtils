package com.example.administrator.okhttpdemo;

import java.io.IOException;



import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by Administrator on 2018/9/21.
 */

public class ProgressRequestBody extends RequestBody {

    private RequestBody mBody;
    private ProgressListener mListener;
    private ProgressSink mProgressSink;
    private BufferedSink mBufferedSink;
    public ProgressRequestBody(RequestBody body, ProgressListener listener){
        this.mBody = body;
        mListener = listener;
    }

    @Override
    public MediaType contentType() {
        return mBody.contentType();
    }

    //写入数据的方法
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        //将Sink重新构造
        mProgressSink = new ProgressSink(sink);
        if(mBufferedSink==null) {
            //创建输出流体系
            mBufferedSink = Okio.buffer(mProgressSink);
        }
        //进行流输出操作
        mBody.writeTo(mBufferedSink);
        mBufferedSink.flush();
    }

    @Override
    public long contentLength(){
        try {
            return mBody.contentLength();
        } catch (IOException e) {
            return -1;
        }
    }

    class ProgressSink extends ForwardingSink {
        private long byteWrite;//当前写入的字节
        public ProgressSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException {
            //必须执行父类方法，否则无法上传
            super.write(source, byteCount);
            byteWrite+= byteCount;
            if(mListener!=null) {
                //更新进度
                mListener.onProgress(byteWrite, contentLength());
            }
        }
    }

    public interface ProgressListener{
        void onProgress(long byteWrite, long contentLength);
    }
}
