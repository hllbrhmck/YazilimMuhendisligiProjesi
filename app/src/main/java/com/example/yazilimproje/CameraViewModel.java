package com.example.yazilimproje;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class CameraViewModel extends AndroidViewModel {
    private ScanDrugUseCase scanDrugUseCase;
    private RunAnalysisUseCase runAnalysisUseCase;

    public MutableLiveData<String> uiStateMessage = new MutableLiveData<>();
    public MutableLiveData<DrugScanResult> scanResult = new MutableLiveData<>();
    public MutableLiveData<AnalysisResult> analysisResult = new MutableLiveData<>();

    public CameraViewModel(Application application) {
        super(application);
        scanDrugUseCase = new ScanDrugUseCase(application);
        runAnalysisUseCase = new RunAnalysisUseCase(application);
    }

    public void processImage(String base64Image) {
        uiStateMessage.setValue("Hızlı veri paketlendi, sunucuya iletiliyor...");

        scanDrugUseCase.scan(base64Image, new DrugRepository.RepositoryCallback() {
            @Override
            public void onSuccess(DrugScanResult result) {
                scanResult.setValue(result);

                if ("BULUNAMADI".equals(result.etkenMadde) || "HATA".equals(result.etkenMadde)) {
                    uiStateMessage.setValue("Sonuç: İlaç okunamadı.\nLütfen ışıklı bir ortamda tekrar deneyin.");
                } else if (result.guvenSkoru < 0.85) {
                    uiStateMessage.setValue("Düşük güven skoru: %" + Math.round(result.guvenSkoru * 100) +
                            "\nİlaç: " + result.ilacAdi +
                            "\nLütfen manuel teyit ediniz.");
                } else {
                    uiStateMessage.setValue("Etken Madde: " + result.etkenMadde +
                            "\nDozaj: " + result.dozaj +
                            "\nGüven Skoru: %" + Math.round(result.guvenSkoru * 100) +
                            "\n(Veritabanına Kaydedildi!)");
                }
            }

            @Override
            public void onError(String error) {
                uiStateMessage.setValue(error);
            }
        });
    }

    public void runInteractionAnalysis() {
        runAnalysisUseCase.runAnalysis(result -> {
            analysisResult.postValue(result);
        });
    }

    public void generatePdfReport() {
        uiStateMessage.setValue("PDF rapor oluşturuluyor...");
        GenerateReportUseCase generateReportUseCase = new GenerateReportUseCase(getApplication());
        generateReportUseCase.generateReport(result -> {
            uiStateMessage.postValue(result);
        });
    }
}