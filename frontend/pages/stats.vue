<template>
  <h1>Statistics</h1>
  <div class="container mt-3 mb-3">
    <!--div class="row">
      <div
        class="col-md-3 mb-3"
        v-for="item in statisticsProvisioner"
        :key="item.name"
      >
        <div class="card text-center">
          <div class="card-body">
            <h5 class="card-title display-6 fw-bold">{{ item.name }}</h5>
            <p class="card-text">Test</p>
          </div>
        </div>
      </div>
    </div-->

    <div
      v-for="statistic in statisticsProvisioner"
      :key="statistic.name"
      class="card mb-3"
    >
      <NuxtLink
        :to="`/provisioner-info?name=${statistic.name}`"
        class="position-absolute top-0 end-0 p-2 text-reset"
      >
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
      <!--div class="chart-container">
        <canvas
          class="chart-issuedCertificate mb-3 ml-3 mr-3"
          :data-provisioner="statistic.name"
        ></canvas>
      </div>
    </div-->
      <!--h2>Database stats</h2>
    <div class="container mt-3 mb-3">
      <div class="row">
        <div
          class="col-md-3 mb-3"
          v-for="item in statisticsDatabase"
          :key="item.translationKey"
        >
          <div class="card text-center mb-3">
            <div class="card-body">
              <h5 class="card-title display-6 fw-bold">{{ item.number }}</h5>
              <p class="card-text">{{ localize(item.translationKey) }}</p>
            </div>
          </div>
        </div>
      </div>
    </div-->
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { ApiProvisionersStatsResponse } from "@/types/api";

const statisticsProvisioner = ref<ApiProvisionersStatsResponse[]>([]);

onMounted(async () => {
  try {
    const response = await fetch(getApiBase() + "/api/provisioner/stats");
    const data: ApiProvisionersStatsResponse[] = await response.json();
    statisticsProvisioner.value = data;
  } catch (error) {
    console.error("Error fetching provisioners:", error);
  }
});
</script>
