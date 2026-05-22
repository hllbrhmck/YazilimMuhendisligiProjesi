package com.example.yazilimproje;

import com.google.gson.annotations.SerializedName;

public class DrugScanResult {
    @SerializedName("ilacAdi")
    public String ilacAdi;

    @SerializedName("etkenMadde")
    public String etkenMadde;

    @SerializedName("guvenSkoru")
    public double guvenSkoru;

    @SerializedName("dozaj")
    public String dozaj;

    @SerializedName("modelVersion")
    public String modelVersion;
}