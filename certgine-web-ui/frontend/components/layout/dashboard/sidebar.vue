<template>
  <aside class="w-80 bg-gray-900 text-white flex flex-col h-full">
    <!-- Header -->
    <div class="p-4 border-b border-gray-800 h-16">
      <div class="flex items-center justify-between">
        <!--img
          src="~/static/images/logo.svg"
          alt="Logo"
          class="h-8 w-auto"
        /-->
        <span class="text-xl font-bold">Certgine</span>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="p-4">
      <div class="relative">
        <input
          v-model="search"
          type="text"
          class="w-full bg-gray-800 text-white rounded-md pl-10 pr-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          placeholder="Search..."
        />
        <div
          class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"
        >
          <svg
            class="h-5 w-5 text-gray-500"
            xmlns="http://www.w3.org/2000/svg"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path
              fill-rule="evenodd"
              d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
              clip-rule="evenodd"
            />
          </svg>
        </div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="mt-5 px-2 flex-1 overflow-y-auto">
      <div class="space-y-2">
        <template v-for="(item, index) in filteredMenu" :key="index">
          <!-- Direct link -->
          <a
            v-if="!item.children"
            :href="item.link"
            :class="[
              'flex items-center px-4 py-2.5 text-sm font-medium rounded-lg',
              isActive(item.link)
                ? 'bg-gray-700 text-white'
                : 'text-gray-300 hover:bg-gray-700 hover:text-white',
            ]"
            class="flex items-center px-4 py-2.5 text-sm font-medium rounded-lg text-gray-300 hover:bg-gray-700 hover:text-white"
          >
            <ComponentLucideIcon :icon="item.icon" class="h-5 w-5 mr-3" />
            {{ item.label }}
          </a>

          <!-- Dropdown -->
          <div v-else>
            <button
              class="w-full flex items-center justify-between px-4 py-2.5 text-sm font-medium rounded-lg text-gray-300 hover:bg-gray-700 hover:text-white"
              @click="toggle(index)"
            >
              <div class="flex items-center">
                <ComponentLucideIcon :icon="item.icon" class="h-5 w-5 mr-3" />
                {{ item.label }}
              </div>
              <svg
                class="ml-2 h-5 w-5 transform transition-transform duration-200"
                :class="{ 'rotate-180': isOpen(index) }"
                xmlns="http://www.w3.org/2000/svg"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path
                  fill-rule="evenodd"
                  d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
            <div v-show="isOpen(index)" class="space-y-1 pl-11">
              <a
                v-for="(child, cIdx) in item.children"
                :key="cIdx"
                :href="child.link"
                :class="[
                  'block px-4 py-2 text-sm rounded-md',
                  isActive(child.link)
                    ? 'bg-gray-700 text-white'
                    : 'text-gray-300 hover:bg-gray-700 hover:text-white',
                ]"
              >
                {{ child.label }}
              </a>
            </div>
          </div>
        </template>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import type { icons } from "lucide-vue-next";
import { useRoute } from "vue-router";

const route = useRoute();

const isActive = (link?: string) => {
  if (!link) return false;
  return route.path === link;
};

const search = ref("");
const openIndexes = ref<number[]>([]);

type MenuItem = {
  label: string;
  link?: string;
  icon: keyof typeof icons;
  children?: { label: string; link: string }[];
};

const menuItems: MenuItem[] = [
  { label: "Dashboard", link: "/ui", icon: "LayoutDashboard" },
  {
    label: "PKI",
    icon: "KeyRound",
    children: [
      { label: "Overview", link: "#" },
      { label: "Root CAs", link: "#" },
      { label: "Intermediate CAs", link: "#" },
      { label: "KeyStore", link: "#" },
    ],
  },
  {
    label: "ACME",
    icon: "Bot",
    children: [
      { label: "Overview", link: "#" },
      { label: "Provisioner", link: "#" },
      { label: "Accounts", link: "#" },
      { label: "Orders", link: "#" },
    ],
  },
  {
    label: "Timestamp Authority",
    icon: "Stamp",
    children: [{ label: "Configuration", link: "#" }],
  },
  {
    label: "System",
    icon: "Activity",
    children: [
      { label: "JVM Info", link: "#" },
      { label: "Metrics", link: "#" },
      { label: "Logs", link: "#" },
      { label: "General Configuration", link: "#" },
    ],
  },
];

const toggle = (index: number) => {
  if (openIndexes.value.includes(index)) {
    openIndexes.value = openIndexes.value.filter((i) => i !== index);
  } else {
    openIndexes.value.push(index);
  }
};

const isOpen = (index: number) => openIndexes.value.includes(index);

// Filter menu + subitems
const filteredMenu = computed(() => {
  if (!search.value.trim()) return menuItems;

  const term = search.value.toLowerCase();

  return menuItems
    .map((item) => {
      if (item.children) {
        const filteredChildren = item.children.filter((child) =>
          child.label.toLowerCase().includes(term)
        );
        if (
          item.label.toLowerCase().includes(term) ||
          filteredChildren.length > 0
        ) {
          return {
            ...item,
            children: filteredChildren,
          };
        }
      } else if (item.label.toLowerCase().includes(term)) {
        return item;
      }
      return null;
    })
    .filter(Boolean) as MenuItem[];
});
</script>
