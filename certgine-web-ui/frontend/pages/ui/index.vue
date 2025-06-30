<template>
  <div class="space-y-6">
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatsCard title="Root CAs" :value="stats.rootCAs" />
      <StatsCard title="Intermediate CAs" :value="stats.intermediateCAs" />
      <StatsCard title="Certificates" :value="stats.certificates" />
      <StatsCard title="Provisioners" :value="stats.provisioners" />
    </div>
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="bg-white p-4 rounded-lg shadow h-80">
        <h2 class="text-lg font-semibold mb-2">Certificates Issued</h2>
        <div  class="h-64">
          <BarChart :chart-data="barData" :chart-options="barOptions" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { ChartData, ChartOptions } from 'chart.js'
import StatsCard from '~/components/dashboard/StatsCard.vue'
import BarChart from '~/components/dashboard/BarChart.vue'

const stats = {
  rootCAs: Math.floor(Math.random() * 5) + 1,
  intermediateCAs: Math.floor(Math.random() * 10) + 1,
  certificates: Math.floor(Math.random() * 500) + 100,
  provisioners: Math.floor(Math.random() * 5) + 1
}

const barData = ref<ChartData<'bar'>>({
  labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
  datasets: [
    {
      label: 'Issued',
      backgroundColor: '#4f46e5',
      data: Array.from({ length: 6 }, () => Math.floor(Math.random() * 100))
    }
  ]
})

const barOptions = ref<ChartOptions<'bar'>>({
  responsive: true,
  maintainAspectRatio: false
})


definePageMeta({
  title: 'Dashboard',
  meta: [
    { name: 'description', content: 'Dashboard' }
  ],
  layout: 'dashboard'
})
</script>
