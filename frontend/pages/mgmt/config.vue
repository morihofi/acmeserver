<template>
  <h1>Configuration</h1>
  Edit the Server Configuration. <span class="fw-bold">Please refer to manual.</span> If you configure it wrong, you may can't access
  this ACME Server instance. In this case you need to manually edit the JSON
  Config File according to the documentation.
  <div v-if="configLayout && currentConfig">
    <ConfigTree :layout="configLayout" :config="currentConfig" />
  </div>
  <div v-else>
    <p>Loading...</p>
  </div>

  <button @click="saveConfig" class="btn btn-primary">Save Config</button>
  <button @click="reviewChanges" class="btn btn-secondary">Review Changes</button>

  <div v-if="showChanges" class="mt-4">
      <h2>Changes</h2>
      <pre>{{ changes }}</pre>
    </div>

</template>
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { cloneDeep, isEqual } from 'lodash-es';

const configLayout = ref(null);
const currentConfig = ref(null);
const originalConfig = ref(null);
const showChanges = ref(false);
const changes = ref({});

const fetchConfigData = async () => {
  try {
    const layoutResponse = await fetch(`${getApiBase()}/api/config/layout`);
    configLayout.value = await layoutResponse.json();

    const configResponse = await fetch(`${getApiBase()}/api/config/current`);
    currentConfig.value = await configResponse.json();
    originalConfig.value = cloneDeep(currentConfig.value);  // Speichere die Originalkonfiguration
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

const reviewChanges = () => {
  changes.value = getChanges(originalConfig.value, currentConfig.value);
  showChanges.value = true;
};

const getChanges = (original: any, current: any) => {
  const changes : any = {};
  for (const key in original) {
    if (!isEqual(original[key], current[key])) {
      changes[key] = { original: original[key], current: current[key] };
    }
  }
  return changes;
};

onMounted(() => {
  fetchConfigData();
});
</script>
