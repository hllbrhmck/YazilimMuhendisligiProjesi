package com.example.yazilimproje;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/v1/scan")
    Call<ScanResponse> scanDrug(@Body ScanRequest request);
}
