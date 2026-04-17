# Mawid+ Web (Doctor Dashboard)

## Stack
- React 18 + Vite
- Supabase JS (`@supabase/supabase-js`)
- React Router v6
- TailwindCSS
- Recharts, date-fns
- Auth: `AuthProvider` + `useAuth()` (shared session)

## Run
```bash
cd Mawid-Web
npm install
npm run dev
```
Open `http://localhost:5173`

## Env
Copy `.env.example` → `.env` with `VITE_SUPABASE_URL` and `VITE_SUPABASE_ANON_KEY` (same as Android `app/build.gradle.kts`).

## Supabase
- Apply migrations in `Mawid-Android/supabase/migrations/` including **007_doctor_email_auth.sql** (doctor RLS + columns).
- Link each doctor login: `auth.users.id` = `doctors.id` (see **008_seed_doctor_auth.sql**).
- Enable **Realtime** replication for `appointments` and `queue_settings` in the Supabase dashboard.

## Structure
- `src/lib/supabaseClient.js` — client
- `src/services/*` — all DB calls
- `src/hooks/*` — state + realtime cleanup
- `src/pages/*` — routes
- `src/components/*` — UI
- `src/router/index.jsx` — routes + protected layout

## Rules
- RTL + primary `#1A73E8`, sidebar `#1E293B`
- No hardcoded doctor IDs; use `user.id` from Supabase Auth
- Realtime: always `removeChannel` / returned cleanup from hooks

## Production readiness (repo)
- Run `npm test` then `npm run build` (needs `VITE_SUPABASE_*` in `.env`).
- Apply all SQL in `Mawid-Android/supabase/migrations/` (including `015_performance_indexes.sql`) on the Supabase project.
- CI: `.github/workflows/ci.yml` runs Web tests/build and Android unit tests.
- Never commit `service_role` keys; clients use **anon** + RLS only.
