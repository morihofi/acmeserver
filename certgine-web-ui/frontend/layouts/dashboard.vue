<template>
  <div class="bg-slate-100 w-screen h-screen flex">
    <LayoutDashboardSidebar :open="sidebarOpen" @close="closeSidebar" />

    <div class="flex-1 h-full flex flex-col overflow-hidden">
      <LayoutDashboardHeader @toggle-sidebar="toggleSidebar" />
      <main class="flex-1 overflow-y-auto p-4">
        <NuxtPage />
      </main>
    </div>

    <div
      v-if="sidebarOpen"
      class="fixed inset-0 bg-black/50 md:hidden z-30"
      @click="closeSidebar"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { useMediaQuery } from "@vueuse/core";

definePageMeta({
  pageTransition: {
    name: "bounce",
    mode: "out-in", // default
  },
});

const isDesktop = useMediaQuery("(min-width: 768px)");
const sidebarOpen = ref(isDesktop.value);

watch(isDesktop, (val) => {
  sidebarOpen.value = val;
});

const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value;
};

const closeSidebar = () => {
  sidebarOpen.value = false;
};
</script>
