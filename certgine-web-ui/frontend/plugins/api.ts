import { ofetch } from 'ofetch'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const api = ofetch.create({
    baseURL: config.public.API_URL || '/api',
    headers: () => {
      const t = localStorage.getItem('token')
      return t ? { Authorization: `Bearer ${t}` } : {}
    }
  })
  return { provide: { api } }
})
