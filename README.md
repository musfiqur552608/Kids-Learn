# Kids Learn

A fully offline learning app for young children: English, Bangla and Arabic
alphabets, numbers and shapes, plus four mini-games, with progress, stars,
badges and a maths-gated parent zone. Kotlin, Jetpack Compose, Room, Hilt.

The app never touches the network — there is no `INTERNET` permission in either
APK (verified with `aapt2 dump permissions`). Speech uses the device's offline
text-to-speech engine and falls back to bundled sound effects when a voice for
the language is missing.

## What's inside

| Module | Content |
| --- | --- |
| English | 26 letters, pronunciation, tracing, quiz |
| Bangla | 11 vowels + 35 consonants (46), same flow |
| Arabic | 28 letters, right-to-left, Naskh font |
| Maths | 20 counting exercises + 13 shapes, addition quiz |
| Games | Memory match, find the correct one, timed quiz |
| Progress | Stars, coins, streak, per-module bars, badge case |
| Parent zone | Settings, child name, reset, behind a maths gate |

## Build

Prerequisites: JDK 17+ (this machine uses the auto-provisioned JDK 25 under
`~/.gradle/jdks`), Android SDK with API 37.

```sh
export JAVA_HOME=$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2
export PATH=$JAVA_HOME/bin:$PATH

./gradlew :app:assembleDebug      # debug APK
./gradlew :app:testDebugUnitTest  # 97 unit tests
./gradlew :app:lintDebug          # lint, abortOnError
./gradlew :app:assembleRelease    # R8-minified APK (~2.8 MB)
```

Outputs land in `app/build/outputs/apk/{debug,release}/`.

### Toolchain quirks worth knowing

These are easy to rediscover the hard way, so they are recorded here:

* **AGP 9.4.1 ships built-in Kotlin.** The project deliberately does *not* apply
  `org.jetbrains.kotlin.android` — doing so registers a second `kotlin { }`
  extension and configuration fails.
* **`gradle.properties` sets `android.disallowKotlinSourceSets=false`.** KSP 2.x
  registers generated sources through `kotlin.sourceSets`, which built-in Kotlin
  forbids; that flag is the documented escape hatch.
* **Robolectric needs ASM 9.9.** Robolectric 4.14 pulls ASM 9.7.1, which rejects
  class file major version 69 (JDK 25), so unit tests died with
  `Unsupported class file major version 69`. `app/build.gradle.kts` lifts ASM to
  9.9 on the unit-test classpaths only.
* **KSP must match Kotlin exactly** (`2.2.10-2.0.2` for Kotlin `2.2.10`).

## Architecture

MVVM + clean architecture, one-way data flow, Hilt for injection.

```
domain/       pure Kotlin: models, repository interfaces, use cases
data/         Room + DataStore + content catalogs, repository implementations
di/           Hilt modules
core/audio/   speech + sound-effect playback behind one interface
ui/           Compose screens, one ViewModel per screen, theme, components
```

| Package | Responsibility |
| --- | --- |
| `domain/model` | `LessonItem`, `QuizQuestion`, `Progress`, `Settings` |
| `domain/usecase` | star/streak/badge/recording rules, parent-gate generator |
| `domain/repository` | interfaces only — no Android imports |
| `data/content` | `EnglishCatalog`, `BanglaCatalog`, `ArabicCatalog`, `MathsCatalog`, `QuizFactory` |
| `data/local` | Room entities, DAOs, `KidsDatabase` (v1, schema exported) |
| `data/prefs` | Preferences DataStore for settings |
| `ui/navigation` | `Route` + `KidsNavHost` |
| `ui/<feature>` | screen + its ViewModel + immutable `UiState` |

The content catalogs are **module-scoped on purpose**: a maths quiz draws only
from counting/shapes, an alphabet quiz only from that module's letters. Content
is never pooled across modules.

## How progress works

* **Stars** — `CalculateStarsUseCase`: 3 stars requires a perfect score on at
  least `MIN_QUESTIONS_FOR_STARS` (3) questions; ≥ 0.66 accuracy → 2; any correct
  answer → 1. Credit starts at a single correct answer, never at zero.
* **Coins** — 2 per correct answer, 5 per star, capped per quiz.
* **Streak** — `CalculateStreakUseCase` with a 2-day grace period, and it is
  idempotent per calendar day (calling it twice on the same day never advances
  it).
* **Badges** — one evaluator, `EvaluateBadgesUseCase`, over `BadgeKey`.
* **Parent gate** — `GenerateParentGateUseCase` produces a simple sum. It is a
  speed bump, not security, and the code says so.

