# Backend deployment (free tier)

1. Create a new Web Service on Render and connect this repository.
2. Set **Root Directory** to `backend`, **Runtime** to Docker, and choose the Free plan if available.
3. Add the environment variable `GROQ_API_KEY` in the Render dashboard. Paste your Groq key there, not in GitHub files.
4. Deploy. Open `https://YOUR-SERVICE.onrender.com/health` and confirm `status: ok` and `groq_configured: true`.
5. In `app/src/main/java/com/holydon/ai/MainActivity.java`, replace `https://YOUR-HOLYDON-BACKEND.onrender.com` with your actual Render URL, commit, and let GitHub Actions rebuild the APK.

Groq powers chat and code help. It does not generate or edit images. Image Studio needs a separate image model/provider. To configure one, set `HF_IMAGE_API_URL` and `HF_TOKEN` as private backend environment variables. Provider availability, pricing and free quotas are controlled by that provider; the app cannot guarantee unlimited free cloud generations.

## Key handling
Never put provider keys in Android source, APK assets, Git commits, screenshots, or public issue reports. If a key was posted into a chat or public place, revoke it at the provider and create a replacement before deployment.
