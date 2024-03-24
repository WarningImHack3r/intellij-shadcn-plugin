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

[Unreleased]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.5...HEAD
[0.7.5]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.4...v0.7.5
[0.7.4]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.3...v0.7.4
[0.7.3]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/compare/v0.7.2...v0.7.3
[0.7.2]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/commits/v0.7.1...v0.7.2
[0.7.1]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/commits/v0.7.0...v0.7.1
[0.7.0]: https://github.com/WarningImHack3r/intellij-shadcn-plugin/commits/v0.7.0
