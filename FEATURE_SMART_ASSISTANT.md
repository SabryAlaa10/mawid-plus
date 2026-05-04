# Mawid+ Smart Assistant — Design & Product Spec (English)

> Reference document for the **AI medical assistant** feature in the **patient Android app**.  
> Status: **design / MVP** — update as implementation progresses.

Use this with **Google Stitch** (or any UI tool) to design screens **before** engineering. Primary UI language for MVP: **Arabic (RTL)**; this spec is in English for the team and tooling.

---

## 1. Product vision

Let the patient **describe symptoms in natural language (Arabic)** inside an **in-app chat**, then:

1. Perform a **safe analysis** that does **not** deliver a definitive disease diagnosis — only suggests an **appropriate medical specialty for triage**.
2. Load the **best matching doctors** from the **Mawid+ database (Supabase)** for that specialty, **sorted by rating** (cap the list, e.g. top **5**).
3. Show a **short list**, let the patient **pick a doctor**, then **available times**, and complete **booking** using the **same business rules** as the existing booking flow.
4. Show a **clear confirmation** after a successful booking.

---

## 2. Out of scope (MVP)

| Out of scope | Notes |
|--------------|--------|
| Definitive diagnosis or naming a disease as a final verdict | Forbidden in prompts and UI copy |
| Replacing a real doctor’s opinion | Mandatory **disclaimer** copy |
| Doctor web dashboard | Phase 1: **patient Android app only** |
| Second UI language | **Arabic only** for MVP |

---

## 3. Functional requirements (MVP)

### 3.1 Chat experience

- Patient text input + (optional, later) **quick-reply chips** for common intents.
- Assistant replies in **Arabic**, with **short follow-up questions** when needed (duration, severity, associated symptoms) — avoid overly long threads.
- If **possible emergency red flags** are detected: show a **fixed emergency banner** (call emergency services / go to ER) — **do not** rely only on booking; optionally **block** auto-booking for that session.

### 3.2 AI outputs (structured)

Prefer **structured JSON** from the model (or parse text → JSON in app layer), including at minimum:

| Field | Type | Purpose |
|--------|------|--------|
| `suggested_specialty_code` | string | Internal id from a **closed list** — not free-form specialty text from the model alone |
| `needs_more_info` | boolean | Whether to ask more questions before listing doctors |
| `follow_up_questions` | string[] (optional) | Short questions to show in chat |
| `safety_escalation` | boolean | If `true` → show **emergency path** and **stop** automated booking |

### 3.3 Specialty ↔ database

- In-app **mapping**: `specialty_code` → Arabic synonyms / patterns to match `doctors.specialty` (current free text in DB).
- Query: filter + `ORDER BY rating DESC` (or equivalent) + `LIMIT`.

### 3.4 Booking

- After doctor + slot selection: call existing booking logic (`patient_id`, `doctor_id`, `appointment_date`, `queue_number`, `time_slot`, etc.).
- Require **authenticated patient**; if not signed in → redirect to **login**.
- On success: confirmation copy (e.g. “Your appointment has been booked”) + optional navigation to **My appointments**.

---

## 4. Non-functional requirements (MVP)

| Area | MVP choice |
|------|------------|
| Language | Arabic-only UI |
| Privacy | Disclose that text may be sent to a **third-party AI provider** when using OpenRouter; cap message length / session size |
| Logging | **Session memory only** for MVP; optional Supabase persistence later with retention policy + RLS |
| Performance | Timeouts, retries, Arabic error messages |

---

## 5. Technology stack (implementation preview)

| Layer | Technology |
|-------|------------|
| Patient app | Kotlin, Jetpack Compose, Navigation, ViewModel, StateFlow |
| Backend / data | Supabase (existing): `doctors`, `appointments`, `profiles`, … |
| AI (experimental) | **OpenRouter API** (HTTP) — pick a model with good Arabic + instruction following |
| HTTP client | Ktor (already in project) or equivalent; **avoid** shipping production API keys in the APK without protection (e.g. Edge Function later) |
| Local chat state | Datastore / Room / in-memory list — team choice |

---

## 6. User flow (for Stitch screen list)

```
[Describe symptoms]
        ↓
[Safe triage → suggested specialty OR follow-up questions]
        ↓
[Doctor list from DB, sorted by rating]
        ↓
[Select doctor]
        ↓
[Available time slots — same rules as current booking]
        ↓
[Select slot]
        ↓
[Confirm → create appointment]
        ↓
[Success + reminders if applicable]
```

### Suggested screens / states for design

1. **Entry** — FAB or home card: “Smart assistant” / «مساعد موعد+».
2. **Chat (main)** — RTL, bubbles (user / assistant), input bar, optional disclaimer strip at top.
3. **Follow-up questions** — same chat surface; assistant bubbles only.
4. **Emergency state** — full-width banner + primary action (call / emergency) + secondary (dismiss / back).
5. **Doctor results** — horizontal cards or vertical list: photo, name, specialty, rating, CTA “Choose” / «اختر».
6. **Slot selection** — date strip + time grid or list (align with existing booking visual language).
7. **Confirm sheet** — doctor, date, time, fee if shown.
8. **Success** — checkmark, short copy, button “View my appointments”.
9. **Login gate** — modal or full-screen if session missing.
10. **Error / offline** — Arabic message + retry.

---

## 7. Implementation phases (engineering)

1. Chat shell + prompts + **safe JSON** (no booking yet).
2. **Doctor query** by `specialty_code`.
3. **Slot UI** for selected doctor.
4. **Booking** via existing repositories.
5. Polish: disclaimers, emergency rules, rate limits, QA.

---

## 8. Risks & dependencies

- **OpenRouter + upstream model**: health-related text goes through third parties — must appear in **privacy policy** and **first-run / assistant screen** copy.
- **Specialty string mismatch**: free-text `doctors.specialty` needs a **synonym map** or future normalized `specialty_code` column.
- **Regulatory / medical compliance**: triage-only positioning; local legal review before public launch.

---

## 9. Code references (repo)

- Navigation: `Mawid-Android/app/src/main/java/com/mawidplus/patient/ui/navigation/AppNavGraph.kt`
- Booking: `BookingViewModel.kt`, `AppointmentRepository.kt`
- Doctors: `DoctorRepository.kt`

---

*Initial design doc — revise as decisions are locked. Last updated: design handoff for Stitch / pre-development.*
