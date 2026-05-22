package com.example.yazilimproje;

import android.app.Application;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DrugRepository {

    private static final String BASE_URL = "https://catwalk-daycare-partake.ngrok-free.dev/";

    private final MLApiService apiService;
    private final DrugDao drugDao;

    public DrugRepository(Application application) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(MLApiService.class);
        drugDao = AppDatabase.getDatabase(application).drugDao();
    }

    public void scanAndSaveDrug(String base64Image, final RepositoryCallback callback) {
        ScanRequest request = new ScanRequest(base64Image);

        apiService.scanDrug(request).enqueue(new Callback<DrugScanResult>() {
            @Override
            public void onResponse(Call<DrugScanResult> call, Response<DrugScanResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DrugScanResult result = response.body();

                    if (result.ilacAdi == null || result.ilacAdi.trim().isEmpty()) {
                        result.ilacAdi = "BİLİNMİYOR";
                    }

                    if (result.etkenMadde == null || result.etkenMadde.trim().isEmpty()) {
                        result.etkenMadde = "HATA";
                    }

                    if (!"BULUNAMADI".equals(result.etkenMadde) && !"HATA".equals(result.etkenMadde)) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            DrugEntity entity = new DrugEntity();
                            entity.ilac_adi = result.ilacAdi;
                            entity.etken_madde = result.etkenMadde;
                            entity.dozaj = result.dozaj == null ? "Bilinmiyor" : result.dozaj;
                            entity.atc_kodu = "BİLİNMİYOR";
                            entity.kayit_tarihi = new SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.getDefault()
                            ).format(new Date());

                            drugDao.insertDrug(entity);
                        });
                    }

                    callback.onSuccess(result);
                } else {
                    callback.onError("Sunucu Hatası: Yanıt alınamadı. Kod: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DrugScanResult> call, Throwable t) {
                callback.onError("Ağ Hatası: " + t.getMessage());
            }
        });
    }

    public interface RepositoryCallback {
        void onSuccess(DrugScanResult result);
        void onError(String error);
    }
}