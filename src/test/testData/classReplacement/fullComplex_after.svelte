<script lang="ts">
    import { Accordion as AccordionPrimitive } from "bits-ui";
    import ChevronDown from "lucide-svelte/icons/chevron-down";
    import { cn } from "$lib/utils.js";

    type $$Props = AccordionPrimitive.TriggerProps;
    type $$Events = AccordionPrimitive.TriggerEvents;

    let className: $$Props["class"] = undefined;
    export let level: AccordionPrimitive.HeaderProps["level"] = 3;
    export { className as class };
</script>

<AccordionPrimitive.Header {level} class="flex">
    <AccordionPrimitive.Trigger
        class={cn(
            "a-flex a-flex-1 a-items-center a-justify-between a-py-4 a-font-medium a-transition-all a-hover:underline a-[&[data-state=open]>svg]:rotate-180",
            className
        )}
        {...$$restProps}
        on:click
    >
        <slot />
        <ChevronDown class="a-h-4 a-w-4 a-transition-transform a-duration-200" />
    </AccordionPrimitive.Trigger>
</AccordionPrimitive.Header>
