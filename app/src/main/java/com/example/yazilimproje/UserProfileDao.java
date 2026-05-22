package com.example.yazilimproje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserProfileDao {
    @Insert
    void insertProfile(UserProfileEntity profile);

    @Query("SELECT * FROM kullanici_profili WHERE profil_id = 1 LIMIT 1")
    UserProfileEntity getDefaultProfile();

    @Query("DELETE FROM kullanici_profili")
    void clearProfiles();
}
