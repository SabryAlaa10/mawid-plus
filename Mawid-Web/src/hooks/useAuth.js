import { createContext, createElement, useContext, useEffect, useState, useCallback, useMemo } from 'react'
import { supabase } from '../lib/supabaseClient'
import * as authService from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let mounted = true
    supabase.auth.getSession().then(({ data: { session } }) => {
      if (!mounted) return
      setUser(session?.user ?? null)
      setLoading(false)
    })

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      setUser(session?.user ?? null)
      setLoading(false)
    })

    return () => {
      mounted = false
      subscription.unsubscribe()
    }
  }, [])

  const signIn = useCallback(async (email, password) => {
    setError(null)
    setLoading(true)
    try {
      await authService.signInWithEmail(email, password)
    } catch (e) {
      setError(e.message || 'فشل تسجيل الدخول')
      throw e
    } finally {
      setLoading(false)
    }
  }, [])

  const signOut = useCallback(async () => {
    setError(null)
    try {
      await authService.signOut()
    } catch (e) {
      setError(e.message || 'فشل تسجيل الخروج')
      throw e
    }
  }, [])

  const value = useMemo(
    () => ({ user, loading, error, signIn, signOut, setError }),
    [user, loading, error, signIn, signOut]
  )

  return createElement(AuthContext.Provider, { value }, children)
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
