<template>
    <h1>Redirecting ...</h1>
</template>
<script setup lang="ts">
import { onMounted } from 'vue'
const { $api } = useNuxtApp()
onMounted(async () => {
  try {
    const status = await $api('/core/status')
    if (status.firstrun) {
      navigateTo('/register', { replace: true })
      return
    }
  } catch {}
  const token = localStorage.getItem('token')
  if (!token) {
    navigateTo('/login', { replace: true })
  } else {
    navigateTo('/ui', { replace: true })
  }
})
</script>
