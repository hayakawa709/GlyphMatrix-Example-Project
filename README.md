# Kemono Friends Glyph Toy

A Nothing Phone (3) Glyph Toy that displays the Kemono Friends logo on the 25x25 Glyph Matrix.

## What this project does

- Registers a Glyph Toy named `Kemono Friends Logo`.
- Displays `app/src/main/res/drawable/kemono_friends_logo.png` on the Glyph Matrix.
- Renders the static logo once when the Glyph Matrix service connects.
- Long-pressing toggles between the logo and a test speedometer mode; releasing the button leaves
  the selected mode active. The test speed accelerates from 0 to 180 km/h over 10 seconds and
  repeats.
- Renders the logo again for AOD and touch press events.

## Notes

This project is based on the Glyph Matrix sample project. It no longer sends the same frame repeatedly in a timer loop. In speedometer mode it updates the simulated speed every 100 ms.

The needle uses two scales: up to 80 km/h, its eight intervals are 10 km/h each. Above 100 km/h, it switches to a 20–180 km/h scale, with 100 km/h at the center and 20 km/h per interval.

## Build

Open this directory in Android Studio, or build from PowerShell with Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```