### Design rules that are deliberate

* **Never advance on a wrong answer.** No red, no buzzer, no failure screen in
  the child's path. `TRY_AGAIN` is a soft amber blip and the same question
  stays on screen.
* **Portrait only**, landscape locked.
* **RTL only inside the Arabic subtree** (`ProvideRtl`), so Bangla and English
  stay left-to-right.

## Testing — 97 unit tests

```sh
./gradlew :app:testDebugUnitTest
```

| Suite | Protects |
| --- | --- |
| `CalculateStarsUseCaseTest`, `CalculateStreakUseCaseTest` | scoring and streak rules |
| `EvaluateBadgesUseCaseTest`, `RecordQuizResultUseCaseTest` | badge unlock + persistence |
| `GenerateParentGateUseCaseTest` | the maths gate |
| `QuizFactoryTest`, `ContentCatalogTest` | quiz invariants, catalog sizes/uniqueness |
| `LetterTracingCanvasTest` | trace-path geometry |
| `StringResourcesTest` | **every** string and plural, formatted, in both locales |
| `ScreenRenderingTest` | **every** screen, rendered with representative state |

Two bug classes shaped the last two suites, and both shipped to a real device
before a test existed:

1. An argument passed twice to `pluralStringResource` — its second parameter is
   the plural *quantity*, not a format argument. Result:
   `IllegalFormatConversionException: d != java.lang.String`, crashing Home.
2. A `<plurals>` id passed to `stringResource`, which resolves plain strings only.
   Result: `Resources$NotFoundException: ... is a complex map type`, crashing
   the first module a child opened.

Kotlin type-checks both, lint did not flag either, and no domain test ever
executes a composable. `ScreenRenderingTest` renders every screen so the next
one is a build failure instead of a crash on a child's phone; `StringResourcesTest`
derives each argument list from the string's own conversion specifiers, so a
translation that swaps `%1$s` for `%1$d` fails in CI.

`androidTest/` also contains Room and navigation tests plus Hilt test
infrastructure; they need a device or emulator.

## Localisation and accessibility

* `values/strings.xml` (English) and `values-bn/strings.xml` (Bangla) — the UI
  chrome only; lesson content stays in its own script inside the catalogs, so
  pronunciation is correct.
* Fonts bundled under `res/font`: `noto_sans_bengali.ttf`,
  `noto_naskh_arabic.ttf` (OFL, licence in
  `app/src/main/assets/licenses/`).
* TalkBack strings (`cd_*`) describe purely visual elements such as the mascot
  and the coin counter.

## Regenerating bundled assets

`tools/` holds the generators so binary assets are reproducible rather than
opaque:

```sh
python3 tools/generate_sfx.py     # 8 WAVs -> res/raw (tap, correct, star, ...)
python3 tools/generate_lottie.py  # confetti.json -> res/raw
python3 tools/generate_narration.py          # 243 MP3s + manifest -> assets/narration
python3 tools/generate_narration.py --check  # offline verify (for CI)
```

### Bundled narration (built-in voice)

Every Listen line plays a pre-generated clip from `assets/narration/`
(`BundledSpeechPlayer`), so Bangla/Arabic speak correctly on any device with
no download, no permission and no network. The clips are the *installed* app:
nothing is fetched at runtime and the APK stays fully offline (still no
`INTERNET` permission) — only the generator script itself needs network, on
the developer's machine.

* Coverage: 26 English + 46 Bangla + 28 Arabic lesson lines and their
  word-only quiz prompts, 20 numbers, 13 shapes, "How many?", 10 mascot
  encouragements. Dynamic English-only prompts ("3 plus 2", badge names) use
  system TTS, which is always present for en-US.
* After any catalog or encouragement-copy edit, re-run the generator and commit
  the changed clips: `BundledNarrationTest` (unit) and `--check` fail until
  the manifest matches the code again.
* To replace the synthetic voices with studio recordings, drop same-named files
  into `assets/narration/` — no code changes needed.

## Alternate app names

The current name is **Kids Learn** (`app_name` in `values/strings.xml`, also the
launcher label). If you want alternatives:

1. **Little Learners** — age-neutral, reads well to a parent browsing a store.
2. **Alphabet Adventure** — leads with the core content (letters), good if the
   maths/games stay secondary.
3. **Shikkha Kids** — Bengali-forward, matches the Bangla/Arabic emphasis.

Renaming is one string in `values/strings.xml`; changing the application id
(`com.freedu.kidslearn`) means a new package name across the source tree and is
a separate, larger change.
