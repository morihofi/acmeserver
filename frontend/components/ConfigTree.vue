<template>
    <div>
      <ul>
        <li v-for="(value, key) in layout" :key="String(key)">
          <div @click="toggle(String(key))" class="cursor-pointer">
            <span v-if="isComplexType(value)">{{ key }}</span>
            <span v-else>{{ key }}: 
              <input 
                type="text" 
                v-model="config[key]" 
                class="form-control"
              />
            </span>
          </div>
          <div v-if="isComplexType(value) && expandedKeys.includes(String(key))" class="ml-4">
            <ConfigTree :layout="value.fields || value.subTypes" :config="config[key]" />
          </div>
        </li>
      </ul>
    </div>
  </template>
  
  <script setup lang="ts">
  import { ref, watch } from 'vue';
  
  interface ConfigTreeProps {
    layout: any;
    config: any;
  }
  
  const props = defineProps<ConfigTreeProps>();
  const expandedKeys = ref<string[]>([]);
  
  const isComplexType = (val: any) => {

    const type = val.type;

    if(type === "String"){
        return false;
    }
    if(type === "Integer"){
        return false;
    }
    if(type === "Float" || type === "Double"){
        return false;
    }
    if(type === "Boolean"){
        return false;
    }

    return val !== null && typeof val === 'object' && !Array.isArray(val);

};
  
  const toggle = (key: string) => {
    if (expandedKeys.value.includes(key)) {
      expandedKeys.value = expandedKeys.value.filter(k => k !== key);
    } else {
      expandedKeys.value.push(key);
    }
  };
  </script>
  
  <style scoped>
  /* Optional: Add any additional styles here */
  </style>
  