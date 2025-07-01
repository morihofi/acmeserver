import { useAuth } from '~/composables/useAuth'

export default defineNuxtRouteMiddleware(async (to, from) => {
  const { user, load } = useAuth()
  if (!user.value) {
    await load()
  }
  if (!user.value) {
    return navigateTo('/login')
  }
})
