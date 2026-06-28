# Essential

Android-приложение для личного vault заметок в Markdown и ИИ-чата с контекстом из ваших материалов. Поддерживает локальный и облачный режимы работы нейросетевого ассистента.

**Автор:** Абдреев Данис Ринатович  
**Конкурс:** IT Школа Samsung — финал 2026, номинация «Программирование»  
**Версия:** 1.4.0 (versionCode 11)  
**Package:** `ru.myessentiality.essential`

## Назначение проекта

Essential предназначен для пользователей, которым нужно хранить знания в виде связанных markdown-заметок и быстро получать ответы ИИ на основе собственного архива — без передачи всего vault в облако. Приложение объединяет редактор заметок, семантический поиск и гибридный ИИ-стек (офлайн на устройстве или через REST API).

## Ключевые возможности и архитектура систем

### 1. Vault заметок (Markdown)

Хранилище заметок работает через Storage Access Framework (внешняя папка) или внутреннее хранилище приложения. Поддерживаются только файлы `*.md`.

Каждая заметка индексируется в Room: заголовок, путь, фрагмент текста, время изменения. Wiki-ссылки `[[Заголовок]]` превращаются в рёбра графа (`WikiEdgeEntity`) для навигации и визуализации связей.

### 2. RAG-чат по личным заметкам

Перед ответом ИИ выполняется поиск релевантных фрагментов в vault:

- **FTS5 / FTS4** — полнотекстовый поиск через виртуальную таблицу `note_fts` (с автоматическим fallback FTS4 на устройствах без FTS5).
- **Семантический индекс (TF-IDF)** — `SemanticIndex` строит векторы по чанкам заметок и ранжирует cosine similarity.
- **VaultContextHelper** — собирает компактный контекст (до ~3500 символов) и список источников для отображения в UI.

Найденные фрагменты передаются в system prompt; пользователь видит карточки источников под ответом.

### 3. Гибридный ИИ (локальный + облачный)

`ChatEngine` маршрутизирует запросы:

| Режим | Когда используется |
|-------|-------------------|
| **Локальный** | Нет облачного режима, нет вложений — LiteRT-LM и/или llama.cpp (GGUF через JNI) |
| **Essential API** | Авторизация на `https://myessentiality.ru`, квоты Free/Pro |
| **OpenAI-compatible / Gemini** | Debug-сборка с ключами в `local.properties` |

Локальный стек:

- **LiteRT-LM** — модели `.litertlm`, GPU/CPU backend, streaming-ответы (`LiteRtLmRunner`, `HybridLocalLlmEngine`).
- **llama.cpp (NDK/JNI)** — GGUF-модели, native-библиотека `libessential_llama.so`.
- **LocalWebResearch** — опциональный веб-контекст для локального режима при наличии сети.

Облачный режим проверяет online-статус через `OnlineChecker` / `NetworkMonitor` и показывает понятные offline-сообщения.

### 4. Шифрованная синхронизация snapshot

`VaultSyncRepository` упаковывает vault в ZIP, шифрует **AES-256-GCM** с ключом из **PBKDF2** (120 000 итераций, SHA-256) и загружает на сервер. Конфликты решаются правилом «побеждает последний snapshot».

### 5. Граф заметок (Louvain)

Для визуализации кластеров связей реализован алгоритм **Louvain** (`ui/graph/Louvain.kt`) — community detection по рёбрам wiki-графа. Пользователь видит группы связанных тем на экране графа.

### 6. Редактор, локализация, UX

- Jetpack Compose + Material 3, кастомная тема, анимации переходов.
- Markdown-рендер: код, формулы (JLatexMath), подсветка синтаксиса.
- Локализация **RU / EN / ES** (`locale/AppLocales.kt`, `values-ru`, `values-en`, `values-es`).
- Голосовой ввод, вложения в чат, heatmap активности, экспорт переписки.

## Архитектура кодовой базы

Проект использует **Dagger Hilt** для DI и модульную структуру пакетов.

