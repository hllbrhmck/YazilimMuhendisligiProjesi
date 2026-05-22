package com.example.yazilimproje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "analiz_gecmisi")
public class AnalysisHistoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int analiz_id;

    public int profil_id = 1;
    public String analiz_tarihi;
    public String ilac_listesi_json;
    public String semptom_listesi_json;
    public String risk_seviyesi;
    public String sonuc_json;
    public String pdf_yolu;
}
