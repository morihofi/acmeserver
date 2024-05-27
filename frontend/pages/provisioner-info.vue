<template>
    <h1>Datails of Provisioner {{ provisionerName }}</h1>

    <div class="table-responsive" v-if="provisionerByNameInfoResponse">

        <h2>General information</h2>
        <table class="table table-striped table-hover">
            <thead>
                <tr>
                    <th>Key</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>
                        Website
                    </td>
                    <td>
                        <a :href="provisionerByNameInfoResponse.website">
                            {{ provisionerByNameInfoResponse.website }}
                        </a>
                    </td>
                </tr>
                <tr>
                    <td>
                        Terms of Service
                    </td>
                    <td>
                        <a :href="provisionerByNameInfoResponse['terms-of-service']">
                            {{
        provisionerByNameInfoResponse['terms-of-service']
    }}
                        </a>
                    </td>
                </tr>
            </tbody>
        </table>

        <h2>Provisioner restrictions</h2>

        <div class="container">
            <div class="row">
                <!-- Allow IP Addresses -->
                <div class="col-sm mb-3">
                    <div class="card text-center">
                        <div class="card-body">
                            <div v-if="provisionerByNameInfoResponse['allow-ip']">
                                <font-awesome-icon icon="fa-solid fa-check" class="text-success icon-fixed-size" />
                            </div>
                            <div v-else>
                                <font-awesome-icon icon="fa-solid fa-xmark" class="text-danger icon-fixed-size" />
                            </div>
                            <p class="card-text mt-3">Allow IP Addresses</p>
                        </div>
                    </div>
                </div>
                <!-- Allow Wildcard -->
                <div class="col-sm mb-3">
                    <div class="card text-center">
                        <div class="card-body">
                            <div v-if="provisionerByNameInfoResponse['allow-dns-wildcards']">
                                <font-awesome-icon icon="fa-solid fa-check" class="text-success icon-fixed-size" />
                            </div>
                            <div v-else>
                                <font-awesome-icon icon="fa-solid fa-xmark" class="text-danger icon-fixed-size" />
                            </div>
                            <p class="card-text mt-3">Allow Wildcard</p>
                        </div>
                    </div>
                </div>
                <!-- End cards -->
            </div>
        </div>


    </div>

</template>
<script setup lang="ts">
import { useRoute } from 'vue-router';
import { ApiProvisionersByNameInfoResponse } from "@/types/api";

const route = useRoute();
const provisionerName = route.query.name;

const provisionerByNameInfoResponse = ref<ApiProvisionersByNameInfoResponse>();

onMounted(async () => {
    try {
        const responseInfo = await fetch(getApiBase() + `/api/provisioner/by-name/${provisionerName}/info`);
        provisionerByNameInfoResponse.value = await responseInfo.json();
    } catch (error) {
        console.error("Error fetching server info:", error);
    }
});

</script>
<style scoped>
.icon-fixed-size {
    height: 2rem;
    width: 2rem;
}
</style>