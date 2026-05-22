package com.example.yazilimproje;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private static final int KAMERA_KODU = 101;
    private static final int KAMERA_IZIN_KODU = 202;

    private ImageView imgOnizleme;
    private TextView txtSonuc;
    private File geciciResimDosyasi;

    private Button btnAnaliz;
    private Button btnTemizle;
    private Button btnManuelGiris;
    private Button btnPdfRapor;

    private LinearLayout layoutRisk;
    private TextView txtRiskBaslik;
    private TextView txtRiskAciklama;

    private CameraViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgOnizleme = findViewById(R.id.imgOnizleme);
        txtSonuc = findViewById(R.id.txtSonuc);

        btnAnaliz = findViewById(R.id.btnAnaliz);
        btnTemizle = findViewById(R.id.btnTemizle);
        btnManuelGiris = findViewById(R.id.btnManuelGiris);
        btnPdfRapor = findViewById(R.id.btnPdfRapor);

        layoutRisk = findViewById(R.id.layoutRisk);
        txtRiskBaslik = findViewById(R.id.txtRiskBaslik);
        txtRiskAciklama = findViewById(R.id.txtRiskAciklama);

        Button btnKamera = findViewById(R.id.btnKamera);

        viewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        viewModel.uiStateMessage.observe(this, message -> {
            txtSonuc.setText(message);
        });

        viewModel.analysisResult.observe(this, result -> {
            if (result != null) {
                layoutRisk.setVisibility(View.VISIBLE);
                txtRiskBaslik.setText(result.baslik);
                txtRiskAciklama.setText(
                        result.aciklama +
                                "\n\nAnaliz Edilen İlaçlar: " +
                                result.analizEdilenIlaclar.toString()
                );

                if (result.riskLevel == RiskLevel.RED) {
                    AlertManager.triggerCriticalAlert(MainActivity.this);
                }

                switch (result.riskLevel) {
                    case RED:
                        layoutRisk.setBackgroundColor(Color.parseColor("#FFCDD2"));
                        txtRiskBaslik.setBackgroundColor(Color.parseColor("#D32F2F"));
                        txtRiskBaslik.setTextColor(Color.WHITE);
                        break;

                    case YELLOW:
                        layoutRisk.setBackgroundColor(Color.parseColor("#FFF9C4"));
                        txtRiskBaslik.setBackgroundColor(Color.parseColor("#FBC02D"));
                        txtRiskBaslik.setTextColor(Color.BLACK);
                        break;

                    case GREEN:
                        layoutRisk.setBackgroundColor(Color.parseColor("#C8E6C9"));
                        txtRiskBaslik.setBackgroundColor(Color.parseColor("#388E3C"));
                        txtRiskBaslik.setTextColor(Color.WHITE);
                        break;

                    case INSUFFICIENT_DATA:
                        layoutRisk.setBackgroundColor(Color.parseColor("#E0E0E0"));
                        txtRiskBaslik.setBackgroundColor(Color.parseColor("#757575"));
                        txtRiskBaslik.setTextColor(Color.WHITE);
                        break;
                }
            } else {
                layoutRisk.setVisibility(View.GONE);
            }
        });

        btnKamera.setOnClickListener(v -> {
            kameraIzniKontrolEtVeAc();
        });

        btnAnaliz.setOnClickListener(v -> {
            viewModel.runInteractionAnalysis();
        });

        btnTemizle.setOnClickListener(v -> {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

                db.drugDao().clearAllDrugs();
                db.symptomDao().clearAllSymptoms();

                runOnUiThread(() -> {
                    layoutRisk.setVisibility(View.GONE);
                    txtSonuc.setText("İlaç ve hastalık listesi sıfırlandı. Yeni test yapabilirsiniz.");
                });
            });
        });

        btnManuelGiris.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ManualEntryActivity.class);
            startActivity(intent);
        });

        btnPdfRapor.setOnClickListener(v -> {
            pdfRaporIcinKimlikDogrula();
        });
    }

    private void kameraIzniKontrolEtVeAc() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            kamerayiAc();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    KAMERA_IZIN_KODU
            );
        }
    }

    private void kamerayiAc() {
        Intent kameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (kameraIntent.resolveActivity(getPackageManager()) == null) {
            txtSonuc.setText("Bu cihazda kamera uygulaması bulunamadı.");
            Toast.makeText(this, "Kamera uygulaması bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            geciciResimDosyasi = File.createTempFile(
                    "isuts_temp_image",
                    ".jpg",
                    getCacheDir()
            );

            Uri resimUri = FileProvider.getUriForFile(
                    MainActivity.this,
                    "com.example.yazilimproje.fileprovider",
                    geciciResimDosyasi
            );

            kameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, resimUri);
            kameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            kameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            startActivityForResult(kameraIntent, KAMERA_KODU);

        } catch (IOException e) {
            txtSonuc.setText("Kamera dosyası hazırlanamadı: " + e.getLocalizedMessage());
        } catch (Exception e) {
            txtSonuc.setText("Kamera açılırken hata oluştu: " + e.getMessage());
        }
    }

    private void pdfRaporIcinKimlikDogrula() {
        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                MainActivity.this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                                errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                                errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL) {
                            viewModel.generatePdfReport();
                        } else {
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Doğrulama Hatası: " + errString,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        viewModel.generatePdfReport();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(
                                getApplicationContext(),
                                "Kimlik doğrulanamadı!",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("PDF Raporuna Erişim")
                .setSubtitle("Hasta mahremiyeti için kimlik doğrulaması gereklidir.")
                .setNegativeButtonText("İptal")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == KAMERA_IZIN_KODU) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                kamerayiAc();
            } else {
                txtSonuc.setText("Kamera izni verilmedi. İlaç tarama işlemi yapılamaz.");
                Toast.makeText(this, "Kamera izni gerekli.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == KAMERA_KODU && resultCode == RESULT_OK) {
            if (geciciResimDosyasi != null && geciciResimDosyasi.exists()) {
                try {
                    Bitmap orijinalBuyukBitmap =
                            BitmapFactory.decodeFile(geciciResimDosyasi.getAbsolutePath());

                    if (orijinalBuyukBitmap == null) {
                        txtSonuc.setText("Fotoğraf okunamadı. Lütfen tekrar deneyin.");
                        return;
                    }

                    int maxBoyut = 640;
                    int orijinalGenislik = orijinalBuyukBitmap.getWidth();
                    int orijinalYukseklik = orijinalBuyukBitmap.getHeight();

                    int yeniGenislik = orijinalGenislik;
                    int yeniYukseklik = orijinalYukseklik;

                    if (orijinalGenislik > maxBoyut || orijinalYukseklik > maxBoyut) {
                        if (orijinalGenislik > orijinalYukseklik) {
                            yeniGenislik = maxBoyut;
                            yeniYukseklik = (yeniGenislik * orijinalYukseklik) / orijinalGenislik;
                        } else {
                            yeniYukseklik = maxBoyut;
                            yeniGenislik = (yeniYukseklik * orijinalGenislik) / orijinalYukseklik;
                        }
                    }

                    Bitmap optimizeBitmap = Bitmap.createScaledBitmap(
                            orijinalBuyukBitmap,
                            yeniGenislik,
                            yeniYukseklik,
                            true
                    );

                    imgOnizleme.setImageBitmap(optimizeBitmap);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    optimizeBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);

                    String base64Image = Base64.encodeToString(
                            outputStream.toByteArray(),
                            Base64.DEFAULT
                    );

                    viewModel.processImage(base64Image);

                } catch (Exception e) {
                    txtSonuc.setText("Fotoğraf işlenirken hata oluştu: " + e.getMessage());
                }
            } else {
                txtSonuc.setText("Fotoğraf dosyası bulunamadı. Lütfen tekrar deneyin.");
            }
        } else if (requestCode == KAMERA_KODU) {
            txtSonuc.setText("Kamera işlemi iptal edildi.");
        }
    }
}