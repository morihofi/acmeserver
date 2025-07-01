<template>
  <div class="max-w-sm mx-auto mt-10 space-y-4">
    <input v-model="email" placeholder="Email" class="border w-full p-2" />
    <input v-model="password" type="password" placeholder="Password" class="border w-full p-2" />
    <button class="bg-blue-500 text-white px-4 py-2" @click="register">Register</button>
    <p v-if="error" class="text-red-500">{{ error }}</p>
  </div>
</template>
<script setup lang="ts">
definePageMeta({
  layout: 'auth',
  title: 'Register'
})
const email = ref('')
const password = ref('')
const error = ref('')
const { $api } = useNuxtApp()

async function register() {
  try {
    await $api('/core/users/register', {
      method: 'POST',
      body: { email: email.value, password: password.value }
    })
    await navigateTo('/login')
  } catch {
    error.value = 'Registration failed'
  }
}
</script>
