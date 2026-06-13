# NAPSAK Geliştirme Yol Haritası ve Planı

Bu doküman, NAPSAK projesini adım adım hayata geçirmek için izleyeceğimiz planı ve teknik mimariyi tanımlar.

## 🛠️ Teknik Mimari Kararları

Projenin sürdürülebilirliği, test edilebilirliği ve temiz kod prensipleri için aşağıdaki teknik kararlar alınmıştır:

### 1. Katmanlı Mimari (Clean Architecture + MVVM)
Uygulama üç ana katmandan oluşacaktır:
*   **Data Katmanı:** Firebase Realtime DB veri kaynakları, DTO'lar ve Repository implementasyonları.
*   **Domain Katmanı (Pure Kotlin):** İş mantığı (Use Case'ler), saf Kotlin veri modelleri ve Repository arayüzleri. (Örn: `CreateRoomUseCase`, `CalculateWinnerUseCase`).
*   **Presentation (UI) Katmanı:** Jetpack Compose ekranları, UI bileşenleri ve Compose durumlarını (State) yöneten `ViewModel` sınıfları.

### 2. Klasör ve Paket Yapısı (`com.napsak.app`)
*   `data/` -> `model/` (DTOs), `datasource/`, `repository/`
*   `domain/` -> `model/` (Entities), `repository/` (Interfaces), `usecase/`
*   `ui/` -> `screens/` (home, lobby, voting, result), `components/`, `navigation/`, `theme/`
*   `di/` -> Hilt modülleri
*   `utils/` -> Yardımcı sınıflar

### 3. Kullanılacak Teknolojiler & Kütüphaneler
*   **Dependency Injection:** Hilt (Clean DI için).
*   **Navigation:** Type-Safe Jetpack Compose Navigation (Kotlin 2.0+ uyumlu).
*   **Local Storage:** Jetpack DataStore (Preferences). Kullanıcı adı (`username`) ve tekil cihaz kimliği (`userId`) saklamak için.
*   **Room Database:** ❌ Kullanılmayacak. Real-time veri senkronizasyonu Firebase önbelleği ile çözüldüğü ve yerel veri tabanı ihtiyacı (çevrimdışı oylama vb.) olmadığı için gereksiz karmaşıklık yaratmaması adına tercih edilmemiştir.
*   **Görsel Yükleme:** Coil (Yemek/Restoran kartlarında asenkron görseller için).

---

## 📅 Adım Adım Geliştirme Planı

### 📍 Adım 1: Klasör Yapısının ve Bağımlılıkların Kurulması (Şu anki Adım)
*   [x] `build.gradle.kts` ve `libs.versions.toml` dosyalarının güncellenmesi (Hilt, Navigation, DataStore, Firebase eklenmesi).
*   [x] Clean Architecture klasör ve paket yapısının oluşturulması.
*   [x] Hilt'in `Application` sınıfı seviyesinde başlatılması.

### 📍 Adım 2: Veri Modeli, Use Case'ler ve Firebase Entegrasyonu
*   [ ] Firebase projesinin kurulması ve Android uygulamasına dahil edilmesi.
*   [ ] Domain katmanında `Room`, `Participant`, `Option` modellerinin ve Use Case'lerin tanımlanması.
*   [ ] Firebase Realtime Database entegrasyonu ve Data katmanı repository implementasyonu.

### 📍 Adım 3: UI Tasarımları & Ekran Geliştirmeleri (Host & Katılımcı)
*   [ ] **Home (Ana Sayfa) Ekranı:** Kullanıcı adı girişi, "Oda Oluştur" ve "Oda Koduyla Katıl" alanları.
*   [ ] **Lobby (Lobi) Ekranı:** Katılımcı listesi (Real-time), hazır durumu butonu, Host için başlatma butonu, QR/Davet linki paylaşımı.
*   [ ] **Voting (Oylama) Ekranı:** Tinder-Swipe (kart kaydırma) animasyonlu oylama bileşeni.
*   [ ] **Result (Sonuç) Ekranı:** Kazanan seçeneğin gösterilmesi, kutlama animasyonu, haritada açma butonu.

### 📍 Adım 4: Web Katılımcı Uygulamasının Geliştirilmesi (Mobil Web)
*   [ ] Web arayüzünün (React/Vite veya Vanilla JS + Firebase) kurulması.
*   [ ] Web lobisi, hazır olma ve Tinder-Swipe oylama arayüzlerinin geliştirilmesi.

### 📍 Adım 5: Test, Eşitlik Algoritması ve Polish
*   [ ] Eşitlik durumlarında rastgele kazanan belirleme mantığının test edilmesi.
*   [ ] Mobil-Web arası gerçek zamanlı veri eşleşme testleri.
