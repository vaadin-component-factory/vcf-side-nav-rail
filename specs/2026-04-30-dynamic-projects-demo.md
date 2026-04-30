# Dynamic projects demo view

**Status:** Draft — design only, not implemented yet
**Date:** 2026-04-30

A new dedicated demo view that exercises a runtime-mutating `SideNavRail` driven by parameterized routes. It is the "Variant a" that came out of the brainstorm round on 2026-04-30: a fixed set of route templates with a `:projectId` parameter, plus a session-scope "active projects" registry that drives both the sidenav structure and a `BeforeEnter` access guard. Navigating to a non-active project's URL is forbidden by the guard.

## Goal

Demonstrate, in one self-contained demo route, the following addon and Flow features working together:

- Runtime addition / removal of `SideNavRailItem` children via the `addItem` / `removeAll` / `remove` API the addon exposes (already covered by unit/E2E tests; this view shows it driven by real user input).
- A dynamic per-session activation model where toggling a checkbox-like control adds or removes a parent rail item plus its sub-items.
- `getActiveViewItem()` lighting up the right item even when the active route resolved a `:projectId` parameter.
- A typical "navigation gate" pattern (`BeforeEnter` redirect) that's independent of, but consistent with, the rail's visible item list.

## Out of scope

- Per-project route registration via `RouteConfiguration#setRoute(...)` — that is the "Variant b" road, not pursued here.
- Persistence of the active set across sessions / browser tabs — session-scoped only.
- Authentication / authorisation. The guard in this demo is "is this project ID currently active?", not a real access check.
- Demonstrating route aliases on the project sub-views.

## Routes and layout

A new top-level layout, `DynamicProjectsLayout`, hosts the demo. It carries the rail and the navbar, similar to `MainLayout`, but lives at the URL prefix `dynamic-projects`. The layout uses `@Layout("dynamic-projects")` so that any `@Route` whose path starts with `dynamic-projects/...` auto-attaches to it without needing explicit `layout = ...` parameters. Routes outside that prefix continue to flow through `MainLayout`.

| Path                                                     | View                       | Purpose                                                            |
| -------------------------------------------------------- | -------------------------- | ------------------------------------------------------------------ |
| `dynamic-projects`                                       | `DynamicProjectsHomeView`  | Welcome / instructions: "Activate projects via the multiselect…"   |
| `dynamic-projects/projects/:projectId/overview`          | `ProjectOverviewView`      | Stub project page — shows the project label and a placeholder body |
| `dynamic-projects/projects/:projectId/issues`            | `ProjectIssuesView`        | Stub project page — same shape                                     |
| `dynamic-projects/projects/:projectId/settings`          | `ProjectSettingsView`      | Stub project page — same shape                                     |

The three stub project views can share a small base class that pulls the `:projectId` route parameter and renders the matching project's display label as the heading.

A small integration link from the original `MainLayout` (e.g. an extra rail item "Dynamic projects demo") routes to `dynamic-projects` so the new demo is discoverable from the existing landing page. The link is one-way; the new layout does not link back.

## Project model

A small enum or record set defining the three demo projects, with stable IDs and human-readable labels:

| ID         | Display label |
| ---------- | ------------- |
| `phoenix`  | Phoenix       |
| `atlas`    | Atlas         |
| `voyager`  | Voyager       |

The IDs are what end up in URLs (`/dynamic-projects/projects/phoenix/overview`); the labels are what end up in the rail and the multiselect.

## Active projects registry (session scope)

A `ActiveProjectsRegistry` class holds a `Set<String>` of currently active project IDs and emits change events to listeners. One instance per Vaadin session, accessed via a static `current()` helper that lazily creates and stores it in `VaadinSession.getCurrent().getAttribute(...)`. Spring DI is *not* used so the demo stays plain Vaadin and works with both Spring Boot and Vanilla setups.

API:

```java
public final class ActiveProjectsRegistry {
    public static ActiveProjectsRegistry current();

    public void activate(String projectId);
    public void deactivate(String projectId);
    public boolean isActive(String projectId);
    public Set<String> getActive();   // unmodifiable snapshot

    public Registration addChangeListener(Consumer<ChangeEvent> listener);

    public record ChangeEvent(String projectId, boolean active) {}
}
```

