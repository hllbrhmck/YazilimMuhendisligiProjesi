İSUTS (İlaç Semptom Uyuşmazlık Tespit Sistemi)

## Proje Tanımı
İSUTS, hastaların aktif olarak kullandıkları ilaçlar ile mevcut kronik hastalıkları (ICD-10 kodları) arasındaki potansiyel uyuşmazlıkları (kontrendikasyonları) tespit eden yapay zeka destekli bir mobil sağlık asistanıdır. Sistem, cihaz kamerasıyla ilaç kutularını analiz edip etken maddeleri tespit eder ve harici tıbbi API'ler ile yerel kural motorunu kullanarak hastalara anlık risk analizi sunar.

## Özellikler
* **Görüntü İşleme ile Tarama:** YOLO ve Tesseract OCR entegrasyonu ile kamera üzerinden ilaç kutusu tanıma ve etken madde ayrıştırma.
* **Manuel Veri Girişi:** ICD-10 hastalık kodları ve ilaç bilgileri için manuel giriş desteği.
* **Akıllı Risk Analizi:** Girilen semptomlar ve ilaçlar arasındaki etkileşimi analiz ederek Kırmızı (Kritik), Sarı (Dikkatli Kullanım) ve Yeşil (Güvenli) risk seviyelerinde sonuç üretme.
* **Kritik Uyarı Sistemi:** Yüksek riskli durumlarda cihazın ses ve titreşim donanımlarını tetikleyen acil durum uyarı modülü.
* **Biyometrik Güvenlikli Raporlama:** Hasta verilerini korumak amacıyla biyometrik doğrulama (parmak izi vb.) ile analiz sonuçlarını PDF formatında dışa aktarma.

## Kullanılan Teknolojiler (Backend, Frontend, Veritabanı vb.)
* **Frontend (İstemci):** Android (Java), XML, MVVM Mimarisi, Retrofit, AndroidX Biometric API
* **Backend (Sunucu):** Python 3, FastAPI, Uvicorn
* **Veritabanı:** Room Database (Android Yerel Veritabanı - SQLite)
* **Yapay Zeka & Görüntü İşleme:** YOLO (Ultralytics), OpenCV, pytesseract
* **Harici Servisler:** RxNorm API, openFDA API, Ngrok

## Kurulum Adımları
1. Projeyi bilgisayarınıza klonlayın.
2. **Backend İçin:** `isuts_backend` klasörüne girin. Python sanal ortamını aktif edip `pip install -r requirements.txt` komutuyla bağımlılıkları yükleyin. `python -m uvicorn main:app --host 0.0.0.0 --port 8000` komutuyla yerel sunucuyu başlatın ve Ngrok üzerinden tünel oluşturun.
3. **Frontend İçin:** `isuts_android` klasörünü Android Studio ile açın. Veri katmanındaki (`DrugRepository` vb.) `BASE_URL` adreslerini oluşturduğunuz Ngrok linki ile güncelleyip projeyi derleyin (Build).

## Kullanım
Uygulamayı Android cihazınızda başlatın. Yeni bir analiz yapmak için "Kamera ile Tara" butonunu kullanarak ilaç kutunuzu okutun veya bilgileri manuel olarak girin. Profilinize ICD-10 hastalık kodlarınızı ekledikten sonra "Analiz Et" butonuna dokunarak sistemin RxNorm ve openFDA üzerinden risk analizi yapmasını bekleyin. Sonuç ekranından detayları inceleyebilir ve PDF raporu oluşturabilirsiniz.

## Katkı (Contribution)
Projeye katkıda bulunmak için bu repoyu forklayabilir, geliştirmelerinizi yeni bir dal (branch) üzerinde yapıp Pull Request (PR) gönderebilirsiniz. Lütfen kod eklemeleri yaparken MVVM mimarisine ve Clean Code prensiplerine sadık kaldığınızdan emin olunuz.

## Lisans
Bu proje MIT Lisansı altında açık kaynak olarak paylaşılmaktadır. Uygulama tıbbi bir tavsiye niteliği taşımaz, sadece ön bilgilendirme amaçlıdır.
