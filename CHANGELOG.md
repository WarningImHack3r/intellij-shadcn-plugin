<!-- Keep a Changelog guide -> https://keepachangelog.com -->
<!-- Types of changes memo:
— “Added” for new features.
— “Changed” for changes in existing functionality.
— “Deprecated” for soon-to-be removed features.
— “Removed” for now removed features.
— “Fixed” for any bug fixes.
— “Security” in case of vulnerabilities.
-->

# intellij-shadcn-plugin Changelog

## [Unreleased]

### Changed

- No longer try to remove `/* */` comments from the `tsconfig.json`/`jsconfig.json` files as they can break parsing (#66)

### Fixed

- [Vue] Fix parsing of `tsconfig.app.json` files (#66)

## [0.9.2] - 2024-09-24

### Added

- Add support for Deno 2 for installing dependencies

## [0.9.1] - 2024-09-14

### Fixed

- Fix a crash when not using TypeScript in a Svelte project (#62)

## [0.9.0] - 2024-08-01

### Added

- Add a notification when component dependencies fail to install or uninstall
- Add a dependency to the Webpack extension due to the newer Vue extension (#55)

### Removed

- Drop support for 2021.3, 2022.1 and 2022.2 IDEs (#55)

## [0.8.5] - 2024-06-22

### Fixed

- Fix additional dependencies installation not working with yarn (#51) (thanks [@shimizu-izumi](https://github.com/shimizu-izumi)!)

## [0.8.4] - 2024-06-20

### Changed

- Temporarily reduce back bundle size by removing unused dependencies

### Fixed

- Fix a crash due to an edge case with tsconfig.json files with JSON5 patterns (#48)

## [0.8.3] - 2024-06-17

### Added

- Add support for `tsconfig.json`/`jsconfig.json` files with JSON5 features (#32, #45)

## [0.8.2] - 2024-06-03

### Fixed

- Fix another regression with imports replacement, improve its overall accuracy

## [0.8.1] - 2024-05-20

### Fixed

- Fix a regression with imports replacement

## [0.8.0] - 2024-05-20

### Changed

- Entirely rewrite the Tailwind classes replacement engine to be more accurate and faster
    - Consequently, the plugin now depends on [Svelte](https://plugins.jetbrains.com/plugin/12375-svelte) & [Vue](https://plugins.jetbrains.com/plugin/9442-vue-js) extensions and only works on WebStorm or IntelliJ IDEA Ultimate
- Overhaul support for Solid as both implementations diverged from shadcn/ui
- Improve crash reporter to include more relevant information

### Fixed

- Fix a potential crash with 2024.1+ IDEs
- Fix JS users with Vue getting notified too often about the unavailability of the JS option

## [0.7.7] - 2024-03-29

### Changed

- Improve HTTP client for safer usage and better performance

### Fixed

- Fix a crash when sending a notification (#30)

## [0.7.6] - 2024-03-24

### Fixed

- Fix a crash when opening the tool window multiple times within a non-shadcn/ui project (#26)

## [0.7.5] - 2024-03-20

### Fixed

- Fix deprecated component alert always displaying
- Fix imports replacement with new shadcn-svelte formats

## [0.7.4] - 2024-03-17

### Fixed

- Fix error logging throwing crashes

## [0.7.3] - 2024-03-02

### Added

- [Svelte/Vue] Notify when an updated component no longer uses some installed files, allowing to remove them
- [Svelte] Add support for the TypeScript option
- Allow to remove installed dependencies when they are no longer used by any component

### Fixed

- Fix a freeze when executing commands on macOS/Linux
- Improve performance and stability

## [0.7.2] - 2024-02-16

### Fixed

- Fix a conflict with my other plugin, `npm-update-dependencies`

## [0.7.1] - 2024-02-11

### Added

- Add support for solid-ui.com
- Add error reporter on crash
- Add support for non-SvelteKit Svelte projects

### Changed

- Improve tool window appearance
- Improve implementation detection accuracy

## [0.7.0] - 2024-01-09

### Added

- Initial release

[Unreleased]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.9.2...HEAD
[0.9.2]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.9.1...v0.9.2
[0.9.1]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.9.0...v0.9.1
[0.9.0]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.5...v0.9.0
[0.8.5]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.4...v0.8.5
[0.8.4]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.3...v0.8.4
[0.8.3]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.2...v0.8.3
[0.8.2]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.1...v0.8.2
[0.8.1]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.8.0...v0.8.1
[0.8.0]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.7...v0.8.0
[0.7.7]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.6...v0.7.7
[0.7.6]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.5...v0.7.6
[0.7.5]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.4...v0.7.5
[0.7.4]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.3...v0.7.4
[0.7.3]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.2...v0.7.3
[0.7.2]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.1...v0.7.2
[0.7.1]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.0...v0.7.1
[0.7.0]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/commits/v0.7.0
