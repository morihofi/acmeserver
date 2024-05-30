<template>
  <h1>Server Statistics</h1>

  <h2>Global</h2>
  <div class="row" v-if="statisticsGlobal">

    <!-- ACME Accounts -->
    <div class="col-md-3 mb-3">
      <div class="card text-center">
        <div class="card-body">
          <h5 class="card-title display-6 fw-bold">{{ statisticsGlobal.acmeAccounts }}</h5>
          <p class="card-text">Active ACME Accounts</p>
        </div>
      </div>
    </div>

    <!-- Total Issued Certificates -->
    <div class="col-md-3 mb-3">
      <div class="card text-center">
        <div class="card-body">
          <h5 class="card-title display-6 fw-bold">{{ statisticsGlobal.certificatesIssued }}</h5>
          <p class="card-text">Total Issued Certificates</p>
        </div>
      </div>
    </div>

    <!-- Total Issued Certificates -->
    <div class="col-md-3 mb-3">
      <div class="card text-center">
        <div class="card-body">
          <h5 class="card-title display-6 fw-bold">{{ statisticsGlobal.certificatesIssueWaiting }}</h5>
          <p class="card-text">Total Certificates waiting for Issue</p>
        </div>
      </div>
    </div>

    <!-- Total Issued Certificates -->
    <div class="col-md-3 mb-3">
      <div class="card text-center">
        <div class="card-body">
          <h5 class="card-title display-6 fw-bold">{{ statisticsGlobal.certificatesRevoked }}</h5>
          <p class="card-text">Total Certificates Revoked</p>
        </div>
      </div>
    </div>

  </div>



  <h2>Statistics by Provisioner</h2>

  <div v-if="statisticsProvisioner.length" v-for="statistic in statisticsProvisioner" :key="statistic.name"
    class="card mb-3">
    <NuxtLink :to="`/provisioner-info?name=${statistic.name}`" class="position-absolute top-0 end-0 p-2 text-reset">
      <font-awesome-icon icon="fas fa-info-circle" class="icon" />
    </NuxtLink>
    <div class="card-header">
      {{ statistic.name }}
    </div>
    <div class="card-body">
      <div class="container mt-3 mb-3">
        <div class="row">
          <!-- ACME Accounts Card -->
          <div class="col-md-3 mb-3">
            <div class="card text-center">
              <div class="card-body">
                <h5 class="card-title display-6 fw-bold">
                  {{ statistic.acmeAccounts }}
                </h5>
                <p class="card-text">ACME Accounts</p>
              </div>
            </div>
          </div>
          <!-- Certificates Issued -->
          <div class="col-md-3 mb-3">
            <div class="card text-center">
              <div class="card-body">
                <h5 class="card-title display-6 fw-bold">
                  {{ statistic.certificatesIssued }}
                </h5>
                <p class="card-text">Certificates Issued</p>
              </div>
            </div>
          </div>
          <!-- Certificates waiting for Issue -->
          <div class="col-md-3 mb-3">
            <div class="card text-center">
              <div class="card-body">
                <h5 class="card-title display-6 fw-bold">
                  {{ statistic.certificatesIssueWaiting }}
                </h5>
                <p class="card-text">Certificates waiting for Issue</p>
              </div>
            </div>
          </div>
          <!-- Certificates revoked -->
          <div class="col-md-3 mb-3">
            <div class="card text-center">
              <div class="card-body">
                <h5 class="card-title display-6 fw-bold">
                  {{ statistic.certificatesRevoked }}
                </h5>
                <p class="card-text">Certificates revoked</p>
              </div>
            </div>
          </div>
          <!-- End cards -->
        </div>
      </div>
    </div>

  </div>
  <div v-else>
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Loading...</span>
    </div>
  </div>

</template>
<script setup lang="ts">
import { ref } from "vue";
import { ApiProvisionersStatsResponse } from "@/types/api";

const statisticsProvisioner = ref<ApiProvisionersStatsResponse[]>([]);
const statisticsGlobal = ref<ApiProvisionersStatsResponse>();

onMounted(async () => {
  try {
    const responseProvisioner = await fetch(getApiBase() + "/api/stats/provisioner-all");
    statisticsProvisioner.value = await responseProvisioner.json();

    const responseGlobal = await fetch(getApiBase() + "/api/stats/provisioner-global");
    statisticsGlobal.value = await responseGlobal.json();

  } catch (error) {
    console.error("Error fetching data:", error);
  }
});
</script>
