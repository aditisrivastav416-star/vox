# Vox — Solution Pitch

**Voice-first personal finance for India, in your language. Built with Gnani AI.**

## The problem

Most people stop tracking their money within weeks. Typing in every chai, auto ride and grocery bill is tedious, and most finance apps assume you are comfortable in English. For hundreds of millions of Indians who think and speak in Hindi or Hinglish, budgeting apps feel like paperwork, so spending stays invisible until the month runs out.

## Our solution

Vox removes the typing. You **say** an expense, **scan** a bill, or **ask** a question, in English, Hindi or a mix of both, and Vox does the bookkeeping.

- **Log by voice from the home screen.** Tap the Vox widget and say *"Spent 400 rupees on groceries"* or *"किराने पर 300 रुपये खर्च किए"*. Vox extracts the amount, the type and the category. You confirm, and it's saved without opening the app.
- **Ask about your money.** *"Can you summarize my spending?"*, *"How much did I spend on food this month?"*, *"किराने पर कितना खर्च किया?"* Vox answers out loud, in the language you asked in.
- **Budgets by voice.** *"इस महीने का बजट 5000 रख दो"* sets this month's budget. *"How much budget do I have left?"* tells you where you stand.
- **Scan receipts.** On-device OCR reads the items, tax and total from Latin and Devanagari receipts. You review them before saving.
- **Split with friends.** Create a trip or home group from your contacts (friends don't need Vox), then split a bill equally or item by item from a scanned receipt.
- **Recurring payments.** Add rent, bills and subscriptions once, with their due date and reminder preferences.

## How Gnani AI powers it

| Gnani API | What it does in Vox |
|---|---|
| **Speech-to-Text (Vachana STT v3)** | Transcribes short voice clips. Vox sends each clip in English and Hindi and keeps the transcript that best matches a finance command, so users never pick a language. |
| **Text-to-Speech (Timbre v2.5)** | Speaks spending answers and budget confirmations back in natural English or Hindi. |

## Architecture & privacy

- **Android app** (Kotlin, Jetpack Compose): all finance data (transactions, budgets, groups, receipts) stays **on the device**. OCR runs on the device too (ML Kit).
- **Thin NestJS proxy on Vercel**: forwards audio to Gnani STT and text to Gnani TTS. The Gnani API key lives only on the server and never ships inside the APK. Only the short voice clip leaves the phone.

## Who it's for

Students, young professionals and families who share expenses, especially Hindi and Hinglish speakers who want to understand their money without spreadsheets.

## What's next

Multi-turn voice conversations, broader Indian-language support (the STT layer already accepts 10 Indian languages), reminder notifications, cloud backup, and syncing shared groups across devices.
