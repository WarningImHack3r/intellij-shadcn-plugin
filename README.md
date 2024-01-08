# intellij-shadcn-plugin

![Build](https://github.com/WarningImHack3r/intellij-shadcn-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.warningimhack3r.intellijshadcnplugin.svg)](https://plugins.jetbrains.com/plugin/com.github.warningimhack3r.intellijshadcnplugin)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.github.warningimhack3r.intellijshadcnplugin.svg)](https://plugins.jetbrains.com/plugin/com.github.warningimhack3r.intellijshadcnplugin)

## ToDo list before 1.0.0

- Rework `class`es replacement detection mechanism to be 100% accurate
  - Add tests for this
- Add support for Vue's `typescript` option (transpiling TypeScript to JavaScript in `*.vue` files)

## Description

<!-- Plugin description -->
Manage your shadcn/ui components in your project. Supports Svelte, React, Vue, and Solid.

This plugin will help you manage your shadcn/ui components through a simple tool window. Add, remove, update them with a single click.  
**This plugin will only work with an existing `components.json` file. Manually copied components will not be detected otherwise.**

## Features

- Automatically detect shadcn/ui components in your project
- Instantly add, remove, update them with a single click
- Refreshes on opening the tool window
- Supports _all_ shadcn/ui implementations: Svelte, React, Vue, and Solid
- Browse available components
- Easily search for remote or existing components
- (Soon) support monorepos
- ...and more!

## Usage

Simply open the `shadcn/ui` tool window and start managing your components.  
If you don't see the tool window, you can open it from `View > Tool Windows > shadcn/ui`.  
**When adding or removing components, the tool window won't refresh automatically yet. You can refresh it by closing and reopening it.**

## Planned Features

- Parse `vite.config.(js|ts)` (and others like `nuxt.config.ts`) to resolve aliases as a fallback of `tsconfig.json`
- Figure out a clean way to refresh the tool window automatically after adding or removing components
- Refresh/recreate the tool window automatically when the project finishes indexing
- Add support for monorepos
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-shadcn-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/WarningImHack3r/intellij-shadcn-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
