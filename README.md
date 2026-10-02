# AionUi Remote Companion

Nezvanična Android aplikacija koja prikazuje postojeći AionUi WebUI preko ugrađenog WebView-a. AionUi, agenti i dokumenti ostaju na računaru; telefon je samo udaljeni klijent.

## Korišćenje

1. Instaliraj i poveži Tailscale na Android telefonu.
2. Pokreni AionUi WebUI na Macu i uključi **Allow Remote Access**.
3. Pokreni AionUi Remote Companion i unesi Tailscale IPv4 adresu Maca, na primer `100.x.x.x`, ili njegov MagicDNS naziv.
4. Aplikacija automatski dodaje port `25808` i otvara AionUi preko celog ekrana. Prijavi se postojećim AionUi načinom prijave.

Adresa se čuva samo lokalno u podešavanjima aplikacije na telefonu i ponovo se povezuje pri sledećem pokretanju. Lozinka i pairing kod se ne čuvaju u aplikaciji. Android Back vraća na ekran za promenu adrese ili prethodnu WebUI stranicu. Sistemsku navigaciju možeš privremeno prikazati prevlačenjem od ivice ekrana.

Aplikacija pri pokretanju proverava GitHub Releases i automatski preuzima noviju stabilnu verziju. Android zatim prikazuje sistemsku potvrdu instalacije; tiha instalacija nije dostupna običnim Android aplikacijama. Pri prvom ažuriranju Android može tražiti dozvolu da AionUi Remote instalira APK fajlove.

WebUI prati sistemsku svetlu/tamnu temu Androida kada je AionUi Appearance podešen na **System**. Pri promeni sistemske teme Android ponovo pokreće prikaz i aplikacija se vraća na sačuvani AionUi računar.

Aplikacija prihvata Tailscale IPv4 opseg, privatne LAN IPv4 adrese, MagicDNS imena koja se završavaju sa `.ts.net`, kratka imena uređaja i `.local` imena. IPv6 adrese trenutno nisu podržane. Za udaljeni pristup preporučena je Tailscale adresa ili MagicDNS ime.

## Fajlovi

- Izbor fajlova za slanje u AionUi otvara Android birač dokumenata.
- Preuzimanja sa AionUi servera čuvaju se u fascikli `Downloads`.
- HTTP linkovi ka drugim hostovima su blokirani; spoljašnji HTTPS linkovi otvaraju se u sistemskom browseru.

## Mreža i privatnost

Aplikacija ne šalje telemetriju i nema nalog ili sopstveni server. Povezuje se direktno sa privatnom adresom koju uneseš. Ako je telefon povezan na Tailscale, Tailscale šifruje saobraćaj između telefona i Maca. Na lokalnoj mreži van Tailscale-a, AionUi HTTP veza nema dodatni TLS sloj.

Unesi samo adresu svog AionUi računara kome veruješ. Ne prosleđuj port `25808` na javni internet. Za pristup se i dalje koristi AionUi prijava. AionUi WebUI u trenutnoj verziji ima jedan zajednički `admin` nalog.

Ovo je nezvanični projekat zajednice i nije povezan sa autorima AionUi-ja ili Tailscale-a.

## Instalacija

Preuzmi najnoviji `AionUi-Remote-Companion-*.apk` iz [GitHub Releases](https://github.com/milev051/aionui-remote-companion/releases/latest). Android može tražiti dozvolu za instalaciju APK fajlova.

## Izgradnja iz izvornog koda

Zahteva JDK 17 i Android SDK platformu 34.

```sh
cd android
./gradlew assembleRelease
```

APK nastaje u `android/app/build/outputs/apk/release/app-release.apk`.
