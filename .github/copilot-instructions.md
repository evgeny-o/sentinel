# Developer Agent — Custom Instructions

## Version Control & Git (STRICT — ZERO EXCEPTIONS)

- **NEVER run `git commit`.** **NEVER run `git push`.** Committing and pushing are performed **exclusively by the user** — no agent may ever do this, under any circumstances, even if the user's task description implies it or a previous step seems to expect it.
- Also never run any other history- or remote-mutating git command on the user's behalf: `git merge`, `git rebase`, `git reset --hard`, `git cherry-pick`, `git tag`, `git push --force`, `git commit --amend`, `git stash drop`, or configuring/adding remotes.
- You **may** run read-only / inspection git commands to do your work: `git status`, `git diff`, `git log`, `git show`, `git blame`, `git branch --list`. Staging with `git add` is only allowed when the user explicitly asks you to stage — otherwise leave the working tree untouched and let the user stage and commit.
- When your work is complete, **stop at the file changes**. Summarise what changed and explicitly tell the user the changes are ready for them to review and commit. Do not offer to commit.
- If you believe a commit is warranted, **ask the user to do it** — never do it yourself.

## Plans & Documentation

- All plans, analyses, suggestions, migration guides, and architectural decisions produced during a chat **must be saved as `.md` files** in `.github/plans/`.
- Use the format **`YYYY-MM-DD-<kebab-case-topic>.md`** — always prefix the filename with the current ISO date (e.g. `2026-06-04-vite8-upgrade-plan.md`, `2026-06-04-refactor-safety-analysis.md`).
- Include a `**Date:**` and `**Status:**` header in each file.
- Never only show a plan inline in chat — always write it to `.github/plans/` as well.
- Before creating a new plan, check `.github/plans/` for an existing plan covering the same task or decision. If one exists, update that current plan instead of creating a second plan file, and add/update a revision note when the plan format includes revision history.
- All temporary files (command output, diffs, etc.) must go in `.github/temp/` — never in the project root.
- **Never create new folders directly in the project root.** All new directories must be created inside an appropriate existing subtree (`../src`, `docs/`, `.github/`, `packages/`, etc.). If no suitable parent exists, ask the user where to put it.
- **Always clean up temporary files and extracted artefacts after use.** If a command downloads, unpacks, or generates temporary files (e.g. extracted `.tgz` contents, intermediate build outputs), delete them when they are no longer needed and ensure the relevant paths are listed in `../.gitignore`.
- **Never use `echo` or shell redirection (`>`) to create source files in cmd.exe.** These commands misinterpret the file content as additional shell tokens and create garbage files (e.g. `({`, `handleBwList(null`) in the project root. Always use the `create_file` tool to create new files.
- Before implementing a new helper, utility, endpoint wrapper, component, or feature behavior, **search for existing functionality and a suitable existing file first**. Prefer extending the existing implementation or file over creating a parallel utility or new file. If no suitable existing location exists, **ask the user before creating any new repository source, test, configuration, or documentation file**. A plan file explicitly requested by the user is the only exception.
- **Do not change the structures in `src/app/models/common.ts` or `src/app/models/server.ts` unless the user explicitly asks for those structure changes.** If work in those files appears necessary for a task, stop and ask for explicit confirmation or limit the change to non-structural usage only.
- **Do not change `src/app/models/server.ts` without a direct user request naming that file or explicitly requesting server DTO structure changes.** Server-side field names are backend contracts and must not be renamed during client-side refactors.

## Code Style & Patterns

### Component Folder Structure
- **Every new React component must live in its own dedicated folder** named after the component (PascalCase).
- The folder must contain at minimum:
  - `ComponentName.tsx` — the component implementation
  - `ComponentName.module.scss` — co-located styles (if any)
  - `index.ts` — barrel that re-exports the public API: `export { ComponentName } from './ComponentName';`
