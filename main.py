import cv2
import pytesseract
import base64
import numpy as np
import re
import requests

from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict, Any
from ultralytics import YOLO

app = FastAPI()

pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

model = YOLO('best.pt')

DRUG_DATABASE = {
    "AFERIN": {
        "etkenMadde": "Parasetamol, Klorfeniramin",
        "dozaj": "300mg/2mg",
        "keywords": ["AFERIN", "300", "PARASETAMOL", "KLORFENIRAMIN", "AFE", "RIN", "FERIN"]
    },
    "APRANAX": {
        "etkenMadde": "Naproksen",
        "dozaj": "550mg",
        "keywords": ["APRANAX", "550", "NAPROKSEN", "FORT", "APRA", "ANAX"]
    },
    "ARVELES": {
        "etkenMadde": "Deksketoprofen",
        "dozaj": "25mg",
        "keywords": ["ARVELES", "25", "DEKSKETOPROFEN", "ARV", "VELES"]
    },
    "BEN-GAY": {
        "etkenMadde": "Metilsalisilat, Mentol",
        "dozaj": "Krem",
        "keywords": ["BENGAY", "BEN-GAY", "MENTOL", "KREM", "POMAD", "BEN", "GAY"]
    },
    "BENEXOL-B12": {
        "etkenMadde": "B1, B6, B12 Vitaminleri",
        "dozaj": "250mg/250mg/1mg",
        "keywords": ["BENEXOL", "B12", "VITAMIN", "B1", "B6", "BENE", "XOL"]
    },
    "COLDWAY-C": {
        "etkenMadde": "İbuprofen, Psödoefedrin, Vitamin C",
        "dozaj": "200mg/30mg/100mg",
        "keywords": ["COLDWAY", "200", "IBUPROFEN", "PSEDOEFEDRIN", "VITAMIN", "WAY", "COLD"]
    },
    "COLEDAN-D": {
        "etkenMadde": "Vitamin D3",
        "dozaj": "Damla",
        "keywords": ["COLEDAN", "COLEDAN-D", "VITAMIN", "D3", "DAMLA", "COLE", "DAN"]
    },
    "DAVIT-3": {
        "etkenMadde": "Vitamin D3",
        "dozaj": "Damla",
        "keywords": ["DAVIT", "DAVIT-3", "VITAMIN", "D3", "DAMLA", "D-VIT"]
    },
    "DOLOREX": {
        "etkenMadde": "Diklofenak",
        "dozaj": "50mg",
        "keywords": ["DOLOREX", "50", "DIKLOFENAK", "DRAJE", "DOLO", "REX"]
    },
    "GAVISCON": {
        "etkenMadde": "Sodyum Aljinat, Sodyum Bikarbonat",
        "dozaj": "Şurup/Tablet",
        "keywords": ["GAVISCON", "ALJINAT", "SODYUM", "SURUP", "MIDE", "GAVIS", "CON"]
    },
    "IBURAMINCOLD": {
        "etkenMadde": "İbuprofen, Psödoefedrin",
        "dozaj": "200mg/30mg",
        "keywords": ["IBURAMIN", "COLD", "IBUPROFEN", "IBURA", "MIN", "ZERO", "PSEDOEFEDRIN"]
    },
    "LANSOR": {
        "etkenMadde": "Lansoprazol",
        "dozaj": "30mg",
        "keywords": ["LANSOR", "30", "LANSOPRAZOL", "MIKROPELLET", "KAPSUL", "LAN", "SOR"]
    },
    "MAJEZIK": {
        "etkenMadde": "Flurbiprofen",
        "dozaj": "100mg",
        "keywords": ["MAJEZIK", "100", "FLURBIPROFEN", "FILM", "TABLET", "MAJ", "EZIK", "SANOVEL", "SANVEL"]
    },
    "PAROL": {
        "etkenMadde": "Parasetamol",
        "dozaj": "500mg",
        "keywords": ["PAROL", "500", "FAROL", "PALF", "PARASETAMOL", "ATABIE", "TABIENPE", "FAR", "OL", "PAL"]
    },
    "RENNIE": {
        "etkenMadde": "Kalsiyum Karbonat, Magnezyum",
        "dozaj": "680mg",
        "keywords": ["RENNIE", "KALSIYUM", "KARBONAT", "CIGNEME", "REN", "NIE", "MAGNEZYUM"]
    },
    "TYLOLHOT": {
        "etkenMadde": "Parasetamol, Psödoefedrin",
        "dozaj": "500mg/60mg",
        "keywords": ["TYLOLHOT", "TYLOL", "HOT", "PARASETAMOL", "TOZ", "POSE", "TYL", "LOL"]
    }
}

