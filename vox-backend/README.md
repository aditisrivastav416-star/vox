# Vox demo API

The Android app stores profile, transactions, budgets, recurring rules, and groups on the device. This NestJS service forwards short recordings to Gnani Prisma STT and spending answers to Gnani Timbre TTS; the Gnani key never goes into the APK. Generate the key with both Speech to Text and Text to Speech scopes.

## Run locally

Copy `.env.example` to `.env`, replace `GNANI_API_KEY` with a key generated in the [Gnani console](https://app.gnani.ai), then run `npm install` and `npm run start:dev` from this folder. Android defaults to the deployed demo API. For a local backend in the emulator, build with `VOX_API_BASE_URL=http://10.0.2.2:3000/api/v1`.

`GET /` identifies this API; it does not serve the Android app as a website. `GET /api/v1/health` returns `{ "ok": true }`. `POST /api/v1/voice/transcribe` accepts JSON `{ "audioBase64": "...", "languageCode": "en-IN" }` and returns `{ "transcript": "..." }`. Audio is M4A, recorded for five seconds by the app, and forwarded to Gnani's `/stt/v3` endpoint. `POST /api/v1/voice/speak` accepts `{ "text": "..." }` and returns base64 MP3 audio from Timbre. The audio request body is limited to roughly 6 MB of decoded audio. These endpoints have no user authentication and should only be exposed for the limited challenge demo; add abuse controls before sharing a public APK widely.

## Vercel

Create a Vercel project with **Root Directory `vox-backend`**. NestJS `src/main.ts` is a supported Vercel entrypoint, so no custom `vercel.json` is needed. Add `GNANI_API_KEY` in Vercel Project Settings → Environment Variables. Deploy, test `https://YOUR-PROJECT.vercel.app/api/v1/health`, then build Android with `VOX_API_BASE_URL=https://YOUR-PROJECT.vercel.app/api/v1`. Never commit the real key or place it in Android Gradle configuration.

The previous PostgreSQL/Prisma API files remain in the repository, but the active module does not load them. Database migrations, workers, OTP, and email/SMS credentials are not used by this demo.
