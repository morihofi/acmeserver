<template>
	<div :class="wrapperClass">
		<label v-if="label" :for="id" :class="labelClasses">{{ label }}</label>

		<div :class="cn('relative isolate rounded-md shadow', !!label && 'mt-1', innerWrapperClass)">
			<input :id="id" v-model="value" :class="inputClasses" v-bind="$attrs" :readonly="props.readonly" :disabled="disabled" />

			<ComponentLucideIcon
				v-if="iconLeft"
				:icon="iconLeft"
				:class="cn(iconClasses, 'left-0', iconLeftClickable && 'pointer-events-auto cursor-pointer', iconLeftClass)"
				@click="emit('onLeftIconClick')"
			/>

			<ComponentLucideIcon
				v-if="iconRight"
				:icon="iconRight"
				:class="cn(iconClasses, 'right-0', iconRightClickable && 'pointer-events-auto cursor-pointer', iconRightClass)"
				@click="emit('onRightIconClick')"
			/>
		</div>
	</div>
</template>

<script lang="ts" setup>
import type { icons } from 'lucide-vue-next';
import type { PropType } from 'vue';
import { cn } from '~/composables/cn';

defineOptions({
	inheritAttrs: false
});

const props = defineProps({
	id: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	variant: {
		type: String as PropType<'primary' | 'secondary' | 'info' | 'success' | 'warning' | 'danger'>,
		default: 'primary'
	},
	size: {
		type: String as PropType<'small' | 'normal' | 'large'>,
		default: 'normal'
	},
	iconLeft: {
		type: String as PropType<keyof typeof icons | undefined>,
		default: undefined
	},
	iconLeftClass: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	iconLeftClickable: {
		type: Boolean,
		default: false
	},
	iconRight: {
		type: String as PropType<keyof typeof icons | undefined>,
		default: undefined
	},
	iconRightClass: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	iconRightClickable: {
		type: Boolean,
		default: false
	},
	label: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	labelClass: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	required: {
		type: Boolean,
		default: false
	},
	readonly: {
		type: Boolean,
		default: false
	},
	disabled: {
		type: Boolean,
		default: false
	},
	wrapperClass: {
		type: String as PropType<string | undefined>,
		default: undefined
	},
	innerWrapperClass: {
		type: String as PropType<string | undefined>,
		default: undefined
	}
});

const value = defineModel({ type: String, default: '' });

const emit = defineEmits<{
	onLeftIconClick: [];
	onRightIconClick: [];
}>();

const id = computed(() => {
	return props.id ?? useId();
});

const labelClasses = computed(() => {
	return cn(
		// default classes
		'px-1 font-medium text-gray-900 dark:text-white',

		// sizes
		{
			'text-sm': props.size === 'small' || props.size === 'normal',
			'text-base': props.size === 'large'
		},

		// required star
		props.required && 'after:ml-0.5 after:text-red-500 after:content-["*"] after:dark:text-red-400',

		// custom classes
		props.labelClass
	);
});

const inputClasses = computed(() => {
	const rootClass = useAttrs().class;

	return cn(
		// default classes
		'peer w-full rounded-md border-0 bg-white ring-1 ring-inset ring-gray-500 placeholder:italic read-only:cursor-not-allowed read-only:opacity-75 focus:ring-2 disabled:cursor-not-allowed disabled:opacity-50 dark:bg-gray-700 dark:text-white',

		// sizes
		{
			'px-2.5 py-2 text-sm': props.size === 'small',
			'px-3 py-2.5 text-sm': props.size === 'normal',
			'px-3.5 py-3 text-base': props.size === 'large'
		},

		// icon space
		{
			'pl-8': !!props.iconLeft && props.size === 'small',
			'pl-9': !!props.iconLeft && props.size === 'normal',
			'pl-11': !!props.iconLeft && props.size === 'large',

			'pr-8': !!props.iconRight && props.size === 'small',
			'pr-9': !!props.iconRight && props.size === 'normal',
			'pr-11': !!props.iconRight && props.size === 'large'
		},

		// variants
		{
			'focus:ring-blue-600 dark:focus:ring-blue-600': props.variant === 'primary',
			'focus:ring-slate-500 dark:focus:ring-gray-400': props.variant === 'secondary',
			'focus:ring-sky-600 dark:focus:ring-sky-600': props.variant === 'info',
			'focus:ring-green-600 dark:focus:ring-green-600': props.variant === 'success',
			'focus:ring-amber-500 dark:focus:ring-amber-600': props.variant === 'warning',
			'focus:ring-red-600 dark:focus:ring-red-600': props.variant === 'danger'
		},

		// class on component
		typeof rootClass === 'string' && rootClass
	);
});

const iconClasses = computed(() => {
	return cn(
		// defaults
		'pointer-events-none absolute top-1/2 z-10 -translate-y-1/2 peer-read-only:cursor-not-allowed peer-read-only:opacity-75 peer-disabled:cursor-not-allowed peer-disabled:opacity-75 dark:text-white',

		// sizes
		{
			'mx-2 size-4': props.size === 'small',
			'mx-2.5 size-5': props.size === 'normal',
			'mx-3 size-6': props.size === 'large'
		}
	);
});
</script>
