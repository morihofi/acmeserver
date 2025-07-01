import type { UserInfo } from '~/types/user'

export const useAuth = () => {
  const user = useState<UserInfo | null>('user', () => null)
  const { $api } = useNuxtApp()

  async function load() {
    try {
      user.value = await $api('/core/users/info')
    } catch {
      user.value = null
    }
  }

  return { user, load }
}