ICD_LOCAL_MAP = {
    "K25": "MIDE_ULSERI",
    "K26": "MIDE_ULSERI",
    "K27": "MIDE_ULSERI",
    "K29": "GASTRIT",
    "I10": "HIPERTANSIYON",
    "I11": "HIPERTANSIYON",
    "I12": "HIPERTANSIYON",
    "I13": "HIPERTANSIYON",
    "I15": "HIPERTANSIYON",
    "N18": "BOBREK_YETMEZLIGI",
    "N19": "BOBREK_YETMEZLIGI"
}

NSAII_MADDELER = [
    "NAPROKSEN",
    "DEKSKETOPROFEN",
    "FLURBIPROFEN",
    "DIKLOFENAK",
    "IBUPROFEN",
    "İBUPROFEN"
]

BETA_KARISIM = [
    "PSEDOEFEDRIN",
    "PSÖDOEFEDRİN"
]

BIRIKIM_RISK = [
    "MAGNEZYUM",
    "KALSIYUM"
]

class ScanRequest(BaseModel):
    imageBase64: str


class InteractionRequest(BaseModel):
    semptomlar: List[str] = []
    symptoms: List[str] = []
    icd10Kodlari: List[str] = []
    ilaclar: List[str] = []
    drugs: List[str] = []
    etkenMaddeler: List[str] = []
    active_ingredients: List[str] = []

@app.post("/api/v1/scan")
async def scan_drug(request: ScanRequest):
    print("\n" + "=" * 50)
    print("[SİSTEM] Yeni ilaç tarama isteği geldi.")

    try:
        img_data = base64.b64decode(request.imageBase64)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            return {
                "ilacAdi": "BİLİNMİYOR",
                "etkenMadde": "HATA",
                "guvenSkoru": 0.0,
                "dozaj": "Görüntü bozuk geldi.",
                "modelVersion": "İSUTS YOLO OCR"
            }

        print(f"[SİSTEM] Görüntü açıldı: {img.shape[1]}x{img.shape[0]}")

        results = model(img)
        yolo_brand = None

        for box in results[0].boxes:
            conf = float(box.conf[0])
            if conf >= 0.15:
                class_id = int(box.cls[0])
                yolo_brand = model.names[class_id].upper()
                print(f"[YOLO] {yolo_brand} - Güven: %{round(conf * 100, 2)}")
                break

        ocr_text = ""
        try:
            ocr_text = pytesseract.image_to_string(img, config='--psm 11').upper()
        except Exception as e:
            print("[OCR HATASI]", str(e))

        if not ocr_text.strip():
            gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            ocr_text = pytesseract.image_to_string(gray, config='--psm 11').upper()

        clean_text = re.sub(r'[^A-Z0-9ÇĞİÖŞÜ ]', '', ocr_text)
        print("[OCR TEMİZ]:", clean_text)

        best_match = None
        highest_score = 0

        for drug_name, info in DRUG_DATABASE.items():
            score = 0

            if yolo_brand == drug_name:
                score += 5

            for keyword in info["keywords"]:
                if keyword in clean_text:
                    score += 2

            if score > highest_score and score >= 1:
                highest_score = score
                best_match = drug_name

        if best_match:
            drug_info = DRUG_DATABASE[best_match]
            final_conf = min(0.99, 0.65 + (highest_score * 0.05))

            return {
                "ilacAdi": best_match,
                "etkenMadde": drug_info["etkenMadde"],
                "guvenSkoru": round(final_conf, 2),
                "dozaj": drug_info["dozaj"],
                "modelVersion": "İSUTS YOLO OCR + 17 İlaç Modeli"
            }

        return {
            "ilacAdi": "BİLİNMİYOR",
            "etkenMadde": "BULUNAMADI",
            "guvenSkoru": 0.0,
            "dozaj": "",
            "modelVersion": "İSUTS YOLO OCR + 17 İlaç Modeli"
        }

    except Exception as e:
        return {
            "ilacAdi": "BİLİNMİYOR",
            "etkenMadde": "HATA",
            "guvenSkoru": 0.0,
            "dozaj": str(e),
            "modelVersion": "İSUTS YOLO OCR + 17 İlaç Modeli"
        }


