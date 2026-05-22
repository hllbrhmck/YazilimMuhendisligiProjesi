package com.example.yazilimproje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SymptomDao {
    @Insert
    void insertSymptom(SymptomEntity symptom);

    @Query("SELECT * FROM semptomlar ORDER BY semptom_id DESC")
    List<SymptomEntity> getAllSymptoms();

    @Query("DELETE FROM semptomlar")
    void clearAllSymptoms();
}
