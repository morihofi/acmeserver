<template>
  <h1>ACME Client setup</h1>
  <p class="fs-6">
    This wizard prepares the command line commands for common ACME clients for
    copying and pasting. You can then customize the commands to suit your
    requirements. Please note, that you must have the CA certificate of this
    ACME Server trusted (installed) on the machine you want to use the ACME
    client. Otherwise, it won't work.
  </p>

  <form>
    <div class="row g-3">
      <div class="col-md-6">
        <label for="emailInput" class="form-label">Email</label>
        <input
          type="email"
          class="form-control"
          id="emailInput"
          v-model="email"
          placeholder="info@example.com"
          required
        />
      </div>
      <div class="col-md-6">
        <label for="domainInput" class="form-label">Domain/IP-Address</label>
        <input
          type="text"
          class="form-control"
          id="domainInput"
          v-model="domain"
          placeholder="example.com"
          required
        />
      </div>
      <div class="col-12">
        <label for="select-provisioner" class="form-label">Provisioner</label>
        <select
          class="form-select"
          id="select-provisioner"
          v-model="provisioner"
          required
        >
          <option value="">
            Choose a Provisioner...
          </option>
          <option
            v-for="provisioner in provisioners"
            :key="provisioner.name"
            :value="provisioner.name"
          >
            {{ provisioner.name }}
          </option>
        </select>
      </div>
    </div>
  </form>

  <div
    v-if="showCommands"
    class="bg-light p-4 my-4 rounded-3 shadow-sm"
  >
    <h2 class="h4">Choose your preferred ACME client</h2>
    <p>
      The code examples below are only guidelines. Customize the commands to
      your needs.
    </p>

    <!-- Certbot Section -->
    <div class="bg-white p-3 my-3 rounded-3 shadow-sm">
      <h3 class="mt-3">Certbot</h3>
      <p>
        Certbot is a free, open-source software tool recommended by Let's
        Encrypt to automatically manage certificates via the ACME protocol on
        self-hosted websites and enable HTTPS. This example uses the nginx
        plugin of certbot. Instead of use --nginx you can also use --manual.
        Please refer to certbot documentation.
      </p>
      <pre
        class="text-black"
      ><code>certbot -n --nginx -d {{ domain }} --server {{ directoryUrl }} --agree-tos --email {{ email }}</code></pre>
    </div>

    <!-- acme.sh Section -->
    <div class="bg-white p-3 rounded-3 shadow-sm">
      <h3 class="mt-3">acme.sh</h3>
      <p>
        acme.sh is an ACME protocol client written in shell that can be used to
        retrieve certificates from an ACME server. It has been designed to be
        purely POSIX compliant and therefore works on a variety of shell
        environments and operating systems. This example uses the standalone
        mode.
      </p>
      <pre
        class="text-black"
      ><code>acme.sh --register-account --server {{ directoryUrl }}</code></pre>
      <pre
        class="text-black"
      ><code>acme.sh --issue --server {{ directoryUrl }} -d {{ domain }} --standalone</code></pre>
    </div>

    <!-- Win-ACME Section -->
    <div class="bg-white p-3 my-3 rounded-3 shadow-sm">
      <h3 class="mt-3">Win-ACME</h3>
      <p>
        Win-ACME is an open source ACME client specifically for Windows that
        offers a wide range of automation options for certificate management,
        including seamless integration with IIS (Internet Information Services).
        Win-ACME is characterized by its ease of use and flexibility, making it
        an excellent choice for Windows-based environments that require
        efficient certificate management. This example uses the IIS web server
        to provide the ACME challenges.
      </p>
      <pre
        class="text-black"
      ><code>wacs --target manual --host {{ domain }} --validation selfhosting --store pemfiles --pemfilespath . --baseuri {{ directoryUrl }} --emailaddress {{ email }} --accepttos</code></pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { ApiProvisionersListResponse } from "@/types/api";

const provisioners = ref<ApiProvisionersListResponse[]>([]);

const email = ref("");
const domain = ref("");
const provisioner = ref("");
const directoryUrl = ref("");

onMounted(async () => {
  try {
    const response = await fetch(getApiBase() + "/api/provisioner/list");
    const data: ApiProvisionersListResponse[] = await response.json();
    provisioners.value = data;
  } catch (error) {
    console.error("Error fetching provisioners:", error);
  }
});

const showCommands = computed(() => {
  if (email.value && domain.value && provisioner.value) {
    const selectedProvisioner = provisioners.value.find(
      (p) => p.name === provisioner.value
    );
    if (selectedProvisioner) {
      directoryUrl.value = selectedProvisioner.directoryUrl;
      return true;
    }
  }
  return false;
});
</script>
