# Contributing to NoammAddons

Thanks for your interest in improving NoammAddons. This guide covers how to set
up the project, the conventions we follow, and how to get a change merged.

## Contributor License Agreement

By submitting a contribution (a pull request, patch, or any other submission) you
agree to release it under this project's [LICENSE](LICENSE).

## Prerequisites

- **JDK 25.** JetBrains Runtime (JBR) is recommended for hotswap.

## Building / Running

- `./gradlew chiseledBuild` builds every Minecraft version.
- `./gradlew buildActive` builds only the active one.
- Jars land in `build/libs/`, one legit and one cheat jar per Minecraft version.
- Run configurations are in the `.run/` folder. (runClient and etc...)
- If you switched the active Stonecutter version, run `./gradlew "Reset active project"` before committing.

## Where does my change go?

- Version specific code with Stonecutter preprocessing goes into the `compat` package.
- **Avoid** Stonecutter's slop if or if else if preprocessing comments. use local swaps
- New features go in `features/impl/<category>/`. The category is derived from the package, so placement matters.
- New events go in the `event/impl` package.
- **Just follow the existing style** (:

## Conventions

- **Conventional Commits are required.** Prefix every commit with `feat:`, `fix:`, `refactor:`, `perf:`, `docs:`, `chore:`, `test:`, `build:`, `ci:`, `style:`, or `revert:`.
- The full shape is `<type>[(scope)][!]: <description>`, subject at most 100 characters and no trailing period.
- Run this once after cloning and a `commit-msg` hook will check the format for you:

  ```sh
  git config core.hooksPath .githooks
  ```
- **Comments explain *why*, not *what*.** Keep them self contained and in American English.

## Pull request process

1. Make your change, keep commits focused and conventionally prefixed.
2. Ensure `./gradlew chiseledBuild` passes and you tested the affected variant (cheat/legit) on the affected Minecraft version(s).
3. Open a PR and fill in the template.

Questions? Open an issue.