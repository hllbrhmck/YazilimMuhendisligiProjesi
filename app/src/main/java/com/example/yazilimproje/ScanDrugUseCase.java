package com.example.yazilimproje;

import android.app.Application;


public class ScanDrugUseCase {
    private final DrugRepository repository;

    public ScanDrugUseCase(Application application) {
        repository = new DrugRepository(application);
    }

    public void scan(String base64Image, DrugRepository.RepositoryCallback callback) {
        repository.scanAndSaveDrug(base64Image, callback);
    }
}
