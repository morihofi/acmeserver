<template>
    <h1>Configuration</h1>
    <div v-if="configLayout && currentConfig">
      <ConfigTree :layout="configLayout" :config="currentConfig" />
    </div>
    <div v-else>
      <p>Loading...</p>
    </div>
    <button @click="saveConfig" class="btn btn-primary">Save Config</button>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

const configLayout = ref(null);
const currentConfig = ref(null);

const fetchConfigData = async () => {
  try {
    const layoutResponse = await fetch(`${getApiBase()}/api/config/layout`);
    configLayout.value = await layoutResponse.json();

    const configResponse = await fetch(`${getApiBase()}/api/config/current`);
    currentConfig.value = await configResponse.json();
  } catch (error) {
    console.error('Error fetching config data:', error);
  }
};

const saveConfig = async () => {
  try {
    await fetch(`${getApiBase()}/api/config/current`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(currentConfig.value),
    });
    alert('Configuration saved successfully');
  } catch (error) {
    console.error('Error saving config data:', error);
    alert('Failed to save configuration');
  }
};

onMounted(() => {
  fetchConfigData();
});
</script>