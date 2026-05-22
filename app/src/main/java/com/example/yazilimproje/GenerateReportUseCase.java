package com.example.yazilimproje;

import android.app.Application;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class GenerateReportUseCase {

    private final Application application;
    private final DrugDao drugDao;
    private final SymptomDao symptomDao;
    private final AnalysisHistoryDao historyDao;

    public interface ReportCallback {
        void onComplete(String message);
    }

    public GenerateReportUseCase(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getDatabase(application);
        this.drugDao = db.drugDao();
        this.symptomDao = db.symptomDao();
        this.historyDao = db.analysisHistoryDao();
    }

    public void generateReport(ReportCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<DrugEntity> ilaclar = drugDao.getAllDrugs();
                List<SymptomEntity> semptomlar = symptomDao.getAllSymptoms();

                if (ilaclar == null || ilaclar.isEmpty()) {
                    callback.onComplete("PDF oluşturulamadı: aktif ilaç listesi boş.");
                    return;
                }

                AnalysisResult reportResult = buildReportResult(ilaclar, semptomlar);

                Uri pdfUri = PdfReportHelper.generateAnalysisPdf(application, reportResult);

                if (pdfUri == null) {
                    callback.onComplete("PDF oluşturma hatası: PDF dosyası üretilemedi.");
                    return;
                }

                callback.onComplete("PDF rapor oluşturuldu:\n" + pdfUri.toString());

            } catch (Exception e) {
                callback.onComplete("PDF oluşturma hatası: " + e.getMessage());
            }
        });
    }

    private AnalysisResult buildReportResult(List<DrugEntity> ilaclar, List<SymptomEntity> semptomlar) {
        List<String> ilacAdlari = new ArrayList<>();

        for (DrugEntity drug : ilaclar) {
            String ad;

            if (drug.ilac_adi != null && !drug.ilac_adi.trim().isEmpty()) {
                ad = drug.ilac_adi;
            } else if (drug.etken_madde != null && !drug.etken_madde.trim().isEmpty()) {
                ad = drug.etken_madde;
            } else {
                ad = "Bilinmeyen İlaç";
            }

            ilacAdlari.add(ad);
        }

        try {
            List<AnalysisHistoryEntity> histories = historyDao.getAllAnalysisHistory();

            if (histories != null && !histories.isEmpty()) {
                AnalysisHistoryEntity latest = histories.get(0);

                RiskLevel level;

                try {
                    level = RiskLevel.valueOf(latest.risk_seviyesi);
                } catch (Exception e) {
                    level = RiskLevel.INSUFFICIENT_DATA;
                }

                StringBuilder aciklama = new StringBuilder();
                aciklama.append("Son analiz geçmişine göre rapor oluşturuldu.\n\n");

                if (latest.semptom_listesi_json != null) {
                    aciklama.append("Hastalık / ICD Bilgisi: ")
                            .append(latest.semptom_listesi_json)
                            .append("\n\n");
                }

                if (latest.sonuc_json != null) {
                    aciklama.append("Analiz Sonucu: ")
                            .append(latest.sonuc_json);
                } else {
                    aciklama.append("Analiz açıklaması bulunamadı.");
                }

                return new AnalysisResult(
                        level,
                        "İSUTS Analiz Raporu",
                        aciklama.toString(),
                        ilacAdlari
                );
            }

        } catch (Exception ignored) {
        }

        StringBuilder aciklama = new StringBuilder();
        aciklama.append("Henüz analiz geçmişi bulunamadı. Bu rapor aktif ilaç ve ICD-10 kayıtlarına göre oluşturuldu.\n\n");

        if (semptomlar != null && !semptomlar.isEmpty()) {
            aciklama.append("Kayıtlı ICD-10 Bilgileri:\n");

            for (SymptomEntity symptom : semptomlar) {
                aciklama.append("- ")
                        .append(symptom.icd10_kodu == null ? "ICD bilinmiyor" : symptom.icd10_kodu)
                        .append(" / ")
                        .append(symptom.semptom_adi == null ? "Hastalık adı bilinmiyor" : symptom.semptom_adi)
                        .append("\n");
            }
        } else {
            aciklama.append("Kayıtlı ICD-10 hastalık bilgisi bulunamadı.");
        }

        return new AnalysisResult(
                RiskLevel.INSUFFICIENT_DATA,
                "Aktif İlaç Listesi Raporu",
                aciklama.toString(),
                ilacAdlari
        );
    }
}