<template>
	<component :is="IconAsyncComponent" v-bind="$attrs" />
</template>

<script lang="ts" setup>
import type { icons } from 'lucide-vue-next';

const props = defineProps({
	icon: {
		type: String as PropType<keyof typeof icons>,
		required: true
	}
});

const IconAsyncComponent = computed(
	() =>
		props.icon &&
		defineAsyncComponent({
			loader: () => import('lucide-vue-next').then((module) => module[props.icon])
		})
);
</script>
