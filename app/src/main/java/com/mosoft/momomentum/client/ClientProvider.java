package com.mosoft.momomentum.client;

import android.util.Log;

import com.mosoft.momomentum.module.MomentumApplication;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okio.Buffer;

public abstract class ClientProvider {

    protected String getTag() {
        return this.getClass().getSimpleName();
    }

    public class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            String requestLog = String.format("Sending request %s on %s : %s",
                    request.url(), chain.connection(), request.headers());

            if (request.method().compareToIgnoreCase("post") == 0) {
                requestLog = "\n" + requestLog + "\n" + bodyToString(request);
            }
            Log.d(getTag(), "request" + "\n" + requestLog);

            Response response = chain.proceed(request);
            long t2 = System.nanoTime();

            String responseLog = String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers());

            String bodyString = response.body().string();

            Log.d(getTag(), "response" + "\n" + responseLog + "\n" + bodyString);

            FileOutputStream fos = new FileOutputStream(new File("/sdcard/netlog"));
            fos.write(bodyString.getBytes());
            fos.write("\\n\\n".getBytes());

            return response.newBuilder()
                    .body(ResponseBody.create(response.body().contentType(), bodyString))
                    .build();
            //return response;
        }
    }

    public static String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return e.getMessage();
        }
    }

    public interface BrokerageClientProvider {
        ClientInterfaces.BrokerageClient getBrokerageClient();
    }

    public interface OptionChainClientProvider {
        ClientInterfaces.OptionChainClient getOptionChainClient();
    }

    public interface StockQuoteClientProvider {
        ClientInterfaces.StockQuoteClient getStockQuoteClient();
    }
}