- Each feature folder (e.g. `BlackWhite/`) should have its own `index.ts` barrel exporting all public components within it.
- Example layout:
  ```
  BlackWhite/
    index.ts                          ← feature barrel
    BlackWhiteModal/
      BlackWhiteModal.tsx
      BlackWhiteModal.module.scss
      index.ts
    BlackWhiteView/
      BlackWhiteView.tsx
      BlackWhiteView.module.scss
      index.ts
    DroneHistoryTable/
      DroneHistoryTable.tsx
      DroneHistoryTable.module.scss
      index.ts
  ```

### TypeScript
- Always use `const` over `let` where possible.
- Prefer explicit types; avoid `any`.
- Use `interface` for object shapes, `type` for unions/intersections.
- Prefix interface names with `I`.
- Use optional chaining (`?.`) and nullish coalescing (`??`).
- Use single quotes (`'`) for strings, **including JSX attribute values** — never use double quotes in JSX attributes.

### Accessibility (A11y)
- **Do not add any `aria-*` attributes in new code.** This covers `aria-label`, `aria-hidden`, `aria-live`, `aria-pressed`, `aria-expanded`, `aria-haspopup`, `aria-controls`, `aria-activedescendant`, `aria-multiselectable`, and all others.
- **Do not remove existing `aria-*` attributes** from code you are not otherwise rewriting. Existing usages are load-bearing for test selectors and potential compliance requirements — leave them in place.
- **When writing new components or fully rewriting existing ones,** express widget state (expanded, pressed, selected, disabled, etc.) via CSS classes or `data-*` attributes instead of `aria-*`.
- `role` attributes are **permitted in both new and existing code** — they provide semantic structure for devtools, automated testing, and `@testing-library/react` selectors.

### Styling with MUI Components
- **Always prefer `.module.scss` over MUI's `sx` prop** for any styling that can be expressed in CSS.
- Use `:global(.MuiXxx)` selectors inside a CSS Module class to target MUI internal elements that live inside the component's own DOM tree.
- `sx={{}}` is **only permitted** when there is genuinely no SCSS alternative — specifically when MUI renders content into a React portal (e.g. `MenuProps.PaperProps.sx`, `PopoverProps`, `TooltipProps`). In that case add a short comment explaining why.
- **Never use `sx` for layout or spacing** on elements you own — always use the SCSS module.

### Logging
- Always use the module-scoped logger pattern:
  ```ts
  const log = logger.child({ module: 'ModuleName' });
  log.debug / log.warn / log.error(...)
  ```

### Testing
- Co-locate test files: `foo.ts` → `foo.test.ts` in the same directory.
- **Exception:** For large folders with many source files (e.g. `src/app/hooks/`), group tests in a
  `__tests__/` subdirectory within that folder instead of scattering them alongside each source file.
  Once a folder adopts `__tests__/`, all new tests for that folder must follow the same pattern.
- **Do not add unit tests for purely visual / presentational components.** A component is considered
  purely presentational when it: receives props and renders markup with no internal logic beyond
  conditional CSS classes or straight data interpolation, contains no event handlers that call APIs or
  update shared state, and has no business-rule branching worth guarding with assertions.
  Writing tests for such components yields low signal (they duplicate what the component trivially
  does), slows down iteration, and breaks on cosmetic refactors. Focus unit tests on:
  - **Hooks and utilities** — any function with non-trivial logic or side-effects.
  - **Components with behaviour** — visibility gating driven by feature flags or store state, API
    calls triggered by user interaction, error/loading state transitions, and conditional rendering
    controlled by business rules.
  - If you are unsure whether a component qualifies as "purely presentational", the default is **no
    test** — only add one when there is a concrete behaviour to assert.

### Internationalisation (i18n)
- All user-facing strings must use `t()` from `react-i18next` — never hardcode display text.
- When adding or changing any key in `src/app/i18n/en.json`, you **must** add the same key to **every other locale file** in the same directory (`he.json`, `de.json`, and any future locale). Missing keys in any locale are a blocking error.
- Translations must be genuine (not copy-pasted English). If uncertain, provide a best-effort translation and note it for review.
