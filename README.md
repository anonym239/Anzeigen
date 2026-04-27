# 🛍️ Flohmarkt Finder

Eine native Android-App (Kotlin + Jetpack Compose) zum Finden von Flohmärkten, Haushaltsauflösungen und Trödelmärkten in der Nähe.

## ✨ Features

- 🔍 **Suche** nach Flohmärkten, Haushaltsauflösungen und Trödelmärkten
- 📍 **Ortsbasierte Suche** – Ort eingeben und Umkreis filtern
- 🏷️ **Kategoriefilter** – Flohmarkt / Haushaltsauflösung / Trödelmarkt / Alles
- 📅 **Datumsfilter** – Heute / Diese Woche / Diesen Monat
- ⭐ **Favoriten** – Einträge lokal speichern (Room-Datenbank)
- 🔗 **Direktlinks** – Originalanzeige im Browser öffnen
- 🔄 **Pull-to-Refresh** – Ergebnisse aktualisieren
- 🎨 **Material You** – Dynamische Farben (Android 12+)
- 🔤 **Große Schrift** – Optimiert für ältere Nutzer

## 🏗️ Technischer Stack

| Komponente | Technologie |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architektur | MVVM + Clean Architecture |
| DI | Hilt |
| Netzwerk | Retrofit + OkHttp |
| Datenbank | Room |
| Bilder | Coil |
| Animationen | Lottie |
| State | StateFlow + ViewModel |
| Navigation | Navigation Compose |
| API | OpenRouter (GPT-4o-mini) |

## 🚀 Setup

### 1. API-Key eintragen

Öffne `app/src/main/java/com/anonym239/flohmarkt/util/Constants.kt` und trage deinen OpenRouter API-Key ein:

```kotlin
const val OPENROUTER_API_KEY = "sk-or-v1-DEIN-API-KEY-HIER"
```

API-Key bekommst du kostenlos auf: https://openrouter.ai

### 2. APK bauen

**Via GitHub Actions (empfohlen):**
1. Code zu GitHub pushen
2. GitHub Actions baut automatisch die APK
3. APK unter "Releases" oder "Actions → Artifacts" herunterladen

**Lokal bauen:**
```bash
./gradlew assembleDebug
```
APK liegt dann unter: `app/build/outputs/apk/debug/app-debug.apk`

### 3. APK installieren

1. APK auf das Android-Gerät übertragen
2. In den Einstellungen "Installation aus unbekannten Quellen" erlauben
3. APK antippen und installieren

## 📱 Mindestanforderungen

- Android 12 (API 31) oder höher
- Internetverbindung für die Suche

## 🏛️ Projektstruktur

```
app/src/main/java/com/anonym239/flohmarkt/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── database/     # Room Database
│   │   └── entity/       # Room Entities
│   ├── remote/
│   │   ├── api/          # Retrofit API Service
│   │   └── dto/          # Data Transfer Objects
│   └── repository/       # Repository Implementierung + Mapper
├── di/                   # Hilt Module
├── domain/
│   ├── model/            # Domain Models
│   └── repository/       # Repository Interface
├── ui/
│   ├── components/       # Wiederverwendbare Compose-Komponenten
│   ├── navigation/       # Navigation Graph
│   ├── screens/
│   │   ├── detail/       # Detailansicht
│   │   ├── favorites/    # Favoritenliste
│   │   └── home/         # Hauptbildschirm
│   └── theme/            # Material You Theme
└── util/                 # Hilfsfunktionen (Result, Constants)
```

## 📄 Lizenz

MIT License – Frei verwendbar
