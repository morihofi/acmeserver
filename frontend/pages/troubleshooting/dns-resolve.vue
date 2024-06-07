<template>
  <div class="container">
    <h1>DNS Resolve</h1>
    <p>
      On this page you can perform DNS lookups to check whether ACME Server can
      reach the desired domains.
    </p>
    <form @submit.prevent="resolveDNS" v-if="!isLoading">
      <div class="row g-3">
        <div class="col-md-6">
          <label for="domain" class="form-label">Domain</label>
          <input type="text" class="form-control" id="domain" v-model="dnsName" placeholder="Enter domain" required />
        </div>
        <div class="col-md-6">
          <label for="dnsType" class="form-label">DNS Type</label>
          <select class="form-select" id="dnsType" v-model="type" required>
            <option v-for="dnsType in dnsTypes" :key="dnsType" :value="dnsType">
              {{ dnsType }}
            </option>
          </select>
        </div>
      </div>
      <button type="submit" class="btn btn-primary mt-3">Resolve</button>
    </form>

    <div class="d-flex justify-content-center" v-if="isLoading">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Looking up...</span>
      </div>
    </div>

    <div v-if="response && !isLoading" class="mt-4">
      <h2>Response</h2>
      <h3>Resolved by configured DNS Servers (UDP)</h3>

      <table class="table">
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Data</th>
            <th scope="col">TTL</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in response['resolved-dns']" :key="record.name">
            <td>{{ record.name }}</td>
            <td>{{ record.data }}</td>
            <td>{{ record.ttl }}</td>
          </tr>
        </tbody>
      </table>

      <h3>Resolved by DNS over HTTPS Server</h3>

      <table class="table">
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Data</th>
            <th scope="col">TTL</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in response['resolved-doh']" :key="record.name">
            <td>{{ record.name }}</td>
            <td>{{ record.data }}</td>
            <td>{{ record.ttl }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="error" class="mt-4 alert alert-danger">
      <h2>Error</h2>
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from "vue";

interface DNSResponse {
  "resolved-doh": DNSRecord[];
  "resolved-dns": DNSRecord[];
}

interface DNSRecord {
  name: string;
  data: string;
  ttl: number;
}

export default defineComponent({
  name: "DNSResolve",
  setup() {
    const dnsName = ref<string>("");
    const type = ref<string>("A");
    const response = ref<DNSResponse | null>(null);
    const error = ref<string | null>(null);
    const isLoading = ref<boolean>(false);

    const dnsTypes: string[] = [
      "A",
      "AAAA",
      "ALL",
      "CAA",
      "CDNSKEY",
      "CDS",
      "CERT",
      "CNAME",
      "DNAME",
      "DNSKEY",
      "DS",
      "HINFO",
      "HTTPS",
      "INTEGRITY",
      "IPSECKEY",
      "KEY",
      "MX",
      "NAPTR",
      "NS",
      "NSEC",
      "NSEC3",
      "NSEC3PARAM",
      "PTR",
      "RP",
      "RRSIG",
      "SIG",
      "SOA",
      "SPF",
      "SRV",
      "SSHFP",
      "SVCB",
      "TLSA",
      "TXT",
      "WKS",
    ];

    const resolveDNS = async () => {
      error.value = null;
      isLoading.value = true;
      try {
        const res = await fetch(
          `${getApiBase()}/api/troubleshooting/dns-resolver`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              accept: "application/json",
            },
            body: JSON.stringify({ type: type.value, dnsName: dnsName.value }),
          }
        );
        if (!res.ok) {
          throw new Error("Network response was not ok");
        }
        response.value = await res.json();
      } catch (err) {
        if (err instanceof Error) {
          error.value = "There has been a problem with your fetch operation: " + err.message;
          console.error("There has been a problem with your fetch operation:", err);
        } else {
          error.value = "An unknown error occurred.";
          console.error("An unknown error occurred:", err);
        }
      }
      isLoading.value = false;
    };

    return { dnsName, type, dnsTypes, response, error, resolveDNS, isLoading};
  },
});
</script>
