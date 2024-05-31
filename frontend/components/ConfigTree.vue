<template>
  <div>
    <ul class="list-unstyled">
      <li v-for="(value, key) in layout" :key="String(key)">
        <div @click="toggle(String(key))" class="cursor-pointer">
          <span v-if="isComplexType(value) || hasSubTypes(value)" class="fw-bold">
            <font-awesome-icon
              :icon="
                expandedKeys.includes(String(key))
                  ? 'fas fa-chevron-down'
                  : 'fas fa-chevron-right'
              "
              class="icon me-2"
            />
            <span>{{ getDescription(String(key), value) }}</span>
          </span>
          <span v-else>
            {{ getDescription(String(key), value) }}:
            <span class="badge bg-danger" v-if="value.required">
              Required
            </span>
            <span class="badge bg-warning" v-if="value.deprecated">
              Deprecated
            </span>
            <template
              v-if="value.type === 'Boolean' || value.type === 'boolean'"
            >
              <input
                type="checkbox"
                v-model="config[key]"
                class="form-check-input"
                :required="value.required"
              />
            </template>
            <template
              v-else-if="value.type === 'Integer' || value.type === 'int'"
            >
              <input
                type="number"
                v-model.number="config[key]"
                class="form-control"
                :required="value.required"
              />
            </template>
            <template
              v-else-if="value.type === 'String' || value.type === 'string'"
            >
              <input
                type="text"
                v-model="config[key]"
                class="form-control"
                :required="value.required"
              />
            </template>
          </span>
        </div>
        <div
          v-if="isComplexType(value) && expandedKeys.includes(String(key))"
          class="ms-4"
        >
          <ConfigTree
            v-if="value.fields && !value.isList"
            :layout="value.fields!"
            :config="config[key]"
          />
        </div>
        <div
          v-if="hasSubTypes(value) && expandedKeys.includes(String(key))"
          class="ms-4"
        >
          <div>
            <select
              :id="key"
              v-model="selectedSubTypes[key]"
              class="form-select mb-3"
              @change="handleSubTypeChange(key)"
            >
              <option
                v-for="(type, typeKey) in value.subTypes"
                :key="typeKey"
                :value="typeKey"
              >
                {{ getDescription(typeKey, type) }}
              </option>
            </select>
            <ConfigTree
              v-if="selectedSubTypes[key] && value.subTypes[selectedSubTypes[key]]"
              :layout="value.subTypes[selectedSubTypes[key]].fields!"
              :config="config[key] || (config[key] = {})"
            />
          </div>
        </div>
      </li>
    </ul>
    <span v-if="!layout">Error: Layout is empty</span>
  </div>
</template>
<script setup lang="ts">
import { ref, watch, defineProps } from "vue";

interface Field {
  deprecated: boolean;
  description: string;
  type: string;
  fields?: Record<string, Field>;
  subTypes?: Record<string, Field>;
  isList: boolean;
  required: boolean;
}

interface ConfigTreeProps {
  layout: Record<string, Field>;
  config: Record<string, any>;
}

const props = defineProps<ConfigTreeProps>();
const expandedKeys = ref<string[]>([]);
const selectedSubTypes = ref<Record<string, string>>({});

watch(
  () => props.config,
  (newConfig) => {
    for (const key in props.layout) {
      if (props.layout[key].subTypes && newConfig[key] && newConfig[key].type) {
        selectedSubTypes.value[key] = newConfig[key].type;
      }
    }
  },
  { deep: true, immediate: true }
);

const isComplexType = (val: Field) => {
  return (
    val.fields &&
    !val.subTypes &&
    val !== null &&
    typeof val === "object" &&
    !Array.isArray(val)
  );
};

const hasSubTypes = (val: Field) => {
  return (
    val.subTypes &&
    val !== null &&
    typeof val === "object" &&
    !Array.isArray(val)
  );
};

const toggle = (key: string) => {
  if (expandedKeys.value.includes(key)) {
    expandedKeys.value = expandedKeys.value.filter((k) => k !== key);
  } else {
    expandedKeys.value.push(key);
  }
};

const handleSubTypeChange = (key: string) => {
  const selectedType = selectedSubTypes.value[key];
  if (
    selectedType &&
    props.layout[key].subTypes &&
    props.layout[key].subTypes![selectedType]
  ) {
    const subTypeFields = props.layout[key].subTypes![selectedType].fields;
    if (subTypeFields) {
      props.config[key] = { type: selectedType };
      for (const subFieldKey in subTypeFields) {
        props.config[key][subFieldKey] = "";
      }
    }
  }
};

const getDescription = (key: string, value: Field) => {
  return value.description || key;
};
</script>