```
app/src/main/java/com/rassvet/essential/
|
|- data/
|  |- api/           - EssentialApi, JWT, OpenAI/Gemini клиенты, streaming
|  |- chat/          - ChatEngine, экспорт, вложения, маршрутизация LLM
|  |- index/         - IndexRepository, SemanticIndex (TF-IDF)
|  |- llm/           - LiteRT-LM, llama.cpp, каталог моделей, загрузка GGUF
|  |- local/         - Room (AppDatabase, DAO, FTS5/FTS4, миграции)
|  |- network/       - NetworkMonitor, OnlineChecker
|  |- security/       - PassphraseKeystore
|  |- sync/           - VaultCryptography, VaultSyncRepository
|  |- vault/          - VaultDocuments (SAF), чтение/запись markdown
|
|- di/                - DatabaseModule, RepositoryModule (Hilt)
|
|- litert/            - LiteRtNativeBootstrap (JNI / GPU sampling)
|
|- locale/            - AppLocales, RelativeTime, подписи тарифов
|
|- service/           - LocalModelDownloadService (фоновая загрузка моделей)
|
|- ui/
|  |- auth/           - вход, регистрация
|  |- chat/           - экран чата, карточки источников
|  |- editor/         - редактор и рендер markdown
|  |- graph/          - граф заметок, Louvain
|  |- home/           - список заметок, главный экран, heatmap
|  |- onboarding/     - первый запуск, создание vault
|  |- settings/       - настройки, квоты, сессии
|  |- splash/         - splash screen
|  |- theme/          - Material 3 тема
|  |- markdown/       - парсер и блоки markdown
|
|- EssentialApplication.kt, MainActivity.kt, EssentialApp.kt
```

Native-код: `app/src/main/cpp/` — CMake, llama.cpp (third_party), JNI-мост `essential_jni.cpp`.

## Используемый стек технологий

| Категория | Технологии |
|-----------|------------|
| Язык | Kotlin (+ Java для части data-слоя) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| СУБД | Room (SQLite), FTS5/FTS4 |
| DI | Dagger Hilt |
| Сеть | OkHttp, Ktor-style REST (`EssentialApi`) |
| ИИ (локально) | LiteRT-LM, llama.cpp (GGUF, NDK 27) |
| ИИ (облако) | REST API Essential, OpenAI-compatible, Gemini |
| Хранилище настроек | DataStore Preferences |
| Шифрование | AES-256-GCM, PBKDF2 |
| Тесты | JUnit, Robolectric, Room Testing, Coroutines Test |

## Требования для сборки

- **JDK 17+**
- **Android SDK 36** (minSdk **24**, targetSdk **36**)
- **NDK 27.x** (native-модули llama.cpp)
- **CMake 3.22+**

Для debug-сборки с прямым облаком (опционально) — ключи в `local.properties` (не коммитить):

```properties
sdk.dir=C\:\\path\\to\\Android\\Sdk
GEMINI_API_KEY=
OPENAI_COMPAT_API_KEY=
OPENAI_COMPAT_BASE_URL=
```

## Подготовка и запуск приложения

### 1. Клонирование репозитория

```bash
git clone https://github.com/abdreevdanis/samsung.git
cd samsung
```

### 2. Сборка debug APK

```bash
./gradlew :app:assembleDebug
```

Windows:

```powershell
gradlew.bat :app:assembleDebug
```

### 3. Установка на устройство

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Готовый APK также доступен в [Releases](https://github.com/abdreevdanis/samsung/releases) — тег **v1.4.0**.

## Сервер и тестовый вход

Production API: **https://myessentiality.ru**

| | |
|---|---|
| Сервер | `https://myessentiality.ru` |
| Логин | *(demo-аккаунт для жюри — см. презентацию)* |
| Пароль | *(см. презентацию)* |

Debug-сборка без сервера: логин `admin`, пароль `admin`.

## Тестирование проекта

### Запуск unit-тестов

```bash
./gradlew :app:testDebugUnitTest
```

Windows:

```powershell
gradlew.bat :app:testDebugUnitTest
```

### Запуск instrumented-тестов

```bash
./gradlew :app:connectedAndroidTest
```

### Структура тестов

Тесты расположены в `app/src/test/java/com/rassvet/essential/` (38+ unit-тестов):

| Файл | Назначение |
|------|------------|
| `data/sync/VaultCryptographyTest.kt` | AES-GCM, PBKDF2, round-trip шифрования |
| `data/index/SemanticIndexTest.kt` | TF-IDF индекс, ранжирование фрагментов |
| `data/chat/VaultContextHelperTest.kt` | сбор RAG-контекста для чата |
| `data/chat/ChatEngineRoutingTest.kt` | маршрутизация local / cloud |
| `data/chat/ChatAttachmentJsonTest.kt` | сериализация вложений |
| `data/api/JwtPayloadTest.kt` | разбор JWT payload |
| `data/api/ApiBaseUrlsTest.kt` | базовые URL API |
| `data/llm/LlamaRuntimeTuningTest.kt` | параметры llama.cpp runtime |
| `data/llm/LocalWebResearchTest.kt` | локальный веб-контекст |
| `ui/graph/LouvainTest.kt` | алгоритм кластеризации графа |
| `ui/markdown/MarkdownParserTest.kt` | разбор markdown-блоков |
| `locale/RelativeTimeTest.kt` | относительное время (RU/EN/ES) |
| `data/chat/ChatDeviceClockTest.kt` | синхронизация времени устройства |

## Контакты

- https://myessentiality.ru
- https://github.com/abdreevdanis/samsung
