<template>
  <Pie :data="chartData" :options="chartOptions" />
</template>

<script setup lang="ts">
import { Pie } from 'vue-chartjs'
import type { ChartData, ChartOptions } from 'chart.js'

defineProps<{ chartData: ChartData<'pie'>; chartOptions?: ChartOptions<'pie'> }>()
</script>
