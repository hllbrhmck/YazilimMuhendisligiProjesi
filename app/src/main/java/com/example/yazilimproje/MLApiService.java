package com.example.yazilimproje;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface MLApiService {

    @Headers("ngrok-skip-browser-warning: 69420")
    @POST("api/v1/scan")
    Call<DrugScanResult> scanDrug(@Body ScanRequest request);
}