`activate` / `deactivate` are no-ops if the state already matches, so callers do not need to dedupe. The change event fires for every effective transition.

## Multiselect in the navbar

A `MultiSelectComboBox<Project>` sits at the navbar's top-right corner of `DynamicProjectsLayout`. It is bound bidirectionally to the registry: the user changing the selection drives `activate` / `deactivate`, and the registry emitting an event syncs the combo box's value (e.g. when a guard redirects after a deactivation flow). Initial value is empty (no projects active on first visit).

```
[ Title ──────────────────────────  Active projects ▾ Phoenix × Atlas × ]
```

ID: `active-projects-select`. Item label generator returns the project's display label.

## SideNav binding

The rail in `DynamicProjectsLayout` has two persistent top-level items:

- **Home** → `dynamic-projects`
- **Projects** → no own URL, parent only; expanded by default

When `Projects` is empty (no project active) it shows itself as an empty folder (the "no children, no popover, just the parent label" case the addon already handles cleanly). When projects are active, each becomes a `SideNavRailItem` child of `Projects` with three nested sub-items (Overview, Issues, Settings) wired to the resolved project paths.

Wiring sequence:

1. On `onAttach` of the rail, the layout subscribes to `ActiveProjectsRegistry#addChangeListener`. Initial render iterates the registry's current `Set<String>` and adds rail items for each.
2. On `ChangeEvent(projectId, active=true)`: a `SideNavRailItem` (`projectLabel`) is added under `Projects`. Its three children use `setPath(view, RouteParameters.of("projectId", projectId))` — Vaadin resolves the template, so the stored path is concrete.
3. On `ChangeEvent(projectId, active=false)`: the rail walks the `Projects` parent's children, finds the matching one (by stored project ID, kept in a side map), and removes it via `parent.remove(item)`.
4. On `onDetach`: the listener registration is released.

The resolved paths in the rail are exactly what `getActiveViewItem()` matches against the current navigation, so the breadcrumb in the layout's content area lights up with the correct project page even during nested navigation.

## BeforeEnter guard on project sub-views

Each of the three `Project*View` classes implements `BeforeEnterObserver`. The shared base class extracts the `:projectId` parameter, looks it up in the registry, and:

- If active → continue rendering normally.
- If inactive → `event.forwardTo(DynamicProjectsHomeView.class)` and the home view shows a transient banner ("Project '<id>' is not active. Activate it via the navbar to view its pages.").

The guard makes "Variant a" semantically honest in the demo: the routes physically exist, but a deactivated project's URLs feel deactivated — typing them in the address bar yields the same "not available" experience as a 404.

## Interaction with the existing demo

The existing `MainLayout` and its rail are unchanged except for one addition: a single rail item under "Operate" (or as a new top-level entry — to be decided during implementation) named **"Dynamic projects demo"** that links to `dynamic-projects`. This makes the new demo discoverable.

`DynamicProjectsLayout` is a separate component; it does not extend or compose `MainLayout`. The two layouts share no Java code beyond importing the same `SideNavRail` API.

## Design decisions captured here

| Question                                            | Choice                                                                  |
| --------------------------------------------------- | ----------------------------------------------------------------------- |
| URL strategy                                        | Variant a — `:projectId` route templates                                |
| Where the demo lives                                | Dedicated layout at `/dynamic-projects`                                 |
| Sub-routes per project                              | Overview, Issues, Settings                                              |
| Registry scope                                      | Session                                                                 |
| `BeforeEnter` guard                                 | Yes — easier to remove later than to add                                |
| Project IDs / labels                                | `phoenix` / `atlas` / `voyager` with capitalised labels                 |
| Multiselect placement                               | Top-right of `DynamicProjectsLayout`'s navbar                           |
| Discovery from existing demo                        | One link from `MainLayout`'s rail                                       |

## Out for V1, possible later

- Adding a fourth sub-route to one project to demonstrate deeper nesting.
- Persisting the active set in `localStorage` or a backend so it survives reload.
- A "Deactivate currently viewed project" affordance on the project pages themselves.
- A second route group `dynamic-projects-scoped/...` that shows the Variant b alternative side-by-side.
