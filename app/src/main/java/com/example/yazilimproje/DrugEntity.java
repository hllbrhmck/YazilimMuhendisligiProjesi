package com.example.yazilimproje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "aktif_ilaclar")
public class DrugEntity {
    @PrimaryKey(autoGenerate = true)
    public int ilac_id;

    public int profil_id = 1;
    public String ilac_adi;
    public String etken_madde;
    public String atc_kodu;
    public String dozaj;
    public String kayit_tarihi;
}