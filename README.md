# Essential

Android-приложение для личных markdown-заметок и ИИ-чата с контекстом из ваших материалов.

**Автор:** Абдреев Данис Ринатович  
**Версия:** 1.4.0 (versionCode 11)  
**Package:** `ru.myessentiality.essential`

## Назначение проекта

Essential помогает хранить знания в markdown-заметках на устройстве и задавать вопросы ИИ-ассистенту с опорой на содержимое vault. Заметки остаются локально; в облако уходят только запросы к API (при включённом облачном режиме).

## Ключевые возможности

### 1. Vault заметок (Markdown)

- Хранилище через SAF (папка на устройстве) или внутреннее хранилище приложения.
- Файлы `*.md`, список заметок, редактор, предпросмотр markdown.
- Индекс в Room: заголовок, путь, фрагмент текста, время изменения.
- **Обратные ссылки** в редакторе — заметки, которые ссылаются на текущую.

### 2. Поиск и RAG-контекст для чата

Перед ответом ИИ приложение ищет фрагменты в vault:

- **FTS5 / FTS4** — полнотекстовый поиск (`note_fts`, с fallback FTS4, если FTS5 недоступен).
- **Семантический индекс (TF-IDF)** — ранжирование чанков заметок по cosine similarity.
- Релевантные фрагменты попадают в prompt; под ответом показываются **карточки источников**.

Поиск по vault можно включать и отключать в настройках.

### 3. ИИ-чат

**Локальный режим (LiteRT-LM)**

- Модели формата `.litertlm`, загрузка из каталога в приложении.
- Streaming-ответы, выбор CPU/GPU backend.
- Без интернета работает офлайн (после загрузки модели).

**Облачный режим (Essential API)**

- Регистрация, вход, сброс пароля через `https://myessentiality.ru`.
- Запросы к `/api/ai/complete`, учёт дневной квоты токенов (Free / Pro).
- Вложения в чат (изображения, файлы) обрабатываются через API.
- При отсутствии сети показывается сообщение об offline.

**Debug-сборка:** логин `admin` / `admin` без сервера (заглушка для разработки).

### 4. Дополнительно

- История чатов (несколько сессий, сохранение в Room).
- Контекст из выбранной заметки или сохранение ответа чата как новой заметки.
- Голосовой ввод в поле сообщения.
- Heatmap активности (статистика использования с сервера после входа).
- Локализация **RU / EN / ES**.
- Markdown: код, подсветка синтаксиса, формулы (JLatexMath).
- Опциональный **LocalWebResearch** — краткий веб-контекст для локального чата (настройки).

## Архитектура кодовой базы

DI: **Dagger Hilt**.

```
app/src/main/java/com/rassvet/essential/
|
|- data/
|  |- api/        - REST-клиент Essential API
|  |- chat/       - ChatEngine, вложения, история
|  |- index/      - IndexRepository, SemanticIndex
|  |- llm/        - LiteRT-LM, загрузка моделей
|  |- local/      - Room, DataStore, FTS
|  |- network/    - проверка online/offline
|  |- notes/      - работа со списком заметок
|  |- vault/      - чтение/запись markdown (SAF)
|
|- di/
|- litert/        - bootstrap LiteRT native
|- locale/
|- service/       - фоновая загрузка моделей
|
|- ui/
|  |- auth/       - вход, регистрация
|  |- chat/       - чат, источники
|  |- editor/     - редактор, чат по заметке
|  |- home/       - главный экран, список заметок
|  |- onboarding/
|  |- settings/   - аккаунт, квоты, модели
|  |- splash/
|  |- theme/
|  |- markdown/
```

## Стек технологий

| Категория | Технологии |
|-----------|------------|
| Язык | Kotlin (+ Java в data-слое) |
| UI | Jetpack Compose, Material 3 |
| БД | Room (SQLite), FTS5/FTS4 |
| DI | Dagger Hilt |
| Сеть | OkHttp |
| Локальный ИИ | LiteRT-LM (`.litertlm`) |
| Облачный ИИ | REST API Essential |
| Настройки | DataStore |

## Требования для сборки

- JDK 17+
- Android SDK 36 (minSdk 24)

## Сборка и установка

```bash
git clone https://github.com/abdreevdanis/samsungrepo.git
cd samsung
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Windows: `gradlew.bat :app:assembleDebug`

APK приложения: [Releases](https://github.com/abdreevdanis/samsungrepo/releases/tag/v1.4.0) — **v1.4.0**.

## Сервер и тестовый вход

API: **https://myessentiality.ru**

| | |
|---|---|
| Сервер | `https://myessentiality.ru` |

Debug APK без сервера: `admin` / `admin`.

## Тестирование

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedAndroidTest
```

Unit-тесты (`app/src/test/...`), 38+:

| Файл | Что проверяет |
|------|----------------|
| `SemanticIndexTest` | TF-IDF поиск |
| `VaultContextHelperTest` | сбор контекста для чата |
| `ChatEngineRoutingTest` | local / cloud маршрутизация |
| `ChatAttachmentJsonTest` | вложения |
| `JwtPayloadTest` | JWT |
| `ApiBaseUrlsTest` | URL API |
| `LlamaRuntimeTuningTest` | параметры локального LLM |
| `LocalWebResearchTest` | веб-контекст |
| `MarkdownParserTest` | markdown |
| `RelativeTimeTest` | локализация времени |
| `ChatDeviceClockTest` | время на устройстве |
