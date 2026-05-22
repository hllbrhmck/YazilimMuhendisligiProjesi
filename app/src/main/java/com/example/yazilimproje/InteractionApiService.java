package com.example.yazilimproje;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface InteractionApiService {

    @Headers("ngrok-skip-browser-warning: 69420")
    @POST("api/v1/analyze")
    Call<JsonObject> checkInteraction(@Body InteractionRequest request);
}