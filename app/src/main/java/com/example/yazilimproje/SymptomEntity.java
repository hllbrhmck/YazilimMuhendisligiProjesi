package com.example.yazilimproje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "semptomlar")
public class SymptomEntity {
    @PrimaryKey(autoGenerate = true)
    public int semptom_id;

    public int profil_id = 1;
    public String semptom_adi;
    public String icd10_kodu;
    public String kayit_tarihi;
}
