# NAPSAK Ürün Gereksinimleri Dokümanı (PRD)

NAPSAK, arkadaş gruplarının veya çiftlerin yaşadığı klasik "Ne yapalım?", "Nereye gidelim?", "Ne yesek?" problemini çözmek için tasarlanmış gerçek zamanlı bir sosyal karar verme uygulamasıdır.

## 🎯 Temel Konsept
1. Bir kişi oda oluşturur (Host).
2. Katılımcılar paylaşılan bağlantı veya QR kod ile odaya katılır (Web veya Mobil).
3. Herkes bağımsız ve anonim şekilde seçenekleri oylar (Tinder usulü sağa/sola kaydırma).
4. Sistem oyları toplar, analiz eder ve en çok tercih edilen seçeneği belirler. Eşitlik durumunda kendisi karar verir.

---

## 🛠️ MVP (Minimum Uygulanabilir Ürün) Kapsamı

### Dahil Olan Özellikler:
*   **Kategori:** Sadece Restoran ve Yemek seçimi (MVP'de diğer kategoriler yok).
*   **Oda Yönetimi:**
    *   Oda oluşturma (Host).
    *   Paylaşılabilir davet bağlantısı ve QR kod üretimi.
    *   Gerçek zamanlı katılımcı takibi (Kimler katıldı, kimler hazır).
    *   "Hazırım" sistemi.
*   **Oylama:**
    *   Tinder tarzı kaydırmalı (Swipe) oylama.
    *   Anonimlik (Kimse kimin neye oy verdiğini oylama sırasında göremez).
*   **Sonuç ve Karar:**
    *   Sonuç hesaplama algoritması.
    *   Eşitlik bozma mekanizması (Eşitlikte NAPSAK rastgele birini seçer).
    *   Haritada aç butonu (Seçilen restoran için).

### MVP Dışı Özellikler (Sonraki Sürümler):
*   Kullanıcı hesabı / kayıt / sosyal giriş.
*   Oda içi sohbet (Chat).
*   Profil sayfaları.
*   Film/Dizi, Etkinlik vb. diğer kategoriler.
*   Turnuva modu.
*   İstatistikler.

---

## 🔄 Kullanıcı Akışı ve Ekranlar

### 1. Ana Sayfa (Home Screen)
*   **Buton:** "N'APSAK?" (Oda oluşturur).
*   **Giriş Alanı:** Oda kodu ile mevcut bir odaya katılma.

### 2. Lobi Ekranı (Lobby Screen)
*   **QR Kod & Davet Linki:** Arkadaşları davet etmek için.
*   **Katılımcı Listesi:** Odaya katılanların listesi ve "Hazır" (Ready) durumları.
*   **Başlat Butonu:** Sadece Oda Sahibi (Host) görebilir ve oylamayı başlatabilir (tüm üyeler hazır olduğunda).

### 3. Oylama Ekranı (Voting Screen)
*   Kartlar halinde yemek/restoran seçenekleri gelir.
*   **Etkileşim:** Sağa Kaydır (Beğendim) / Sola Kaydır (Beğenmedim).

### 4. Sonuç Ekranı (Result Screen)
*   Kazanan restoran kartı.
*   Kutlama animasyonu.
*   "Haritada Aç" butonu.

---

## 🎛️ Oda Durumları (Room States)
*   `WAITING`: Katılımcılar odaya katılıyor.
*   `READY`: Tüm katılımcılar hazır.
*   `VOTING`: Oylama devam ediyor.
*   `RESULT`: Kazanan hesaplandı ve gösteriliyor.