@app.post("/api/v1/analyze")
def analyze_interaction(request: InteractionRequest):
    print("\n" + "=" * 50)
    print("[ANALİZ] Yeni uyuşmazlık analizi isteği geldi.")
    
  
    tum_semptomlar = request.semptomlar + request.symptoms
    tum_ilaclar = request.ilaclar + request.drugs
    tum_etken_maddeler = request.etkenMaddeler + request.active_ingredients
    
    print("[ICD]", request.icd10Kodlari)
    print("[İLAÇ]", tum_ilaclar)
    print("[ETKEN (Gelen)]", tum_etken_maddeler)

   
    aranacak_ilaclar = " ".join(tum_ilaclar).upper()
    for ilac_adi, detaylar in DRUG_DATABASE.items():
        if ilac_adi in aranacak_ilaclar or any(k in aranacak_ilaclar for k in detaylar["keywords"]):
            tum_etken_maddeler.append(detaylar["etkenMadde"].upper())

    if not tum_etken_maddeler:
        return build_response(
            "INSUFFICIENT_DATA",
            "Yetersiz Veri",
            "Analiz yapılacak etken madde bulunamadı.",
            "Sistem"
        )

    icd_groups = resolve_icd_groups(request.icd10Kodlari, tum_semptomlar)
    maddeler_text = normalize_text(" ".join(tum_etken_maddeler))

    rxnorm_result = check_rxnorm(tum_etken_maddeler)
    openfda_result = check_openfda(tum_etken_maddeler)

    local_result = local_rule_engine(
        icd_groups=icd_groups,
        maddeler_text=maddeler_text,
        rxnorm_result=rxnorm_result,
        openfda_result=openfda_result
    )

    return local_result


def resolve_icd_groups(icd_codes: List[str], symptoms: List[str]) -> List[str]:
    groups = []

    for raw_code in icd_codes:
        code = normalize_icd(raw_code)

        if code in ICD_LOCAL_MAP:
            groups.append(ICD_LOCAL_MAP[code])
            continue

        prefix3 = code[:3]
        if prefix3 in ICD_LOCAL_MAP:
            groups.append(ICD_LOCAL_MAP[prefix3])

    symptom_text = normalize_text(" ".join(symptoms))

    if "MIDE" in symptom_text or "ÜLSER" in symptom_text or "ULSER" in symptom_text or "GASTRIT" in symptom_text:
        groups.append("MIDE_ULSERI")

    if "TANSIYON" in symptom_text or "HIPERTANSIYON" in symptom_text or "HİPERTANSİYON" in symptom_text:
        groups.append("HIPERTANSIYON")

    if "BOBREK" in symptom_text or "BÖBREK" in symptom_text:
        groups.append("BOBREK_YETMEZLIGI")

    return list(set(groups))


def local_rule_engine(
        icd_groups: List[str],
        maddeler_text: str,
        rxnorm_result: Dict[str, Any],
        openfda_result: Dict[str, Any]
):
    kaynaklar = []

    if rxnorm_result.get("used"):
        kaynaklar.append("RxNorm")

    if openfda_result.get("used"):
        kaynaklar.append("openFDA")

    kaynaklar.append("Yerel Kural Motoru")

    kaynak_text = " + ".join(kaynaklar)

    if "MIDE_ULSERI" in icd_groups or "GASTRIT" in icd_groups:
        if contains_any(maddeler_text, NSAII_MADDELER):
            return build_response(
                "RED",
                "KRİTİK UYARI: Mide Kanaması Riski",
                "Girilen ICD-10 kodu mide/ülser/gastrit grubunda değerlendirildi. "
                "Aktif ilaç etken maddeleri içinde NSAİİ grubu tespit edildi. "
                "Bu kombinasyon mide kanaması veya mide tahrişi riskini artırabilir.",
                kaynak_text
            )

    if "HIPERTANSIYON" in icd_groups:
        if contains_any(maddeler_text, BETA_KARISIM):
            return build_response(
                "RED",
                "KRİTİK UYARI: Hipertansiyon Riski",
                "Girilen ICD-10 kodu hipertansiyon grubunda değerlendirildi. "
                "Etken maddeler içinde psödoefedrin tespit edildi. "
                "Psödoefedrin tansiyonu yükseltebilir.",
                kaynak_text
            )

    if "BOBREK_YETMEZLIGI" in icd_groups:
        if contains_any(maddeler_text, BIRIKIM_RISK):
            return build_response(
                "RED",
                "KRİTİK UYARI: Toksik Birikim Riski",
                "Girilen ICD-10 kodu böbrek yetmezliği grubunda değerlendirildi. "
                "Magnezyum/kalsiyum içeren ürünler böbrek fonksiyon bozukluğunda birikim riski oluşturabilir.",
                kaynak_text
            )

    if maddeler_text.count("PARASETAMOL") > 1:
        return build_response(
            "YELLOW",
            "DİKKAT: Çift Parasetamol Kullanımı",
            "Aktif ilaç listesinde birden fazla parasetamol içeren ürün tespit edildi. "
            "Aynı etken maddenin tekrarlı kullanımı doz aşımı riski oluşturabilir.",
            kaynak_text
        )

    if openfda_result.get("warning_found"):
        return build_response(
            "YELLOW",
            "DİKKAT: Harici Kaynak Uyarısı",
            "openFDA üzerinden etken maddeyle ilişkili uyarı/önlem bilgisi bulundu. "
            "Yerel kural motorunda kritik kırmızı risk çıkmadı ancak dikkatli kullanım önerilir.",
            kaynak_text
        )

    if rxnorm_result.get("used") or openfda_result.get("used"):
        return build_response(
            "GREEN",
            "Uyuşmazlık Tespit Edilmedi",
            "Harici API sorguları ve yerel kural motoru sonucunda mevcut ICD-10 kodları ile aktif etken maddeler arasında kritik uyuşmazlık bulunmadı.",
            kaynak_text
        )

    return build_response(
        "INSUFFICIENT_DATA",
        "Yetersiz Harici Veri",
        "Harici API kaynaklarından yeterli doğrulama alınamadı. Yerel kural motorunda da eşleşen kritik risk bulunamadı.",
        kaynak_text
    )


