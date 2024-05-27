
// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  build: {
    transpile: ['bootstrap']
  }, 
  vite: {
    define: {
      'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV)
    }
  },
  plugins: ["~/plugins/fontawesome.ts"],
  css: ["bootstrap/dist/css/bootstrap.min.css", "~/assets/css/main.css"],
  runtimeConfig: {
		public: {
			API_URL: '' // defined in .env
		}
	},
  app: {
    head: {
      charset: "utf-8",
      viewport: "width=device-width, initial-scale=1",
      title: "ACME Server"
    },
  },
});