import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const buttonVariants = cva(
    "a-inline-flex a-items-center a-justify-center a-whitespace-nowrap a-rounded-md a-text-sm a-font-medium a-ring-offset-background a-transition-colors a-focus-visible:outline-none a-focus-visible:ring-2 a-focus-visible:ring-ring a-focus-visible:ring-offset-2 a-disabled:pointer-events-none a-disabled:opacity-50",
    {
        variants: {
            variant: {
                default: "a-bg-primary a-text-primary-foreground a-hover:bg-primary/90",
                destructive:
                    "a-bg-destructive a-text-destructive-foreground a-hover:bg-destructive/90",
                outline:
                    "a-border a-border-input a-bg-background a-hover:bg-accent a-hover:text-accent-foreground",
                secondary:
                    "a-bg-secondary a-text-secondary-foreground a-hover:bg-secondary/80",
                ghost: "a-hover:bg-accent a-hover:text-accent-foreground",
                link: "a-text-primary a-underline-offset-4 a-hover:underline",
            },
            size: {
                default: "a-h-10 a-px-4 a-py-2",
                sm: "a-h-9 a-rounded-md a-px-3",
                lg: "a-h-11 a-rounded-md a-px-8",
                icon: "a-h-10 a-w-10",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
        },
    }
)

export interface ButtonProps
    extends React.ButtonHTMLAttributes<HTMLButtonElement>,
        VariantProps<typeof buttonVariants> {
    asChild?: boolean
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
    ({className, variant, size, asChild = false, ...props}, ref) => {
        const Comp = asChild ? Slot : "button"
        return (
            <Comp
                className={cn(buttonVariants({variant, size, className}))}
                ref={ref}
                {...props}
            />
        )
    }
)
Button.displayName = "Button"

export {Button, buttonVariants}
