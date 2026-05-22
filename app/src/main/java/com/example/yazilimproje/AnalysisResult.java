package com.example.yazilimproje;

import java.util.List;

public class AnalysisResult {
    public RiskLevel riskLevel;
    public String baslik;
    public String aciklama;
    public List<String> analizEdilenIlaclar;

    public AnalysisResult(RiskLevel riskLevel, String baslik, String aciklama, List<String> analizEdilenIlaclar) {
        this.riskLevel = riskLevel;
        this.baslik = baslik;
        this.aciklama = aciklama;
        this.analizEdilenIlaclar = analizEdilenIlaclar;
    }
}