import { ofetch } from 'ofetch'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const api = ofetch.create({
    baseURL: config.public.API_URL || '/api',
    onRequest({ options }) {
      if (process.client) {
        const t = localStorage.getItem('token')
        if (t) {
          options.headers = {
            ...(options.headers || {}),
            Authorization: `Bearer ${t}`
          }
        }
      }
    }
  })
  return { provide: { api } }
})
