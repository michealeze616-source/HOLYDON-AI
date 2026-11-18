# HOLYDON AI

A native Java Android AI assistant by HOLYDON.

## Project layout
- `android/` — native Android app (Java, minSdk 23)
- `backend/` — Flask API for Groq chat and optional image provider integration
- `.github/workflows/android.yml` — builds a debug APK on GitHub Actions

## Security
Set `GROQ_API_KEY` only as a private environment variable on your backend host. Never commit API keys or put them in the Android app. Groq powers text chat and code assistance; image generation requires a separate image provider and its own credentials or a compatible free service.

## Build
Push to `main` and download the `HOLYDON-AI-debug-apk` artifact from the Actions run.
