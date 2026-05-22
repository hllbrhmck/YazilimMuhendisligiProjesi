package com.example.yazilimproje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DrugDao {
    @Insert
    void insertDrug(DrugEntity drug);

    @Query("SELECT * FROM aktif_ilaclar")
    List<DrugEntity> getAllDrugs();

    @Query("DELETE FROM aktif_ilaclar")
    void clearAllDrugs();
}