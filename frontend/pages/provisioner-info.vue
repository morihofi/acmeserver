<template>
    <h1>Datails of Provisioner {{ provisionerName }}</h1>

</template>
<script setup lang="ts">
import { useRoute } from 'vue-router';

const route = useRoute();
const provisionerName = route.query.name;

</script>