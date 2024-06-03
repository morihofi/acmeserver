<template>
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
      <NuxtLink class="navbar-brand" href="/">ACME Server</NuxtLink>
      <button
        class="navbar-toggler"
        type="button"
        data-bs-toggle="collapse"
        data-bs-target="#navbarSupportedContent"
        aria-controls="navbarSupportedContent"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav me-auto mb-2 mb-lg-0">
          <li class="nav-item" v-for="item in menuItems" :key="item.name">
            <NuxtLink
              :class="['nav-link', route.path === item.to ? 'active' : '']"
              aria-current="page"
              :to="item.to"
            >
              <font-awesome-icon :icon="item.icon" class="icon" />&nbsp;
              <span>{{ item.name }}</span>
            </NuxtLink>
          </li>
        </ul>
        <!--
            <ul class="d-flex">
                <a href="/mgmt" class="btn btn-outline-secondary">
                    <i class="fa-solid fa-arrow-right-to-bracket"></i>&nbsp;
                    ${localizer.lookup("web.core.menu.login")}
                </a>
            </ul>
            -->
      </div>
    </div>
  </nav>
</template>
<script setup lang="ts">
import { useRoute } from "vue-router";

const route = useRoute();
</script>

<script lang="ts">
export default {
  data() {
    return {
      menuItems: [
        { name: "Home", icon: "fa-solid fa-home", to: "/" },
        { name: "Statistics", icon: "fa-solid fa-chart-simple", to: "/stats" },
        {
          name: "Command Builder",
          icon: "fa-solid fa-terminal",
          to: "/cmd-builder",
        },
        {
          name: "Troubleshooting",
          icon: "fa-solid fa-wrench",
          to: "/troubleshooting",
        },
      ]
    };
  },
};
</script>
