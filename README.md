# Licencjat
## Uruchomienie aplikacji
1. Pobierz Android Studio wraz z (Android SDK, Emulator, Android Virtual Device) ze strony: https://developer.android.com/studio
2. Uruchom Android Studio.
3. Na ekranie powitalnym kliknij przycisk "Get from VCS" lub jeśli masz już otwarty inny projekt, wybierz z górnego paska: File -> New -> Project from Version Control.
5. W polu URL wklej link do tego repozytorium: https://github.com/noxxgk/Licencjat.git wybierz folder docelowy na swoim dysku i kliknij Clone.
6. Poczekaj na pobranie zależności (Gradle), jeśli u góry pojawi się "Sync Now" kliknij go, jeśli pojawią się błędy o brakujących składnikach SDK kliknij "Install missing component", (ten krok może chwilkę potrwać).
7. W razie błędów sprawdź konfigurację SDK, File -> Project Structure -> Default Config, target SDK = 35 , min SDK version = 24, jeśli tego brak Android Studio zaproponuje pobranie.
8. W przypadku braku błędów otwórz emulator, Tools -> Device Manager -> Create device, należy w wybrać dowolny model telefonu np. Pixel 8 oraz wybieramy jego system API 35, pobieramy obraz systemu oraz włączamy emulator.
9. Klikamy run czyli shift + F10 lub zielony trójkąt na górnym pasku.
