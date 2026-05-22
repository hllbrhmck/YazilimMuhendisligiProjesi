package com.example.yazilimproje;

import android.app.Application;

import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ContraIndicationRepository {

    private final DrugDao drugDao;
    private final SymptomDao symptomDao;
    private final AnalysisHistoryDao analysisHistoryDao;
    private final InteractionApiService apiService;

    private static final String API_BASE_URL = "https://catwalk-daycare-partake.ngrok-free.dev/";

    public ContraIndicationRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);

        drugDao = db.drugDao();
        symptomDao = db.symptomDao();
        analysisHistoryDao = db.analysisHistoryDao();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(InteractionApiService.class);
    }

    public void runAnalysis(AnalysisCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<DrugEntity> aktifIlaclar = drugDao.getAllDrugs();
            List<SymptomEntity> aktifSemptomlar = symptomDao.getAllSymptoms();

            List<String> ticariIsimler = new ArrayList<>();
            List<String> etkenMaddeler = new ArrayList<>();
            List<String> semptomIsimleri = new ArrayList<>();
            List<String> icd10Kodlari = new ArrayList<>();

            for (DrugEntity drug : aktifIlaclar) {
                if (drug.ilac_adi != null && !drug.ilac_adi.trim().isEmpty()) {
                    ticariIsimler.add(drug.ilac_adi.trim());
                } else {
                    ticariIsimler.add("Bilinmeyen İlaç");
                }

                if (drug.etken_madde != null && !drug.etken_madde.trim().isEmpty()) {
                    etkenMaddeler.add(drug.etken_madde.trim());
                }
            }

            for (SymptomEntity symptom : aktifSemptomlar) {
                if (symptom.semptom_adi != null && !symptom.semptom_adi.trim().isEmpty()) {
                    semptomIsimleri.add(symptom.semptom_adi.trim());
                }

                if (symptom.icd10_kodu != null && !symptom.icd10_kodu.trim().isEmpty()) {
                    icd10Kodlari.add(symptom.icd10_kodu.trim().toUpperCase(Locale.ROOT));
                }
            }

            if (ticariIsimler.isEmpty() || etkenMaddeler.isEmpty()) {
                AnalysisResult sonuc = new AnalysisResult(
                        RiskLevel.INSUFFICIENT_DATA,
                        "Yetersiz Veri",
                        "Analiz yapılacak aktif ilaç bulunamadı. Lütfen önce ilaç taratın veya manuel ilaç ekleyin.",
                        ticariIsimler
                );
                callback.onResult(sonuc);
                return;
            }

            InteractionRequest requestBody = new InteractionRequest(
                    semptomIsimleri,
                    icd10Kodlari,
                    ticariIsimler,
                    etkenMaddeler
            );

            apiService.checkInteraction(requestBody).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                JsonObject res = response.body();

                                String riskStr = safeGet(res, "risk_level", "INSUFFICIENT_DATA");
                                String baslik = safeGet(res, "baslik", "Analiz Sonucu");
                                String aciklama = safeGet(res, "aciklama", "Açıklama alınamadı.");
                                String kaynak = safeGet(res, "kaynak", "Bilinmiyor");

                                RiskLevel level;
                                try {
                                    level = RiskLevel.valueOf(riskStr);
                                } catch (Exception e) {
                                    level = RiskLevel.INSUFFICIENT_DATA;
                                }

                                String semptomMetni = semptomIsimleri.isEmpty()
                                        ? "Belirtilmedi"
                                        : semptomIsimleri.toString();

                                String icdMetni = icd10Kodlari.isEmpty()
                                        ? "Belirtilmedi"
                                        : icd10Kodlari.toString();

                                String gorunurAciklama =
                                        "ICD-10 Kodları: " + icdMetni +
                                                "\nSemptom/Hastalık: " + semptomMetni +
                                                "\nKaynak: " + kaynak +
                                                "\n\n" + aciklama;

                                AnalysisResult sonuc = new AnalysisResult(
                                        level,
                                        baslik,
                                        gorunurAciklama,
                                        ticariIsimler
                                );

                                saveAnalysisHistory(icdMetni, semptomMetni, sonuc);
                                callback.onResult(sonuc);
                            } else {
                                AnalysisResult hata = new AnalysisResult(
                                        RiskLevel.INSUFFICIENT_DATA,
                                        "API Hatası",
                                        "Python analiz API isteği işleyemedi. HTTP Kod: " + response.code(),
                                        ticariIsimler
                                );
                                callback.onResult(hata);
                            }
                        } catch (Exception e) {
                            AnalysisResult hata = new AnalysisResult(
                                    RiskLevel.INSUFFICIENT_DATA,
                                    "Yanıt İşleme Hatası",
                                    "API yanıtı okunurken hata oluştu: " + e.getMessage(),
                                    ticariIsimler
                            );
                            callback.onResult(hata);
                        }
                    });
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        AnalysisResult hata = new AnalysisResult(
                                RiskLevel.INSUFFICIENT_DATA,
                                "Bağlantı Hatası",
                                "Python sunucusuna ulaşılamadı. Ngrok açık mı?\nHata: " + t.getMessage(),
                                ticariIsimler
                        );
                        callback.onResult(hata);
                    });
                }
            });
        });
    }

    private String safeGet(JsonObject object, String key, String defaultValue) {
        try {
            if (object != null && object.has(key) && !object.get(key).isJsonNull()) {
                return object.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    private void saveAnalysisHistory(String icdOzet, String semptomOzet, AnalysisResult sonuc) {
        AnalysisHistoryEntity history = new AnalysisHistoryEntity();
        history.analiz_tarihi = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        history.ilac_listesi_json = sonuc.analizEdilenIlaclar.toString();
        history.semptom_listesi_json = "ICD: " + icdOzet + " | Semptom: " + semptomOzet;
        history.risk_seviyesi = sonuc.riskLevel.name();
        history.sonuc_json = "{baslik:'" + sonuc.baslik + "', aciklama:'" + sonuc.aciklama + "'}";
        history.pdf_yolu = null;

        analysisHistoryDao.insertAnalysis(history);
    }

    public interface AnalysisCallback {
        void onResult(AnalysisResult result);
    }
}