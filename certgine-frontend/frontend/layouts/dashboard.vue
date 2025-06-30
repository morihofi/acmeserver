<template>
  <div class="bg-slate-100 w-screen h-screen flex">
    <LayoutDashboardSidebar />

    <div class="flex-1 h-full flex flex-col overflow-hidden">
      <LayoutDashboardHeader />
      <main class="flex-1 overflow-y-auto p-4">
        <NuxtPage />
      </main>
    </div>
  </div>
</template>
