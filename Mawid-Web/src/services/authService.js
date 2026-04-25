import { supabase } from '../lib/supabaseClient'

export async function signInWithEmail(email, password) {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw error
  return data
}

export async function signOut() {
  const { error } = await supabase.auth.signOut()
  if (error) throw error
}

export function getCurrentUser() {
  return supabase.auth.getUser()
}

export function getCurrentSession() {
  return supabase.auth.getSession()
}

export async function signUpDoctor({ fullName, specialty, email, password }) {
  const { data, error } = await supabase.auth.signUp({
    email,
    password,
    options: {
      data: {
        full_name: fullName,
        specialty,
        role: 'doctor',
      }
    }
  })
  if (error) throw error
  return data
}