def check_rxnorm(etken_maddeler: List[str]) -> Dict[str, Any]:
    result = {
        "used": False,
        "matched_terms": [],
        "errors": []
    }

    for madde in etken_maddeler:
        for token in split_active_ingredients(madde):
            try:
                url = "https://rxnav.nlm.nih.gov/REST/approximateTerm.json"
                params = {
                    "term": token,
                    "maxEntries": 1
                }

                response = requests.get(url, params=params, timeout=5)

                if response.status_code == 200:
                    data = response.json()
                    candidates = data.get("approximateGroup", {}).get("candidate", [])

                    if candidates:
                        result["used"] = True
                        result["matched_terms"].append({
                            "input": token,
                            "rxcui": candidates[0].get("rxcui"),
                            "score": candidates[0].get("score")
                        })

            except Exception as e:
                result["errors"].append(str(e))

    return result


def check_openfda(etken_maddeler: List[str]) -> Dict[str, Any]:
    result = {
        "used": False,
        "warning_found": False,
        "matched_terms": [],
        "errors": []
    }

    for madde in etken_maddeler:
        for token in split_active_ingredients(madde):
            search_term = token.lower()

            try:
                url = "https://api.fda.gov/drug/label.json"
                params = {
                    "search": f'openfda.generic_name:"{search_term}"',
                    "limit": 1
                }

                response = requests.get(url, params=params, timeout=5)

                if response.status_code == 200:
                    data = response.json()
                    results = data.get("results", [])

                    if results:
                        result["used"] = True
                        result["matched_terms"].append(token)

                        label = results[0]
                        warning_keys = [
                            "warnings",
                            "precautions",
                            "contraindications",
                            "adverse_reactions"
                        ]

                        for key in warning_keys:
                            if key in label and label[key]:
                                result["warning_found"] = True
                                break

            except Exception as e:
                result["errors"].append(str(e))

    return result


def split_active_ingredients(raw: str) -> List[str]:
    if not raw:
        return []

    text = raw.replace("İ", "I")
    parts = re.split(r"[,/+]", text)

    cleaned = []
    for part in parts:
        item = part.strip()
        item = re.sub(r"[^A-Za-zÇĞİÖŞÜçğıöşü0-9 ]", "", item)

        if item and len(item) >= 3:
            cleaned.append(item)

    return cleaned


def contains_any(text: str, keywords: List[str]) -> bool:
    for keyword in keywords:
        if normalize_text(keyword) in text:
            return True
    return False


def normalize_icd(code: str) -> str:
    if not code:
        return ""

    code = code.strip().upper()
    code = code.replace(" ", "")
    code = code.replace(".", "")

    return code[:3] if len(code) >= 3 else code


def normalize_text(text: str) -> str:
    if not text:
        return ""

    text = text.upper()
    text = text.replace("İ", "I")
    text = text.replace("Ş", "S")
    text = text.replace("Ğ", "G")
    text = text.replace("Ü", "U")
    text = text.replace("Ö", "O")
    text = text.replace("Ç", "C")
    return text


def build_response(risk_level: str, baslik: str, aciklama: str, kaynak: str):
    return {
        "risk_level": risk_level,
        "baslik": baslik,
        "aciklama": aciklama,
        "kaynak": kaynak
    }


@app.get("/")
async def root():
    return {
        "status": "İSUTS API aktif",
        "scan_endpoint": "/api/v1/scan",
        "analyze_endpoint": "/api/v1/analyze"
    }