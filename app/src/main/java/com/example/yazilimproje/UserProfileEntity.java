package com.example.yazilimproje;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "kullanici_profili")
public class UserProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public int profil_id;

    public int yas;
    public String cinsiyet;
    public String kronik_hastaliklar;
    public String kvkk_onay_tarihi;
}
