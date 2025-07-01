<template>
  <div class="space-y-4">
    <div class="flex justify-between items-center">
      <h1 class="text-xl font-semibold">Root Certificate Authorities</h1>
      <NuxtLink to="/ui/pki/root-cas/new" class="bg-blue-600 text-white px-3 py-1 rounded">New Root CA</NuxtLink>
    </div>
    <table class="min-w-full bg-white rounded-lg shadow">
      <thead class="bg-gray-50">
        <tr>
          <th class="px-4 py-2 text-left text-sm font-medium text-gray-600">Certificate Authority</th>
          <th class="px-4 py-2 text-left text-sm font-medium text-gray-600">Downloads</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ca in cas" :key="ca.id" class="border-t">
          <td class="px-4 py-3">
            <p class="font-medium">{{ ca.name }}</p>
            <p class="text-sm text-gray-500">{{ ca.description }}</p>
            <NuxtLink :to="`/ui/pki/root-cas/${ca.id}`" class="text-blue-600 text-sm">Show details</NuxtLink>
          </td>
          <td class="px-4 py-3 space-y-1">
            <a :href="ca.pemPath" class="text-blue-600 flex items-center gap-1"><LucideIcon name="Download" class="w-4 h-4"/> PEM</a>
            <a :href="ca.derPath" class="text-blue-600 flex items-center gap-1"><LucideIcon name="Download" class="w-4 h-4"/> DER</a>
            <a :href="ca.cabPath" class="text-blue-600 flex items-center gap-1"><LucideIcon name="Download" class="w-4 h-4"/> CAB</a>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { useFetch } from '#app'
import LucideIcon from '~/components/component/LucideIcon.vue'

const { data: cas } = await useFetch('/api/rootcas')

definePageMeta({
  title: 'Root CAs',
  layout: 'dashboard'
})
</script>
