# intellij-shadcn-plugin

![Build](https://github.com/WarningImHack3r/intellij-shadcn-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.warningimhack3r.intellijshadcnplugin.svg)](https://plugins.jetbrains.com/plugin/com.github.warningimhack3r.intellijshadcnplugin)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.github.warningimhack3r.intellijshadcnplugin.svg)](https://plugins.jetbrains.com/plugin/com.github.warningimhack3r.intellijshadcnplugin)

## Template ToDo list
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).

## Description
<!-- Plugin description -->
Manage your shadcn/ui components in your project.

This plugin will help you manage your shadcn/ui components through a simple tool window. Add, remove, update them with a single click.

## Features
- Automatically detect shadcn/ui components in your project
- Instantly add, remove, update them with a single click
- Refreshes on opening the tool window
- Supports _all_ shadcn/ui implementations: Svelte, React, Vue, Solid, and even Kotlin/JS
- Browse available components
- Easily search for remote or existing components
- (Soon) support monorepos
- ...and more!

## Usage
Simply open the `shadcn/ui` tool window and start managing your components.  
If you don't see the tool window, you can open it from `View > Tool Windows > shadcn/ui`.

## Planned Features
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
