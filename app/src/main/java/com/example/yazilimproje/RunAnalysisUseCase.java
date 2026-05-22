package com.example.yazilimproje;

import android.app.Application;

public class RunAnalysisUseCase {
    private final ContraIndicationRepository repository;

    public RunAnalysisUseCase(Application application) {
        repository = new ContraIndicationRepository(application);
    }

    public void runAnalysis(ContraIndicationRepository.AnalysisCallback callback) {
        repository.runAnalysis(callback);
    }
}