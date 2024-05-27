<template>
  <div class="bg-body-tertiary p-5 rounded" style="padding-top: 4.5rem">
    <h1>🥳 ACME Server is running!</h1>
    <p>
      If you see this 🔒-Icon in your address bar of your browser, everything is
      correct configured. Otherwise, you have to import this CA Certificate onto
      your PC or Browser.
    </p>

    <div class="btn-group">
      <a
        :href="`${getApiBase()}/ca.crt`"
        class="btn btn-primary btn-lg"
        type="button"
      >
        <font-awesome-icon
          icon="fa-solid fa-download"
          class="icon"
        />&nbsp;Download CA certificate
      </a>
      <button
        type="button"
        class="btn btn-lg btn-primary dropdown-toggle dropdown-toggle-split"
        data-bs-reference="parent"
        data-bs-toggle="dropdown"
        aria-haspopup="true"
        aria-expanded="false"
      >
        <span class="visually-hidden">Toggle Dropdown</span>
      </button>
      <div class="dropdown-menu">
        <a class="dropdown-item" :href="`${getApiBase()}/ca.der`">
          <font-awesome-icon icon="fa-solid fa-download" class="icon" />&nbsp;
          Download in DER (Binary) Format
        </a>
        <a class="dropdown-item" :href="`${getApiBase()}/ca.pem`">
          <font-awesome-icon icon="fa-solid fa-download" class="icon" />&nbsp;
          Download in PEM (Base64) Format
        </a>
        <a class="dropdown-item" :href="`${getApiBase()}/ca.cab`">
          <font-awesome-icon icon="fa-solid fa-download" class="icon" />&nbsp;
          Download in CAB Format (Legacy Windows Mobile)
        </a>
      </div>
    </div>
  </div>

  <!-- Provisioners -->
  <h2 class="mb-3">Provisioners</h2>

  <div v-if="provisioners.length">
    <p class="mb-4">
      If you want to get more information about a provisioner, just click on the
      info icon in the provisioner tile. Copy the directory URL to use it in
      your ACME client or create a certificate using the GetHTTPSForFree UI.
    </p>
    <div id="provisioner-container" class="row g-4 gap-3 mb-4">
      <div
        v-for="provisioner in provisioners"
        :key="provisioner.name"
        class="col-md-6 col-lg-4"
      >
        <div class="card h-100 shadow-sm">
          <NuxtLink
            :to="`/provisioner-info?name=${provisioner.name}`"
            class="position-absolute top-0 end-0 p-2 text-reset"
          >
            <font-awesome-icon icon="fas fa-info-circle" class="icon" />
          </NuxtLink>
          <div class="card-body d-flex flex-column">
            <h5 class="card-title">{{ provisioner.name }}</h5>
            <div class="input-group mt-3 mb-1">
              <div class="form-floating flex-grow-1">
                <input
                  type="text"
                  class="form-control"
                  :name="`acme-directory-${provisioner.name}`"
                  :value="provisioner.directoryUrl"
                  readonly
                />
                <label :for="`acme-directory-${provisioner.name}`">
                  Directory URL:
                </label>
              </div>
              <button
                class="btn btn-outline-secondary"
                type="button"
                :id="`button-addon-${provisioner.name}`"
                @click="copy($event)"
              >
                <font-awesome-icon icon="fa-solid fa-copy" class="icon" />
              </button>
            </div>
            <a
              :href="`${getApiBase()}/gethttpsforfree/?provisioner=${
                provisioner.name
              }`"
              class="btn btn-secondary mt-2"
              target="_blank"
            >
              <font-awesome-icon icon="fas fa-globe" class="icon" /> Create
              manually using GetHTTPSForFree
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else-if="isLoading && !provisioners.length">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Loading...</span>
    </div>
  </div>
  <p v-else-if="!isLoading">No provisioners available.</p>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { ApiProvisionersListResponse } from "@/types/api";

const provisioners = ref<ApiProvisionersListResponse[]>([]);
const isLoading = ref(true);

onMounted(async () => {
  try {
    const response = await fetch(getApiBase() + "/api/provisioner/list");
    const data: ApiProvisionersListResponse[] = await response.json();
    provisioners.value = data;
  } catch (error) {
    console.error("Error fetching provisioners:", error);
  }
});

const copy = async (event: any) => {
  const element = event.currentTarget;
  const inputElement = element.parentElement.querySelector("input");

  const success = await copyTextToClipboard(inputElement.value);
  element.classList.remove("btn-outline-secondary");
  if (success) {
    element.classList.add("btn-success");
  } else {
    element.classList.add("btn-danger");
  }

  setTimeout(() => {
    element.classList.add("btn-outline-secondary");
    element.classList.remove("btn-success");
    element.classList.remove("btn-danger");
  }, 750);
};
</script>
