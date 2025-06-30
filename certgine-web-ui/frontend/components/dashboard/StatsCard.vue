<template>
  <div class="bg-white rounded-lg shadow p-4 flex items-center">
    <div class="flex-1">
      <p class="text-sm text-gray-500">{{ title }}</p>
      <p class="text-2xl font-semibold">{{ value }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title: string; value: number }>()
</script>
