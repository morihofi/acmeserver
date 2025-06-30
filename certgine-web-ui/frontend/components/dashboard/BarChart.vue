<template>
  <Bar :data="chartData" :options="chartOptions" />
</template>

<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import type { ChartData, ChartOptions } from 'chart.js'

defineProps<{ chartData: ChartData<'bar'>; chartOptions?: ChartOptions<'bar'> }>()
</script>
