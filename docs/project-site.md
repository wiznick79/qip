# Permanent project site

QIP's project tour lives in `site/`. It is static HTML/CSS, separate from the authenticated React
application and runtime deployment. It needs no database, Ollama, API credentials, analytics,
external fonts, CDN resources, or JavaScript. Native disclosure controls explain the retrieval pipeline.

The overview, workflow, architecture, real synthetic-demo screenshot, and setup links remain usable
when a hosted QIP application is offline. The illustrative case card is explicitly labeled; it is not
presented as a live inference result. No live-demo URL or availability claim is currently configured.

## Build and preview

With Node 24 installed, from the repository root:

```shell
npm --prefix site run verify
python -m http.server 28081 --bind 127.0.0.1 --directory target/project-site
```

Open `http://localhost:28081/`. Alternatively, open `target/project-site/index.html` directly.
There are no npm dependencies to install. Build output is ignored under `target/`; do not edit it.
The build copies the existing screenshot from `docs/images/`, avoiding a second maintained copy.
The earlier standalone `docs/qip-architecture.html` export is independent and is not bundled.

## GitHub Pages publication

After reviewing and merging the site, enable **Settings → Pages → Build and deployment → GitHub Actions**
in the repository. Run the **Project site** workflow on `main` if it has not already run.
The expected URL is `https://wiznick79.github.io/qip/`; publication is a separate step.

The workflow verifies relevant pull requests and deploys only from `main` on relevant pushes or manual
dispatch. Only the deployment job receives Pages and OIDC write permissions. Its artifact is limited
to `target/project-site`, never the whole repository, document storage, or local configuration.
Actions are pinned to SHAs and covered by the existing Dependabot Actions group.

The site does not deploy, start, stop, or probe the QIP application. Adding a live-demo link later should
include an explicit availability policy rather than assuming the application is always running.

## Maintenance

- Edit `site/index.html` for copy, diagrams, and links; `site/styles.css` for presentation.
- Keep architecture claims consistent with implemented behavior and the architecture document.
- Use synthetic screenshots only. Preserve their caption and text alternative.
- Run `npm --prefix site run verify`; inspect desktop and narrow mobile layouts before publication.
- Tests check relative assets, section links, artifact scope, and independence from a live backend.
- Preview at a nested path as well as `/`: GitHub Pages hosts the site under `/qip/`.
