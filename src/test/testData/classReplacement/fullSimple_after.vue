<script setup lang="ts">
import type { HTMLAttributes } from 'vue'
import { cn } from '@/lib/utils'

const props = defineProps<{
  class?: HTMLAttributes['class']
}>()
</script>

<template>
  <h5 :class="cn('a-mb-1 a-font-medium a-leading-none a-tracking-tight', props.class)">
    <slot/>
  </h5>
</template>
