# Ultimate Calculator (Android)

A native Android calculator built with Kotlin, Jetpack Compose and Material 3:
standard and scientific calculators, unit converter, financial tools, math tools,
history, memory and themes. Works fully offline; only the optional currency
converter uses the internet.

## Features

| Area | What works |
|---|---|
| Standard | + − × ÷, %, decimals, ±, brackets with smart open/close, AC, backspace, repeated equals, live result preview, thousands separators, precision control, long-press extras (÷→√, ×→xʸ, −→±, %→!, 0→00) |
| Scientific | sin cos tan and inverses (2nd), sinh cosh tanh, log, ln, logₓ, √, ∛, ⁿ√, x², x³, xʸ, x⁻¹, 10ˣ, eˣ, \|x\|, n!, mod, nPr, nCr, gcd, lcm, floor, ceil, round, π, e, EE, DEG/RAD indicator, landscape layout |
| Engine | Own tokenizer + recursive-descent parser (no eval), BigDecimal arithmetic (40 digits), operator precedence, unary minus, implicit multiplication, auto-closing brackets, domain checks, overflow/underflow, clear error messages |
| Memory | MC, MR, M+, M−, MS with up to 10 slots and a visible memory indicator |
| History | Saved automatically in Room, search, favorites, copy, share, reuse, delete one, clear all (or keep favorites), persists after restart |
| Converter | 19 categories: length, distance, weight, mass, temperature, area, volume, speed, time, data, transfer speed, pressure, energy, power, force, frequency, angle, fuel consumption, cooking. Instant conversion and an all-units list |
| Finance | Loan with amortization schedule, mortgage, savings, compound and simple interest, VAT, sales tax, discount, profit/loss, markup and margin, tip and split bill, salary, percentage of salary, investment return. All in BigDecimal |
| Currency | Live rates from ExchangeRate-API (open.er-api.com), cached with the last update time. No rates are ever invented |
| Math tools | Fractions and mixed numbers, fraction ↔ decimal, ratios and proportions, percent change/difference, prime check and factorization, GCD/LCM, statistics (mean, median, mode, SD, variance, sum), arithmetic and geometric progressions, linear equation, 2-variable system, quadratic (incl. complex roots), scientific notation. Steps shown where useful |
| Personalization | Light / dark / system, AMOLED black, Navy, Gold, Classic and custom accent colors, font and button size, decimals, separators, default DEG/RAD, sound and vibration, live preview toggle, reset |
| Android extras | Splash screen, adaptive + themed icon, app shortcuts (Scientific, Converter, Finance, History), Quick Settings tile, home-screen widget (last result + quick buttons), physical keyboard input, phone and tablet layouts (navigation rail ≥ 600 dp), state kept on rotation and restart |

### Not included, and why
- **Floating calculator window**: needs the "display over other apps" permission (`SYSTEM_ALERT_WINDOW`), which conflicts with the no-unnecessary-permissions requirement and gets extra Google Play review. The Quick Settings tile and widget give fast access instead.
- **A full calculator inside the widget**: home-screen widgets can't host a reliable interactive keypad; the widget shows the last result and opens the app in one tap.
- **Ads and in-app purchases**: see Monetization below. This build has no ads and everything unlocked.

## Build

Requirements: Android Studio (Narwhal or newer) or JDK 17 + Android SDK 36.

```bash
# debug APK
gradle assembleDebug            # → app/build/outputs/apk/debug/app-debug.apk
# unit tests (math engine, finance, converters, memory)
gradle testDebugUnitTest
# instrumented Room test (needs a device or emulator)
gradle connectedDebugAndroidTest
```

Open the folder in Android Studio to use its bundled Gradle; the project pins
AGP 8.11.1, Kotlin 2.0.21 and Gradle 8.14.3 (target API 36, as Google Play requires) (used by CI). GitHub Actions builds
every push, runs the unit tests and publishes the debug APK under **Releases**.

## Release (Google Play)

1. Create an upload key once:
   `keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. Create `keystore.properties` in the project root (never commit it):
   ```
   storeFile=../upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
   or set the environment variables `STOREFILE`, `STOREPASSWORD`, `KEYALIAS`, `KEYPASSWORD`.
3. Raise `versionCode` and `versionName` in `app/build.gradle.kts`.
4. `gradle bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (R8 shrinking is on).
5. Upload the AAB in Play Console with Play App Signing enabled.

### Store listing draft
- **Category**: Tools
- **Short description**: Accurate calculator with scientific, finance and unit tools. Offline and private.
- **Full description**: Ultimate Calculator gives you a fast everyday calculator, a full scientific
  mode, 19 unit converters, loan/mortgage/savings and business tools, fractions, statistics and
  equation solvers, all with precise decimal math. History, memory and beautiful themes (including
  AMOLED, navy and gold). No account, no tracking, works offline.
- **Privacy policy**: needed by Play for every app. Suggested text: the app stores calculations and
  settings only on the device, collects no personal data, and contacts open.er-api.com only when the
  user taps "Update rates" in the currency converter.
- **Assets to capture**: phone screenshots of Calculator, Scientific, Converter, Finance (loan with
  schedule), History, Settings; a 1024×500 feature graphic using the navy and gold icon colors.

## Monetization (optional, not enabled)

The code has no ads or billing so it always works fully. A suggested split if you add it later:
free = standard calculator, basic scientific, basic converters, history, basic themes;
premium = math tools, finance tools, advanced statistics, custom accent, favorites/search extras,
widget. If you add AdMob, place a banner only on the Converter/Finance list screens, never over
keys, never interstitials during calculation.

## Project structure

```
app/src/main/java/com/hmalmulla/calculator/
  engine/      MathEngine, Parser (tokenizer + parser), NumberFormatter, AdvancedMath, Finance, Units, Memory
  data/        Room history, DataStore settings
  ui/calculator  standard + scientific screens, keypad, ViewModel
  ui/converter   unit converter
  ui/tools       finance tools, math tools, currency, shared tool form
  ui/history, ui/settings, ui/theme
  system/      widget, Quick Settings tile
app/src/test/  JUnit tests for engine, finance, tools, units
app/src/androidTest/  Room persistence test
```
