<template>
  <header
    class="w-full bg-white shadow-sm flex items-center justify-between px-6 py-4 sticky top-0 z-50"
  >
    <!-- Seitentitel -->
    <div class="flex items-center space-x-2">
      <button class="md:hidden" @click="$emit('toggle-sidebar')">
        <LucideMenu class="size-5 text-gray-500" />
      </button>
      <h1 class="text-xl font-semibold text-gray-800">{{ pageTitle }}</h1>
    </div>

    <!-- Icons: Notification + User -->
    <div class="flex items-center align-baseline space-x-4">
      <!-- Notification Bell -->
      <div ref="notificationsRef" class="relative flex items-center">
        <button class="focus:outline-none" @click="toggleNotifications">
          <LucideBell class="size-5 text-gray-500 hover:text-indigo-600" />
        </button>
        <div
          v-if="showNotifications"
          class="absolute right-0 top-full mt-2 w-80 bg-white border rounded shadow-md z-50"
        >
          <div class="p-4 border-b font-semibold">Notifications</div>
          <ul>
            <li
              v-for="note in notifications"
              :key="note.id"
              class="p-4 hover:bg-gray-100 text-sm"
            >
              <p>
                <strong>{{ note.name }}</strong> requests permission for
                <em>{{ note.project }}</em>
              </p>
              <p class="text-xs text-gray-500">{{ note.time }}</p>
            </li>
          </ul>
        </div>
      </div>

      <!-- User Dropdown -->
      <div ref="userMenuRef" class="relative">
        <button class="flex items-center space-x-2" @click="toggleUserMenu">
          <img
            class="w-8 h-8 rounded-full"
            src="https://images.unsplash.com/photo-1472099645785-5658abf4ff4e"
          />
        </button>
        <div
          v-if="showUserMenu"
          class="absolute right-0 top-full mt-2 w-48 bg-white border rounded shadow-md z-50"
        >
          <div class="px-4 py-3 border-b">
            <p class="text-sm font-semibold">User</p>
            <p class="text-xs text-gray-500">user@example.com</p>
          </div>
          <ul class="text-sm">
            <li>
              <a href="#" class="block px-4 py-2 hover:bg-gray-100"
                >Edit Profile</a
              >
            </li>
            <li>
              <a href="#" class="block px-4 py-2 hover:bg-gray-100"
                >Account Settings</a
              >
            </li>
            <li>
              <a href="#" class="block px-4 py-2 hover:bg-gray-100">Support</a>
            </li>
            <li>
              <a href="#" class="block px-4 py-2 text-red-600 hover:bg-gray-100"
                >Sign Out</a
              >
            </li>
          </ul>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { LucideBell, LucideMenu } from "lucide-vue-next";
import { ref } from "vue";
import { onClickOutside } from "@vueuse/core";

import { useRoute } from "vue-router";

defineEmits(["toggle-sidebar"]);

const showNotifications = ref(false);
const showUserMenu = ref(false);
const notificationsRef = ref<HTMLElement | null>(null);
const userMenuRef = ref<HTMLElement | null>(null);

onMounted(() => {
  if (!notificationsRef.value || !userMenuRef.value) {
    return;
  }

  onClickOutside(userMenuRef, () => (showUserMenu.value = false));
  onClickOutside(notificationsRef, () => (showNotifications.value = false));
});

const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value;
  showUserMenu.value = false;
};

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value;
  showNotifications.value = false;
};

const notifications = ref([
  { id: 1, name: "Terry Franci", project: "Nganter App", time: "5 min ago" },
  { id: 2, name: "Alena Franci", project: "Nganter App", time: "8 min ago" },
  { id: 3, name: "Jocelyn Kenter", project: "Nganter App", time: "15 min ago" },
  { id: 4, name: "Brandon Philips", project: "Nganter App", time: "1 hr ago" },
]);

const pageTitle = useRoute().meta.title;
</script>
