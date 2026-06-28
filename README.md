# Essential

Android-приложение для личного vault заметок в Markdown и ИИ-чата с контекстом из ваших материалов. Поддерживает локальный и облачный режимы работы нейросетевого ассистента.

**Автор:** Абдреев Данис Ринатович  
**Конкурс:** IT Школа Samsung - финал 2026, номинация «Программирование»  
**Версия:** 1.4.0 (versionCode 11)  
**Package:** `ru.myessentiality.essential`

## Назначение проекта

Essential предназначен для пользователей, которым нужно хранить знания в виде markdown-заметок и быстро получать ответы ИИ на основе собственного архива - без передачи всего vault в облако. Приложение объединяет редактор заметок, поиск по vault, обратные ссылки между заметками и гибридный ИИ-стек (офлайн на устройстве или через REST API).

## Ключевые возможности и архитектура систем

### 1. Vault заметок (Markdown)

Хранилище работает через Storage Access Framework (внешняя папка) или внутреннее хранилище приложения. Поддерживаются файлы `*.md`.

Каждая заметка индексируется в Room: заголовок, путь, фрагмент текста, время изменения. В редакторе отображаются **обратные ссылки** - заметки, которые ссылаются на текущую.

### 2. RAG-чат по личным заметкам

Перед ответом ИИ выполняется поиск релевантных фрагментов в vault:

- **FTS5 / FTS4** - полнотекстовый поиск через виртуальную таблицу `note_fts` (с fallback FTS4 на устройствах без FTS5).
- **Семантический индекс (TF-IDF)** - `SemanticIndex` строит векторы по чанкам заметок и ранжирует cosine similarity.
- **VaultContextHelper** - собирает компактный контекст (до ~3500 символов) и список источников для UI.

Найденные фрагменты передаются в system prompt; пользователь видит карточки источников под ответом.

### 3. Гибридный ИИ (локальный + облачный)

`ChatEngine` маршрутизирует запросы:

| Режим | Когда используется |
|-------|-------------------|
| **Локальный** | Нет облачного режима, нет вложений — LiteRT-LM на устройстве |
| **Essential API** | Авторизация на `https://myessentiality.ru`, квоты Free/Pro |
| **OpenAI-compatible / Gemini** | Debug-сборка с ключами в `local.properties` |

Локальный режим:

- **LiteRT-LM** — модели `.litertlm`, GPU/CPU backend, streaming (`LiteRtLmRunner`, `HybridLocalLlmEngine`).
- **LocalWebResearch** — опциональный веб-контекст при наличии сети.

Облачный режим проверяет online-статус через `OnlineChecker` / `NetworkMonitor`.

### 4. Шифрованная синхронизация snapshot

`VaultSyncRepository` упаковывает vault в ZIP, шифрует **AES-256-GCM** с ключом из **PBKDF2** (120 000 итераций, SHA-256) и загружает на сервер. Конфликты: «побеждает последний snapshot».

### 5. Редактор, локализация, UX

- Jetpack Compose + Material 3, кастомная тема, анимации переходов.
- Markdown-рендер: код, формулы (JLatexMath), подсветка синтаксиса.
- Локализация **RU / EN / ES**.
- Голосовой ввод, вложения в чат, heatmap активности, экспорт переписки.

## Архитектура кодовой базы

Проект использует **Dagger Hilt** для DI.

```
app/src/main/java/com/rassvet/essential/
|
|- data/
|  |- api/           - EssentialApi, JWT, OpenAI/Gemini, streaming
|  |- chat/          - ChatEngine, экспорт, вложения, маршрутизация LLM
|  |- index/         - IndexRepository, SemanticIndex (TF-IDF)
|  |- llm/           - LiteRT-LM, каталог и загрузка моделей
|  |- local/         - Room (AppDatabase, DAO, FTS5/FTS4, миграции)
|  |- network/       - NetworkMonitor, OnlineChecker
|  |- notes/         - NotesRepository
|  |- security/      - PassphraseKeystore
|  |- sync/          - VaultCryptography, VaultSyncRepository
|  |- vault/         - VaultDocuments (SAF)
|
|- di/                - DatabaseModule, RepositoryModule (Hilt)
|- litert/            - LiteRtNativeBootstrap
|- locale/            - AppLocales, RelativeTime
|- service/           - LocalModelDownloadService
|
|- ui/
|  |- auth/           - вход, регистрация
|  |- chat/           - экран чата, карточки источников
|  |- editor/         - редактор markdown, обратные ссылки
|  |- home/           - список заметок, главный экран, heatmap
|  |- onboarding/     - первый запуск, создание vault
|  |- settings/       - настройки, квоты, сессии
|  |- splash/         - splash screen
|  |- theme/          - Material 3
|  |- markdown/       - парсер и блоки markdown
|
|- EssentialApplication.kt, MainActivity.kt, EssentialApp.kt
```

## Используемый стек технологий

| Категория | Технологии |
|-----------|------------|
| Язык | Kotlin (+ Java в data-слое) |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| СУБД | Room (SQLite), FTS5/FTS4 |
| DI | Dagger Hilt |
| Сеть | OkHttp, REST (`EssentialApi`) |
| ИИ (локально) | LiteRT-LM |
| ИИ (облако) | REST API Essential, OpenAI-compatible, Gemini |
| Настройки | DataStore Preferences |
| Шифрование | AES-256-GCM, PBKDF2 |
| Тесты | JUnit, Robolectric, Room Testing, Coroutines Test |

## Требования для сборки

- **JDK 17+**
- **Android SDK 36** (minSdk **24**, targetSdk **36**)

Опционально для debug с прямым облаком - ключи в `local.properties` (не коммитить):

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

APK также в [Releases](https://github.com/abdreevdanis/samsung/releases) - тег **v1.4.0**.

## Сервер и тестовый вход

Production API: **https://myessentiality.ru**

| | |
|---|---|
| Сервер | `https://myessentiality.ru` |
| Логин | *(demo-аккаунт для жюри - см. презентацию)* |
| Пароль | *(см. презентацию)* |

Debug-сборка без сервера: логин `admin`, пароль `admin`.

## Тестирование проекта

### Unit-тесты

```bash
./gradlew :app:testDebugUnitTest
```

### Instrumented-тесты

```bash
./gradlew :app:connectedAndroidTest
```

### Структура тестов

`app/src/test/java/com/rassvet/essential/` - 38+ unit-тестов:

| Файл | Назначение |
|------|------------|
| `data/sync/VaultCryptographyTest.kt` | AES-GCM, PBKDF2 |
| `data/index/SemanticIndexTest.kt` | TF-IDF индекс, ранжирование |
| `data/chat/VaultContextHelperTest.kt` | сбор RAG-контекста |
| `data/chat/ChatEngineRoutingTest.kt` | маршрутизация local / cloud |
| `data/chat/ChatAttachmentJsonTest.kt` | сериализация вложений |
| `data/api/JwtPayloadTest.kt` | разбор JWT |
| `data/api/ApiBaseUrlsTest.kt` | URL API |
| `data/llm/LlamaRuntimeTuningTest.kt` | параметры локального LLM runtime |
| `data/llm/LocalWebResearchTest.kt` | локальный веб-контекст |
| `ui/markdown/MarkdownParserTest.kt` | разбор markdown |
| `locale/RelativeTimeTest.kt` | относительное время |
| `data/chat/ChatDeviceClockTest.kt` | время устройства |
