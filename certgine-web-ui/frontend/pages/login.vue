<template>
  <div class="max-w-sm mx-auto mt-10 space-y-4">
    <input v-model="email" placeholder="Email" class="border w-full p-2" />
    <input v-model="password" type="password" placeholder="Password" class="border w-full p-2" />
    <input v-if="showTotp" v-model="totp" placeholder="TOTP" class="border w-full p-2" />
    <button class="bg-blue-500 text-white px-4 py-2" @click="doLogin">Login</button>
    <p v-if="error" class="text-red-500">{{ error }}</p>
  </div>
</template>
<script setup lang="ts">
definePageMeta({
  layout: 'auth',
  title: 'Login'
})
const email = ref('')
const password = ref('')
const totp = ref('')
const showTotp = ref(false)
const error = ref('')
import type { LoginResponse } from '~/types/user'
const { $api } = useNuxtApp()

async function doLogin() {
  try {
    const res: LoginResponse = await $api('/core/users/login', {
      method: 'POST',
      body: { email: email.value, password: password.value, totp: totp.value }
    })
    if (res.token) {
      localStorage.setItem('token', res.token)
      await navigateTo('/ui')
    } else if (res.totpRequired || res.webauthnRequired) {
      showTotp.value = !!res.totpRequired
      error.value = 'Second factor required'
    }
  } catch (e) {
    error.value = 'Login failed'
  }
}
</script>
