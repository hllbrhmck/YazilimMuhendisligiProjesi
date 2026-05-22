package com.example.yazilimproje;

import java.util.List;

public class InteractionRequest {
    public List<String> semptomlar;
    public List<String> icd10Kodlari;
    public List<String> ilaclar;
    public List<String> etkenMaddeler;

    public InteractionRequest(
            List<String> semptomlar,
            List<String> icd10Kodlari,
            List<String> ilaclar,
            List<String> etkenMaddeler
    ) {
        this.semptomlar = semptomlar;
        this.icd10Kodlari = icd10Kodlari;
        this.ilaclar = ilaclar;
        this.etkenMaddeler = etkenMaddeler;
    }
}