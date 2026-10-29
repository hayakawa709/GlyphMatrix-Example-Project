# Kemono Friends Glyph Toy

A Nothing Phone (3) Glyph Toy that displays the Kemono Friends logo on the 25x25 Glyph Matrix.

## What this project does

- Registers a Glyph Toy named `Kemono Friends Logo`.
- Displays `app/src/main/res/drawable/kemono_friends_logo.png` on the Glyph Matrix.
- Renders the static logo once when the Glyph Matrix service connects.
- Renders the logo again for AOD and touch press events.

## Notes

This project is based on the Glyph Matrix sample project, but the Toy service has been simplified for a static image. It no longer sends the same frame repeatedly in a timer loop, which avoids unnecessary work and reduces instability when switching Toys.

## Build

Open this directory in Android Studio, or build from PowerShell with Android Studio's bundled JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```
