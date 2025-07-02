<template>
  <div class="min-h-screen flex bg-slate-100 min-w-full">
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
