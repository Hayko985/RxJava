package com.ashokslsk.rxjava.service;

import com.google.gson.GsonBuilder;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class GistServiceFactory {

    public GistService getGistService() {
        OkHttpClient okHttpClient = getOkHttpClient();
        Retrofit retrofit = provideGistRetrofit(okHttpClient);
        return retrofit.create(GistService.class);
    }

    /**
     * This will gives the {@link OkHttpClient} instance with 30 seconds timeout for Read and Connect timeout limitation.
     * and also It has set {@link okhttp3.logging.HttpLoggingInterceptor.Logger} information to print the all information
     * about your service call like URL, Header if available, body content, Http Type, etc,,,.
     *
     * @return
     */
    private OkHttpClient getOkHttpClient() {
        X509TrustManager trustManager = null;

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }
            trustManager = (X509TrustManager) trustManagers[0];
        } catch (KeyStoreException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS) // 30 seconds Connection Timeout
                .readTimeout(30, TimeUnit.SECONDS) // 60 seconds Read Timeout
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)); // Logger for Api call

        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, new TrustManager[]{trustManager}, null);
            client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);
            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build();
            client.connectionSpecs(Collections.singletonList(cs));
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return client.build();
    }

    /**
     * It returns retrofit instance to make API call for given URL with RxJava2 support.
     *
     * @param okHttpClient - it provide all timeout info, logger info and all necessary
     *                     information to {@link Retrofit} by {@link OkHttpClient}
     * @return
     */
    private Retrofit provideGistRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // provides RxJava2 webservice call support here.
                .client(okHttpClient) // Sets OkHttpClient.
                .addConverterFactory(providesGsonConverterFactory()) // Set Gson converter here
                .baseUrl(GistService.BASE_URL).build();
    }

    /**
     * It provide Gson instance to convert Json to Pojo.
     *
     * @return
     */
    private GsonConverterFactory providesGsonConverterFactory() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        return GsonConverterFactory.create(gsonBuilder.create());
    }
}
