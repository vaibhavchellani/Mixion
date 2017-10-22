package com.taskail.mixion.utils;

import android.app.Application;
import android.util.Log;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;

import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by ed on 10/22/17.
 */

public class MixionApolloApplication extends Application {
    private static final String TAG = "MixionApolloApplication";

    private static final String BASE_URL = "https://steemql.herokuapp.com/graphql";
    private static final String SQL_CACHE_NAME = "mixiondb";
    private ApolloClient apolloClient;

    @Override
    public void onCreate() {
        super.onCreate();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Log.d(TAG, "onCreate: The okHttpInterceptor");

        ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(this, SQL_CACHE_NAME);
        NormalizedCacheFactory normalizedCacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
                .chain(new SqlNormalizedCacheFactory(apolloSqlHelper));

        Log.d(TAG, "onCreate: The Sql Helper");


        CacheKeyResolver cacheKeyResolver = new CacheKeyResolver() {
            @Nonnull
            @Override
            public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
                String typeName = (String)recordSet.get("__typename");

                Log.d(TAG, "fromFieldRecordSet: " + typeName);


                if ("User".equals(typeName)) {
                    String userKey = typeName + "." + recordSet.get("login");
                    return CacheKey.from(userKey);
                }
                if (recordSet.containsKey("id")) {
                    String typeNameAndIDKey = recordSet.get("__typename") + "." + recordSet.get("id");
                    return CacheKey.from(typeNameAndIDKey);
                }
                return CacheKey.NO_KEY;
            }

            @Nonnull
            @Override
            public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
                return CacheKey.NO_KEY;
            }
        };

        apolloClient = ApolloClient.builder()
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .normalizedCache(normalizedCacheFactory, cacheKeyResolver)
                .build();
    }

    public ApolloClient apolloClient(){
        return apolloClient;
    }
}