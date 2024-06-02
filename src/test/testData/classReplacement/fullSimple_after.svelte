<script lang="ts">
    import { Avatar as AvatarPrimitive } from "bits-ui";
    import { cn } from "$lib/utils.js";

    type $$Props = AvatarPrimitive.Props;

    let className: $$Props["class"] = undefined;
    export let delayMs: $$Props["delayMs"] = undefined;
    export { className as class };
</script>

<AvatarPrimitive.Root
    {delayMs}
    class={cn("a-relative a-flex a-h-10 a-w-10 a-shrink-0 a-overflow-hidden a-rounded-full", className)}
    {...$$restProps}
>
    <slot />
</AvatarPrimitive.Root>
