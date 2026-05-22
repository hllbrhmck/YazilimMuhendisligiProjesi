package com.example.yazilimproje;

public enum RiskLevel {
    GREEN,  // Güvenli (Etkileşim yok)
    YELLOW, // Dikkatli kullanılmalı (Hafif yan etkiler)
    RED,    // Kesinlikle birlikte KULLANILMAMALI (Kritik tehlike)
    INSUFFICIENT_DATA // Yeterli veri yok
}