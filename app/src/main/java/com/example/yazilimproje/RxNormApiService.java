package com.example.yazilimproje;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RxNormApiService {

    @GET("interaction/interaction.json")
    Call<JsonObject> checkInteraction(@Query("rxcui") String rxcui);
}