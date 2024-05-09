"use client"

import * as React from "react"
import * as AlertDialogPrimitive from "@radix-ui/react-alert-dialog"

import {cn} from "@/lib/utils"
import {buttonVariants} from "@/registry/default/ui/button"

const AlertDialog = AlertDialogPrimitive.Root

const AlertDialogTrigger = AlertDialogPrimitive.Trigger

const AlertDialogPortal = AlertDialogPrimitive.Portal

const AlertDialogOverlay = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Overlay>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Overlay>
>(({className, ...props}, ref) => (
    <AlertDialogPrimitive.Overlay
        className={cn(
            "a-fixed a-inset-0 a-z-50 a-bg-black/80 a-data-[state=open]:animate-in a-data-[state=closed]:animate-out a-data-[state=closed]:fade-out-0 a-data-[state=open]:fade-in-0",
            className
        )}
        {...props}
        ref={ref}
    />
))
AlertDialogOverlay.displayName = AlertDialogPrimitive.Overlay.displayName

const AlertDialogContent = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Content>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Content>
>(({className, ...props}, ref) => (
    <AlertDialogPortal>
        <AlertDialogOverlay/>
        <AlertDialogPrimitive.Content
            ref={ref}
            className={cn(
                "a-fixed a-left-[50%] a-top-[50%] a-z-50 a-grid a-w-full a-max-w-lg a-translate-x-[-50%] a-translate-y-[-50%] a-gap-4 a-border a-bg-background a-p-6 a-shadow-lg a-duration-200 a-data-[state=open]:animate-in a-data-[state=closed]:animate-out a-data-[state=closed]:fade-out-0 a-data-[state=open]:fade-in-0 a-data-[state=closed]:zoom-out-95 a-data-[state=open]:zoom-in-95 a-data-[state=closed]:slide-out-to-left-1/2 a-data-[state=closed]:slide-out-to-top-[48%] a-data-[state=open]:slide-in-from-left-1/2 a-data-[state=open]:slide-in-from-top-[48%] a-sm:rounded-lg",
                className
            )}
            {...props}
        />
    </AlertDialogPortal>
))
AlertDialogContent.displayName = AlertDialogPrimitive.Content.displayName

const AlertDialogHeader = ({
                               className,
                               ...props
                           }: React.HTMLAttributes<HTMLDivElement>) => (
    <div
        className={cn(
            "a-flex a-flex-col a-space-y-2 a-text-center a-sm:text-left",
            className
        )}
        {...props}
    />
)
AlertDialogHeader.displayName = "AlertDialogHeader"

const AlertDialogFooter = ({
                               className,
                               ...props
                           }: React.HTMLAttributes<HTMLDivElement>) => (
    <div
        className={cn(
            "a-flex a-flex-col-reverse a-sm:flex-row a-sm:justify-end a-sm:space-x-2",
            className
        )}
        {...props}
    />
)
AlertDialogFooter.displayName = "AlertDialogFooter"

const AlertDialogTitle = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Title>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Title>
>(({className, ...props}, ref) => (
    <AlertDialogPrimitive.Title
        ref={ref}
        className={cn("a-text-lg a-font-semibold", className)}
        {...props}
    />
))
AlertDialogTitle.displayName = AlertDialogPrimitive.Title.displayName

const AlertDialogDescription = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Description>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Description>
>(({className, ...props}, ref) => (
    <AlertDialogPrimitive.Description
        ref={ref}
        className={cn("a-text-sm a-text-muted-foreground", className)}
        {...props}
    />
))
AlertDialogDescription.displayName =
    AlertDialogPrimitive.Description.displayName

const AlertDialogAction = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Action>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Action>
>(({className, ...props}, ref) => (
    <AlertDialogPrimitive.Action
        ref={ref}
        className={cn(buttonVariants(), className)}
        {...props}
    />
))
AlertDialogAction.displayName = AlertDialogPrimitive.Action.displayName

const AlertDialogCancel = React.forwardRef<
    React.ElementRef<typeof AlertDialogPrimitive.Cancel>,
    React.ComponentPropsWithoutRef<typeof AlertDialogPrimitive.Cancel>
>(({className, ...props}, ref) => (
    <AlertDialogPrimitive.Cancel
        ref={ref}
        className={cn(
            buttonVariants({variant: "outline"}),
            "a-mt-2 a-sm:mt-0",
            className
        )}
        {...props}
    />
))
AlertDialogCancel.displayName = AlertDialogPrimitive.Cancel.displayName

export {
    AlertDialog,
    AlertDialogPortal,
    AlertDialogOverlay,
    AlertDialogTrigger,
    AlertDialogContent,
    AlertDialogHeader,
    AlertDialogFooter,
    AlertDialogTitle,
    AlertDialogDescription,
    AlertDialogAction,
    AlertDialogCancel,
}
