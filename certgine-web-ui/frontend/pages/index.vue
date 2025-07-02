<template>
  <div
    class="h-screen w-full bg-gray-50 flex flex-col justify-center items-center px-4"
  >
    <div class="w-full max-w-sm space-y-6 bg-white p-6 rounded-xl shadow">
      <h1 class="text-3xl font-semibold text-center text-black">Certgine</h1>

      <!-- Tab navigation -->
      <div class="border-b border-gray-300 flex">
        <button
          :class="[
            'flex-1 text-center py-2 font-medium',
            isLogin
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : 'text-gray-500 hover:text-indigo-600',
          ]"
          @click="isLogin = true"
        >
          Login
        </button>
        <button
          :class="[
            'flex-1 text-center py-2 font-medium',
            !isLogin
              ? 'border-b-2 border-indigo-600 text-indigo-600'
              : 'text-gray-500 hover:text-indigo-600',
          ]"
          @click="isLogin = false"
        >
          Register
        </button>
      </div>

      <!-- Form Title -->
      <h2 class="text-center text-xl font-semibold text-gray-900">
        {{ isLogin ? "Sign in to your account" : "Create a new account" }}
      </h2>

      <!-- Auth form -->
      <form
        class="space-y-4"
        @submit.prevent="isLogin ? doLogin() : register()"
      >
        <div>
          <label class="block text-sm font-medium text-gray-700">Email</label>
          <input
            v-model="email"
            type="email"
            required
            class="mt-1 w-full rounded-md border border-gray-300 px-4 py-2 text-sm focus:ring-indigo-500 focus:border-indigo-500"
          >
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700"
            >Password</label
          >
          <input
            v-model="password"
            type="password"
            required
            class="mt-1 w-full rounded-md border border-gray-300 px-4 py-2 text-sm focus:ring-indigo-500 focus:border-indigo-500"
          >
        </div>

        <!-- TOTP for login -->
        <div v-if="isLogin && showTotp">
          <label class="block text-sm font-medium text-gray-700">TOTP</label>
          <input
            v-model="totp"
            type="text"
            class="mt-1 w-full rounded-md border border-gray-300 px-4 py-2 text-sm focus:ring-indigo-500 focus:border-indigo-500"
          >
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-indigo-600 text-white py-2 rounded-md font-semibold hover:bg-indigo-500 transition disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <span v-if="loading">Loading...</span>
          <span v-else>{{ isLogin ? "Sign in" : "Register" }}</span>
        </button>

        <p v-if="error" class="text-red-500 text-center text-sm mt-2">
          {{ error }}
        </p>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";

definePageMeta({ layout: "auth", title: "Auth" });
const isLogin = ref(true);
const email = ref("");
const password = ref("");
const totp = ref("");
const showTotp = ref(false);
const error = ref("");
const loading = ref(false);
const { $api } = useNuxtApp();

onMounted(async () => {
  try {
    const status = await $api("/core/status");
     if (status.firstrun) {
      isLogin.value = false; // Direkt zur Registrierung wechseln
     }
  } catch {}

  const token = localStorage.getItem("token");
  if (token) {
    navigateTo("/ui", { replace: true });
  }
});

async function doLogin() {
  error.value = "";
  loading.value = true;
  try {
    const res = await $api("/core/users/login", {
      method: "POST",
      body: { email: email.value, password: password.value, totp: totp.value },
    });

    if (res.token) {
      localStorage.setItem("token", res.token);
      await navigateTo("/ui");
    } else if (res.totpRequired || res.webauthnRequired) {
      showTotp.value = !!res.totpRequired;
      error.value = "Second factor required";
    }
  } catch {
    error.value = "Login failed";
  } finally {
    loading.value = false;
  }
}

async function register() {
  error.value = "";
  loading.value = true;
  try {
    await $api("/core/users/register", {
      method: "POST",
      body: { email: email.value, password: password.value },
    });
    isLogin.value = true;
  } catch {
    error.value = "Registration failed";
  } finally {
    loading.value = false;
  }
}
</script>
