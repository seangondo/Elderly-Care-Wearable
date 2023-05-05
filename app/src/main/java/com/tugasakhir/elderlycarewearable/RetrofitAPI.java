package com.tugasakhir.elderlycarewearable;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RetrofitAPI {
    @GET("/wearable/get_id/{id}/{sensor}")
    Call<Object> getWatchId(@Path("id") String watch_id,
                            @Path("sensor") String sensor);
}
