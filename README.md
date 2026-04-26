# Mobilny alkomat - aplikacja do sprawdzenia stanu alkoholu we krwi

Aplikacja stworzona jest w Kotlinie wraz z wykorzystaniem chmury Firestore, pozwala sprawdzić ile osoba badająca się miała we alkoholu we krwi oraz jak dużo czasu będzie potrzebować aby całkowicie wytrzeźwieć. Projekt stworzony w celach naukowych jak i w celach czysto funkcjonalnych.

## Funkcjonalności
1. Możliwość sprawdzenia ilości alkoholu we krwi w szczytowym momencie.
2. Możliwość sprawdzenia czasu jakiego potrzebujemy aby całkowicie wytrzeźwieć.
3. Logowanie i rejestracja pozwala korzystać wielu osobom na raz.
4. Dane wpisywane w aplikacji są przechowywane w chmurze co pozwala na weryfikację osoby i momentu sprawdzenia trzeźwości.
5. Mapa pokazuje wykres zmieniania się poziomu alkoholu w czasie.
6. Aplikacja w celu upewnienia się czy jest się trzeźwym, ma opcje pokazania w mapach Google komisariaty policji.
7. Aplikacja wysyła powiadomienie w celu przypomnienia czy faktycznie jesteśmy trzeźwi.

## Wygląd działania aplikacji
<table>
  <tr>
    <td align="center">
      <img src="ss/2.png" width="320"/><br/>
      <sub>ekran główny</sub>
    </td>
    <td align="center">
      <img src="ss/3.png" width="320"/><br/>
      <sub>ekran główny</sub>
    </td>
    <td align="center">
      <img src="ss/4.png" width="320"/><br/>
      <sub>miejsce z wynikami </sub>
    </td>
    <td align="center">
      <img src="ss/5.png" width="320"/><br/>
      <sub>wygląd wykresu</sub>
    </td>
    <td align="center">
      <img src="ss/1.png" width="320"/><br/>
      <sub>wygląd map google</sub>
    </td>
</table>



## Uruchomienie aplikacji
1. Pobierz Android Studio wraz z (Android SDK, Emulator, Android Virtual Device) ze strony: https://developer.android.com/studio
2. Uruchom Android Studio.
3. Na ekranie powitalnym kliknij przycisk "Get from VCS" lub jeśli masz już otwarty inny projekt, wybierz z górnego paska: File -> New -> Project from Version Control.
5. W polu URL wklej link do tego repozytorium: https://github.com/noxxgk/Licencjat.git wybierz folder docelowy na swoim dysku i kliknij Clone.
6. Poczekaj na pobranie zależności (Gradle), jeśli u góry pojawi się "Sync Now" kliknij go, jeśli pojawią się błędy o brakujących składnikach SDK kliknij "Install missing component", (ten krok może chwilkę potrwać).
7. W razie błędów sprawdź konfigurację SDK, File -> Project Structure -> Default Config, target SDK = 35 , min SDK version = 24, jeśli tego brak Android Studio zaproponuje pobranie.
8. W przypadku braku błędów otwórz emulator, Tools -> Device Manager -> Create device, należy w wybrać dowolny model telefonu np. Pixel 8 oraz wybieramy jego system API 35, pobieramy obraz systemu oraz włączamy emulator.
