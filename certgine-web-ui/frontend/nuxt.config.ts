// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-05-15',
  devtools: { enabled: true },
  modules: ['@nuxt/eslint', '@nuxtjs/tailwindcss'],
  plugins: ['~/plugins/chart.js', '~/plugins/api.ts'],
  ssr: false,
  runtimeConfig: {
    public: {
      API_URL: process.env.NUXT_PUBLIC_API_URL
    }
  }
})