<script setup lang="ts">
import { computed, type HTMLAttributes } from 'vue'
import {
  AlertDialogContent,
  type AlertDialogContentEmits,
  type AlertDialogContentProps,
  AlertDialogOverlay,
  AlertDialogPortal,
  useForwardPropsEmits,
} from 'radix-vue'
import { cn } from '@/lib/utils'

const props = defineProps<AlertDialogContentProps & { class?: HTMLAttributes['class'] }>()
const emits = defineEmits<AlertDialogContentEmits>()

const delegatedProps = computed(() => {
  const {class: _, ...delegated} = props

  return delegated
})

const forwarded = useForwardPropsEmits(delegatedProps, emits)
</script>

<template>
  <AlertDialogPortal>
    <AlertDialogOverlay
        class="a-fixed a-inset-0 a-z-50 a-bg-black/80 a-data-[state=open]:animate-in a-data-[state=closed]:animate-out a-data-[state=closed]:fade-out-0 a-data-[state=open]:fade-in-0"
    />
    <AlertDialogContent
        v-bind="forwarded"
        :class="
          cn(
            'a-fixed a-left-1/2 a-top-1/2 a-z-50 a-grid a-w-full a-max-w-lg a--translate-x-1/2 a--translate-y-1/2 a-gap-4 a-border a-bg-background a-p-6 a-shadow-lg a-duration-200 a-data-[state=open]:animate-in a-data-[state=closed]:animate-out a-data-[state=closed]:fade-out-0 a-data-[state=open]:fade-in-0 a-data-[state=closed]:zoom-out-95 a-data-[state=open]:zoom-in-95 a-data-[state=closed]:slide-out-to-left-1/2 a-data-[state=closed]:slide-out-to-top-[48%] a-data-[state=open]:slide-in-from-left-1/2 a-data-[state=open]:slide-in-from-top-[48%] a-sm:rounded-lg',
            props.class,
          )
        "
    >
      <slot/>
    </AlertDialogContent>
  </AlertDialogPortal>
</template>
