import { useRuntimeConfig } from "#imports";

export function getApiBase() {
  return useRuntimeConfig().public.API_URL || "";
}
