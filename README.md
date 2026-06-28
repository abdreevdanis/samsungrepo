# Essential

Android-приложение для личных markdown-заметок и ИИ-чата с контекстом из ваших материалов.

**Автор:** Абдреев Данис Ринатович  
**Конкурс:** IT Школа Samsung - финал 2026, номинация «Программирование»  
**Версия:** 1.4.0 (versionCode 11)  
**Package:** `ru.myessentiality.essential`

## Возможности

- Vault с markdown-заметками на устройстве
- Редактор, wiki-ссылки, граф заметок (Louvain)
- ИИ-чат с RAG: FTS + семантический индекс
- Локальный режим: LiteRT-LM / llama.cpp
- Облачный режим: REST API, квоты Free/Pro
- Шифрованная синхронизация snapshot (AES-256-GCM)
- Голосовой ввод, вложения, heatmap активности

## Сборка

Требования: JDK 17+, Android SDK 36, NDK 27.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Windows:

```powershell
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

## APK

Скачайте из [Releases](https://github.com/abdreevdanis/samsung/releases).

## Сервер

Production API: `https://myessentiality.ru`

## Структура

| Путь | Описание |
|------|----------|
| `app/` | Android-клиент (Kotlin, Compose, Hilt, Room) |
| `app/.../data/` | API-клиент, vault, sync, chat, index, Room |
| `app/.../ui/` | Compose-экраны |
| `app/.../litert/` | LiteRT-LM |
| `app/.../locale/` | Локализация RU/EN/ES |
| `app/.../service/` | Фоновые сервисы |

## Тесты

38+ unit-тестов: криптография vault, JWT, markdown, семантический поиск, Louvain, маршрутизация чата.

