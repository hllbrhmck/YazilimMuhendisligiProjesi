package com.example.yazilimproje;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AnalysisHistoryDao {
    @Insert
    void insertAnalysis(AnalysisHistoryEntity history);

    @Query("SELECT * FROM analiz_gecmisi ORDER BY analiz_id DESC")
    List<AnalysisHistoryEntity> getAllAnalysisHistory();

    @Query("DELETE FROM analiz_gecmisi")
    void clearAllHistory();
}
