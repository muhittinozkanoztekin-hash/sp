# Sunplus Remote (v0.1 - test prototip)

Ucast v5.0.1 APK'sının decompile edilmesiyle çıkarılan protokole göre
yazıldı. Vendor hiçbir yerde bu protokolü dokümante etmiyor - hepsi
`classes.dex` ve `libnative_lib.so` içinden çıkarıldı.

## Bu ne yapıyor

- UDP 8998'i dinleyip kutunun discovery broadcast'ini yakalıyor (ip, isim,
  sPort, dvbSPort çözülüyor)
- Remote'daki HER tuşu gönderebiliyor (yön tuşları, kanal/ses +-, 0-9,
  renkli tuşlar, play/pause/stop/ff/rw, epg, fav, menu, vs - RemoteFragment.java'daki
  tam liste)

## Bu ne yapMIYOR (henüz)

Kanal listesi çekme, kanal değiştirme, EPG verisi, oynatma URL'si - bunlar
apk'da farklı ve daha karmaşık bir mekanizma kullanıyor (UDP + telefonun
kendi üstünde çalıştırdığı bir HTTP callback sunucusu). Remote çalıştığı
doğrulanınca bir sonraki adım olarak eklenebilir.

## Önemli: hiç test edilmedi

Bu kod bende (sandbox'ta) hiç senin ağına erişimim olmadığı için telefonda
çalıştırılıp kutuna karşı denenmedi - sadece decompile edilen kaynağa
bakarak yazıldı. Yani ilk açılışta bir şeyler kırık çıkabilir; birlikte
düzeltiriz.

## APK'yı nasıl alırsın

Ben (bu sandbox'ta) APK'yı derleyemiyorum - Android SDK'yı ve Gradle'ın
kendisini indirmem gereken sunucular (dl.google.com, maven.google.com,
services.gradle.org) burada engelli, denedim, 403 dönüyor. İki seçenek var:

**A) GitHub Actions ile (Android Studio'suz, hazır .apk indirirsin)**
1. Bu klasörü bir GitHub reposuna push'la (`.github/workflows/build.yml`
   zaten içinde)
2. GitHub'da repo > Actions sekmesine git, workflow otomatik çalışmış
   olacak (push'ta tetikleniyor); bitince Actions run'ının altındaki
   "Artifacts" bölümünden `sunplus-remote-debug-apk` diye inip .apk'yı
   indir
3. Telefona at, "bilinmeyen kaynaklardan yükleme" iznini aç, kur

**B) Android Studio ile**
1. `File > Open` ile bu klasörü aç, Gradle sync'i bekle
2. Run'a bas (fiziksel telefon USB'de veya emulatorde)

Hangisini seçersen seç, sonrası aynı:
1. Telefonunu kutuyla AYNI Wi-Fi ağına bağlı tut
2. Orijinal Ucast uygulaması açıksa kapat (aynı 8998 portunu dinliyor,
   çakışma ihtimaline karşı)
3. Uygulamayı aç - üstte "Cihaz aranıyor..." yazacak, birkaç saniye içinde
   kutunun adı/IP'siyle güncellenmesi lazım
4. Bulunca herhangi bir tuşa bas (KANAL +, yön tuşları, vs.) ve kutuda bir
   tepki olup olmadığına bak

## Neden native_lib.so'yu aynen kopyaladık

Her mesaj (discovery, tuş, kanal komutu) göndermeden önce özel bir
encode/decode'dan geçiyor - bu, native (ARM) kütüphaneye derlenmiş,
algoritmayı çözmedik. Kütüphane JNI'yi `com/example/ucast/LocalApplication`
sınıf yoluna sabit kayıtlı, o yüzden `LocalApplication.kt` bu paket/isimle
aynen durmalı - taşınırsa veya yeniden adlandırılırsa native çağrılar
çözülemez.

## sPort / dvbSPort her kutuda farklı

Bunlar sabit değil - kutunun kendisi discovery broadcast'inde yayınlıyor.
O yüzden IP'yi biliyor olsak bile (192.168.1.11) doğru portu öğrenmek için
discovery adımı gerekli; hardcode edilmedi.
