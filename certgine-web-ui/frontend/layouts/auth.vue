<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-100">
    <NuxtPage />
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  pageTransition: {
    name: 'bounce',
    mode: 'out-in'
  }
})
</script>
