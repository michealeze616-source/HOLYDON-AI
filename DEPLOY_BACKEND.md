# HOLYDON AI backend deployment

The Android app is built separately from the cloud AI service. Deploy this backend once, then enter its URL inside the app under **Settings**.

## Deploy on Render

1. Open Render and create a new **Blueprint** using this GitHub repository, or create a Web Service manually.
2. For a manual service, set **Root Directory** to `backend`, **Runtime** to Docker, and select the Free plan if available.
3. Add `GROQ_API_KEY` as a private environment variable in Render. Do not put provider keys in Android source or GitHub.
4. Deploy and open `https://YOUR-SERVICE.onrender.com/health`. Confirm the response includes `"status":"ok"` and `"groq_configured":true`.
5. Install the Android APK, open **Settings**, enter the service URL (without an endpoint path), tap **Save Connection**, then **Test Backend Connection**.

## What works when the backend is configured

- Chat and code assistance through Groq, with the recent conversation turns sent as context.
- PDF text extraction and question answering for selectable-text PDFs up to 12 MB and 80 pages.
- Image generation through the optional image provider configured below.

Scanned/image-only PDFs do not have OCR yet. PDF processing uses extracted text, so very large documents are truncated to a bounded context.

## Optional image generation

Groq is a text model and does not generate images. To enable Image Studio, configure these private backend environment variables in Render:

- `HF_IMAGE_API_URL`: the inference endpoint for a compatible text-to-image model.
- `HF_TOKEN`: the provider access token.

The configured endpoint must accept a JSON body with an `inputs` prompt and return image bytes. Provider availability, pricing and free quotas are controlled by that provider. The app cannot guarantee unlimited free image generation. Upload-based image editing is not implemented yet.

## Key safety

Never place API keys in Android source, APK assets, Git commits, screenshots, or public issue reports. If a key is exposed, revoke it at the provider and create a replacement.
