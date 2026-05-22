package com.example.yazilimproje;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ManualEntryActivity extends AppCompatActivity {

    private EditText edtIlacAdi;
    private EditText edtEtkenMadde;
    private EditText edtDozaj;
    private EditText edtIcd10;

    private TextView txtIcdCozum;
    private TextView txtManuelBilgi;

    private Button btnManuelIlacEkle;
    private Button btnHastalikEkle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);

        edtIlacAdi = findViewById(R.id.edtIlacAdi);
        edtEtkenMadde = findViewById(R.id.edtEtkenMadde);
        edtDozaj = findViewById(R.id.edtDozaj);
        edtIcd10 = findViewById(R.id.edtIcd10);

        txtIcdCozum = findViewById(R.id.txtIcdCozum);
        txtManuelBilgi = findViewById(R.id.txtManuelBilgi);

        btnManuelIlacEkle = findViewById(R.id.btnManuelIlacEkle);
        btnHastalikEkle = findViewById(R.id.btnHastalikEkle);

        btnManuelIlacEkle.setOnClickListener(v -> manuelIlacEkle());
        btnHastalikEkle.setOnClickListener(v -> hastalikEkle());

        edtIcd10.addTextChangedListener(new TextWatcher() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable runnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(runnable);

                runnable = () -> {
                    String code = normalizeIcd(s.toString());
                    if (code.length() >= 3) {
                        txtIcdCozum.setText("Çözümlenen Hastalık: " + resolveIcdName(code));
                    } else {
                        txtIcdCozum.setText("Çözümlenen Hastalık: ICD-10 kodu bekleniyor.");
                    }
                };

                handler.postDelayed(runnable, 300);
            }
        });
    }

    private void manuelIlacEkle() {
        String ilacAdi = edtIlacAdi.getText().toString().trim();
        String etkenMadde = edtEtkenMadde.getText().toString().trim();
        String dozaj = edtDozaj.getText().toString().trim();

        if (ilacAdi.isEmpty()) {
            Toast.makeText(this, "İlaç adı boş olamaz.", Toast.LENGTH_SHORT).show();
            return;
        }


        if (etkenMadde.isEmpty()) {
            etkenMadde = "Bilinmiyor";
        }

        String finalEtkenMadde = etkenMadde;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            DrugEntity entity = new DrugEntity();
            entity.ilac_adi = ilacAdi;
            entity.etken_madde = finalEtkenMadde;
            entity.dozaj = dozaj.isEmpty() ? "Bilinmiyor" : dozaj;
            entity.atc_kodu = "MANUEL";
            entity.kayit_tarihi = now();

            AppDatabase.getDatabase(getApplicationContext()).drugDao().insertDrug(entity);

            runOnUiThread(() -> {
                Toast.makeText(this, "İlaç aktif listeye eklendi.", Toast.LENGTH_SHORT).show();
                txtManuelBilgi.setText("Son işlem: " + ilacAdi + " aktif ilaç listesine eklendi.");
                edtIlacAdi.setText("");
                edtEtkenMadde.setText("");
                edtDozaj.setText("");
            });
        });
    }

    private void hastalikEkle() {
        String icdCode = normalizeIcd(edtIcd10.getText().toString());

        if (icdCode.isEmpty()) {
            Toast.makeText(this, "ICD-10 kodu boş olamaz.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (icdCode.length() < 3) {
            Toast.makeText(this, "Geçerli bir ICD-10 kodu giriniz. Örn: K25, I10, N18", Toast.LENGTH_SHORT).show();
            return;
        }

        String hastalikAdi = resolveIcdName(icdCode);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SymptomEntity symptom = new SymptomEntity();
            symptom.icd10_kodu = icdCode;
            symptom.semptom_adi = hastalikAdi;
            symptom.kayit_tarihi = now();

            AppDatabase.getDatabase(getApplicationContext()).symptomDao().insertSymptom(symptom);

            runOnUiThread(() -> {
                Toast.makeText(this, "ICD-10 hastalık bilgisi kaydedildi.", Toast.LENGTH_SHORT).show();
                txtManuelBilgi.setText("Son işlem: " + icdCode + " / " + hastalikAdi + " kaydedildi.");
                edtIcd10.setText("");
                txtIcdCozum.setText("Çözümlenen Hastalık: ICD-10 kodu bekleniyor.");
            });
        });
    }

    private String resolveIcdName(String code) {
        String normalized = normalizeIcd(code);

        if (normalized.startsWith("K25")) return "Mide ülseri";
        if (normalized.startsWith("K26")) return "Duodenum ülseri";
        if (normalized.startsWith("K27")) return "Peptik ülser";
        if (normalized.startsWith("K29")) return "Gastrit / mide rahatsızlığı";

        if (normalized.startsWith("I10")) return "Hipertansiyon";
        if (normalized.startsWith("I11")) return "Hipertansif kalp hastalığı";
        if (normalized.startsWith("I12")) return "Hipertansif böbrek hastalığı";
        if (normalized.startsWith("I13")) return "Hipertansif kalp ve böbrek hastalığı";
        if (normalized.startsWith("I15")) return "Sekonder hipertansiyon";

        if (normalized.startsWith("N18")) return "Kronik böbrek yetmezliği";
        if (normalized.startsWith("N19")) return "Böbrek yetmezliği";

        return "Tanımlı olmayan ICD-10 kodu";
    }

    private String normalizeIcd(String code) {
        if (code == null) return "";

        return code
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "")
                .replace(".", "");
    }

    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}