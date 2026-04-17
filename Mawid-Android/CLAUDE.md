# Mawid+ Android (Patient App)

## Stack
- Kotlin + Jetpack Compose
- Supabase Kotlin SDK (io.github.jan-tennert.supabase)
- Coroutines + Flow
- Hilt for DI
- MVVM pattern

## Project Structure
- core/model → data classes
- core/network → SupabaseClient singleton
- data/repository → all Supabase calls
- ui/screens → one folder per screen (Screen + ViewModel)
- ui/components → reusable Compose components

## Supabase Tables
- profiles(id, full_name, phone, role)
- doctors(id, clinic_id, specialty, slot_duration_minutes)
- appointments(id, patient_id, doctor_id, queue_number, status, appointment_date)
- queue_settings(doctor_id, current_number, queue_date, is_open)

## Rules
- MVVM: no business logic inside Composables
- All Supabase calls inside Repository only
- Use Result<T> sealed class for all responses
- Use StateFlow in ViewModels, not LiveData
- Every composable takes data + lambdas as params (no direct ViewModel access)
- Handle loading + error states in every screen