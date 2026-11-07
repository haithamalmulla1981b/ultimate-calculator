# Publishing Ultimate Calculator on Google Play

Everything below is ready to copy into Play Console (play.google.com/console).

## 1. Files

| What | Where |
|---|---|
| Signed release bundle | `ultimate-calculator-1.0.0-signed.aab` (from the publishing kit) |
| Upload key | `ultimate-calculator-upload.jks` + `UPLOAD_KEY_KEEP_SAFE.txt`. Keep private and backed up |
| App icon 512 × 512 | `icon-512.png` |
| Feature graphic 1024 × 500 | `feature-graphic-1024x500.png` |
| Privacy policy URL | https://github.com/haithamalmulla1981b/ultimate-calculator/blob/main/PRIVACY.md |

App details: package `com.hmalmulla.calculator` · version 1.0.0 (code 1) · targets Android 16 (API 36) · min Android 8.0.

## 2. Create the app
Play Console → **Create app** → App name **Ultimate Calculator** · Default language **English (United States)** ·
**App** · **Free** · accept the declarations.

## 3. Store listing (Grow → Store presence → Main store listing)

**App name** (max 30): `Ultimate Calculator`

**Short description** (max 80):
`Precise calculator with scientific, finance and unit tools. Offline and private.`

**Full description** (max 4000):
```
Ultimate Calculator is a fast, accurate calculator for everyday sums, school, work and money decisions. Everything works offline, with no account and no tracking.

EVERYDAY CALCULATOR
• Large, clear display with the result shown as you type
• Correct order of operations, brackets, percentages and negative numbers
• Repeat the last operation by tapping = again
• Memory keys (MC, MR, M+, M−, MS) with multiple memory slots
• Copy or share any result with one tap

SCIENTIFIC MODE
• sin, cos, tan and their inverses, hyperbolic functions
• log, ln, log with any base, roots, powers, factorial
• nPr, nCr, GCD, LCM, modulo, rounding, π and e
• Degrees and radians, landscape layout

PRECISE MATH ENGINE
Built on exact decimal arithmetic, so 0.1 + 0.2 = 0.3 and √2² = 2. Clear messages explain problems such as dividing by zero, instead of crashing.

FINANCE TOOLS
• Loan calculator with full amortization schedule
• Mortgage, savings, compound and simple interest
• VAT and sales tax, discounts, profit and loss, markup and margin
• Tip and bill splitting, salary and investment return
• Currency converter with live rates (internet needed only to update rates)

UNIT CONVERTER
19 categories including length, weight, temperature, area, volume, speed, time, data, transfer speed, pressure, energy, power, force, frequency, angle, fuel consumption and cooking measures.

MATH TOOLS
Fractions and mixed numbers, decimal ↔ fraction, ratios, percentage change, prime numbers and factorization, statistics (mean, median, mode, standard deviation, variance), progressions, and equation solvers (linear, two-variable and quadratic) with step-by-step working.

HISTORY AND PERSONALIZATION
• Searchable history with favorites, kept on your device
• Light, dark and AMOLED black themes; navy, gold and custom accent colors
• Adjustable text and button size, decimal places and separators
• Vibration and sound options

FAST ACCESS
Home-screen widget, Quick Settings tile and app shortcuts.

PRIVATE BY DESIGN
No ads, no sign-in, no data collection. Your calculations never leave your phone.

Developed by H.M.Al-Mulla
```

**Graphics**
- App icon: `icon-512.png`
- Feature graphic: `feature-graphic-1024x500.png`
- Phone screenshots (2–8 needed, 9:16 portrait): take them on your phone from the installed app. Suggested set:
  1. Calculator with a result (e.g. `1250×12%`)
  2. Scientific mode with `sin(30)+log(100)`
  3. Unit converter (Temperature or Length)
  4. Finance → Loan with the schedule visible
  5. History with a few entries
  6. Settings showing themes (dark/gold looks best)

**Category**: Tools · **Tags**: Calculator, Productivity tools · **Contact email**: your developer account email (required).

## 4. App content (Policy → App content)

| Section | Answer |
|---|---|
| Privacy policy | the PRIVACY.md URL above |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** |
| Content rating | Start questionnaire → category **Utility, productivity, communication, or other** → answer **No** to every content question → expected rating: Everyone / PEGI 3 |
| Target audience | **13–15, 16–17, 18 and over** (choosing under-13 brings the extra Families policy; not needed for a calculator) |
| News app | No |
| Data safety | **Does your app collect or share any of the required user data types? → No.** Is data encrypted in transit? → Yes (HTTPS). Account creation → not applicable. Result: "No data collected, no data shared" |
| Government app | No |
| Financial features | The app only calculates; it doesn't offer loans, banking, payments or trading. Choose **"My app doesn't provide any financial features"** |
| Health | No health features |

## 5. Release
Testing → **Internal testing** first is recommended (add yourself as tester, check the app installs from Play),
then **Production → Create new release**:
- Play App Signing: **use Google-generated app signing key** (default), then upload the signed AAB.
- Release name: `1.0.0`
- Release notes:
  ```
  <en-US>
  First release: everyday and scientific calculator, finance tools, 19 unit converters, math tools, history, memory and themes.
  </en-US>
  ```
- Countries: choose all or your list → **Review release → Start rollout**.

Note for new personal developer accounts: Google requires a closed test with at least 12 testers for
14 days before production access. If your account was created after November 2023 and hasn't done
this, run the closed test first.

## 6. Future updates
1. Raise `versionCode` (2, 3, …) and `versionName` in `app/build.gradle.kts`.
2. Push to GitHub; download `app-release.aab` from the new release.
3. Sign it with the same upload key, e.g.
   `zip -d app-release.aab 'META-INF/*'` then
   `jarsigner -keystore ultimate-calculator-upload.jks app-release.aab upload`
   (or add the key as GitHub secrets so CI signs automatically).
