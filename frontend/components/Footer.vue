<template>
  <footer
    class="d-flex flex-wrap justify-content-between align-items-center py-3 my-4 border-top"
  >
    <div class="col-md-4 d-flex align-items-center">
      <span class="mb-3 mb-md-0 text-muted">Made with ❤️ by morihofi</span>
    </div>
    <div class="col-md-4 d-flex align-items-center justify-content-center" v-if="serverInfo">
      <span class="mb-3 mb-md-0 text-muted">Version {{ serverInfo.metadataInfo.version }}</span>
    </div>
    <div class="col-md-4 d-flex align-items-center justify-content-end">
      <span class="mb-3 mb-md-0 text-muted">
        <a
          class="text-muted"
          target="_blank"
          to="https://github.com/morihofi/acmeserver"
        
        >
          <font-awesome-icon icon="fa-brands fa-github" class="icon"/>
      </a>
      </span>
    </div>
  </footer>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { ApiServerInfoResponse } from "@/types/api";

const serverInfo = ref<ApiServerInfoResponse>();

onMounted(async () => {
  try {
    const responseInfo = await fetch(getApiBase() + "/api/serverinfo");
    serverInfo.value = await responseInfo.json();
  } catch (error) {
    console.error("Error fetching server info:", error);
  }
});
</script>