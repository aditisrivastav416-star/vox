# Vox — Gnani internship challenge checklist

## Working demo

- [x] Switch personal finance records to device storage: profile, transactions, budgets, recurring rules, and groups.
- [x] Remove database and OTP dependencies from the active backend module.
- [x] Add a Vercel compatible NestJS service that forwards audio to Gnani Prisma STT.
- [x] Record a short voice command on Android and show a confirmation before saving a transaction.
- [x] Answer this/last month spending questions from device data and speak the response with Gnani Timbre TTS.
- [ ] Expand the voice dialogue beyond monthly spending and one-shot transaction commands; follow-up context is not implemented yet.
- [ ] Create a Gnani API key with **both Speech to Text and Text to Speech scopes**, then test English, Hindi, and Hinglish recordings and replies. The key is not available in this repository.
- [ ] Improve language selection and transaction parsing for Hindi and code mixed speech; current parser recognizes numeric amounts and simple English categories only.
- [ ] Add device backup/export and restore if the demo data needs to survive uninstall or phone loss.
- [ ] Complete receipt capture/OCR; the current receipt repository is still a placeholder.
- [ ] Verify manual entry, Activity, Home summaries, budgets, recurring, groups, and splits on a physical device.
- [ ] Check microphone permission denial, no connection, Gnani credit exhaustion, and API errors in the app.
- [ ] Add abuse controls before distributing the APK or Vercel endpoint publicly. The demo proxy is unauthenticated.
- [ ] Decide how to handle shared groups across devices. Device-only groups currently work locally and cannot sync with another user's phone.

## Deploy and submit

- [ ] Confirm that the separate internship registration was submitted. The screenshot confirms an active API account and ₹5,000 credits, but does not by itself prove challenge registration.
- [ ] Sign in at [app.gnani.ai](https://app.gnani.ai), generate an API key, and make one test request in the playground. The email does not contain the key.
- [ ] Put the key in `vox-backend/.env` for local testing and in Vercel Environment Variables for deployment. Do not put it in Android or git.
- [ ] Deploy Vercel with `vox-backend` as Root Directory; check `/api/v1/health` and test transcription.
- [ ] Set `VOX_API_BASE_URL` to the deployed HTTPS `/api/v1` URL when building the APK; test the installed APK with mobile data.
- [ ] Write the project problem, target users, Gnani API used, and each team member's contribution in the participant workspace.
- [ ] Record a truthful working demo/video showing a Gnani powered voice command and the saved local transaction.
- [ ] Post the video publicly on LinkedIn and/or X, tag Gnani AI, and include `#GnaniAI` and `#GreatIndianAIInternshipChallenge`.
- [ ] Submit the demo/video, written explanation, API/model details, and social post links through the programme page by **11:59 PM IST, November 10, 2026**.

Source: [Gnani challenge participation terms](https://www.gnani.ai/great-indian-ai-internship-registration-form).
