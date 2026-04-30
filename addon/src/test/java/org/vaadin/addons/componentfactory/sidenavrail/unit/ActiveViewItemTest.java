/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.addons.componentfactory.sidenavrail.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Location;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Covers {@link SideNavRail#getActiveViewItem()} and
 * {@link SideNavRail#getActiveViewItems()} — path/alias matching against the
 * current active view location, with {@code matchNested} intentionally ignored.
 */
class ActiveViewItemTest {

    @BeforeEach void setUp() { MockVaadin.setup(); }
    @AfterEach void tearDown() { MockVaadin.tearDown(); }

    @Test
    void emptyListWhenNoItemMatches() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Code", "/code"));
        UI.getCurrent().add(nav);
        setActiveLocation("dashboard");

        assertTrue(nav.getActiveViewItems().isEmpty(),
                "no item matches the current location → empty list");
        assertTrue(nav.getActiveViewItem().isEmpty(),
                "convenience accessor must mirror the empty result");
    }

    @Test
    void singleMatchByPath() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem code = new SideNavRailItem("Code", "/code");
        nav.addItem(code);
        UI.getCurrent().add(nav);
        setActiveLocation("code");

        assertEquals(List.of(code), nav.getActiveViewItems());
        assertSame(code, nav.getActiveViewItem().orElseThrow());
    }

    @Test
    void singleMatchByAlias() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem code = new SideNavRailItem("Code", "/code");
        code.setPathAliases(Set.of("source", "src"));
        nav.addItem(code);
        UI.getCurrent().add(nav);
        setActiveLocation("source");

        assertEquals(List.of(code), nav.getActiveViewItems(),
                "alias match must surface the item just like a path match");
    }

    @Test
    void matchNestedParentDoesNotMatchWhenOnlyDescendantOwnsPath() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        parent.setMatchNested(true);
        SideNavRailItem child = new SideNavRailItem("Branches", "/code/branches");
        parent.addItem(child);
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        setActiveLocation("code/branches");

        // matchNested is deliberately ignored — the parent does not surface as
        // active just because a descendant owns the current path.
        assertEquals(List.of(child), nav.getActiveViewItems(),
                "matchNested parent must not appear; only the descendant matches");
    }

    @Test
    void multipleItemsWithSamePathAllReturnedInDfsOrder() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem first = new SideNavRailItem("Code", "/code");
        SideNavRailItem nested = new SideNavRailItem("Code (nested)", "/code");
        first.addItem(nested);
        SideNavRailItem second = new SideNavRailItem("Code (root copy)", "/code");
        nav.addItem(first);
        nav.addItem(second);
        UI.getCurrent().add(nav);
        setActiveLocation("code");

        // DFS pre-order: first, then its descendant, then the second root.
        assertEquals(List.of(first, nested, second), nav.getActiveViewItems());
        assertSame(first, nav.getActiveViewItem().orElseThrow(),
                "single-match accessor must return the DFS-first item");
    }

    @Test
    void aliasVsPathCollisionReturnsBoth() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem byPath = new SideNavRailItem("Source", "/source");
        SideNavRailItem byAlias = new SideNavRailItem("Code", "/code");
        byAlias.setPathAliases(Set.of("source"));
        nav.addItem(byPath);
        nav.addItem(byAlias);
        UI.getCurrent().add(nav);
        setActiveLocation("source");

        assertEquals(List.of(byPath, byAlias), nav.getActiveViewItems(),
                "both the path-owner and the alias-owner must surface");
    }

    @Test
    void popoverClonesAreNotReturned() {
        // In rail mode, parents with children get a popover that contains a
        // nested SideNav with cloned SideNavItems. Those clones must not be
        // counted as matches — the API only walks the real rail item tree.
        SideNavRail nav = new SideNavRail();
        nav.setRailMode(true);
        SideNavRailItem parent = new SideNavRailItem("Code", "/code");
        SideNavRailItem branches = new SideNavRailItem("Branches", "/code/branches");
        parent.addItem(branches);
        nav.addItem(parent);
        UI.getCurrent().add(nav);
        setActiveLocation("code/branches");

        List<SideNavRailItem> matches = nav.getActiveViewItems();
        assertEquals(List.of(branches), matches,
                "only the real branches item must surface, not the popover clone");
        assertSame(branches, matches.get(0));
    }

    @Test
    void emptyWhenNoActiveLocation() {
        // MockVaadin.setup() leaves the active view location at Location("").
        // No item with an empty path → no matches.
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Code", "/code"));
        UI.getCurrent().add(nav);

        assertTrue(nav.getActiveViewItems().isEmpty(),
                "default empty location must not match any path-bearing item");
        assertTrue(nav.getActiveViewItem().isEmpty());
    }

    @Test
    void trailingSlashesAreNormalized() {
        SideNavRail nav = new SideNavRail();
        SideNavRailItem code = new SideNavRailItem("Code", "/code/");
        nav.addItem(code);
        UI.getCurrent().add(nav);
        setActiveLocation("code");

        assertEquals(List.of(code), nav.getActiveViewItems(),
                "trailing slash on item path must not prevent the match");
    }

    private static void setActiveLocation(String path) {
        Location location = new Location(path);
        try {
            Field field = com.vaadin.flow.component.internal.UIInternals.class
                    .getDeclaredField("viewLocation");
            field.setAccessible(true);
            field.set(UI.getCurrent().getInternals(), location);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(
                    "Failed to set active view location via reflection — Vaadin "
                            + "internal layout may have changed", e);
        }
    }

    @Test
    void emptyWhenNoUI() {
        SideNavRail nav = new SideNavRail();
        nav.addItem(new SideNavRailItem("Code", "/code"));
        UI.getCurrent().add(nav);

        // Tear the UI down; SideNavRail must return empty rather than NPE.
        MockVaadin.tearDown();
        try {
            assertTrue(nav.getActiveViewItems().isEmpty(),
                    "no current UI → empty list, no NPE");
            assertTrue(nav.getActiveViewItem().isEmpty());
        } finally {
            // Re-establish a UI so the @AfterEach tearDown succeeds.
            MockVaadin.setup();
        }
    }

    @Test
    void getActiveViewItemReturnsEmptyOptionalNotNull() {
        SideNavRail nav = new SideNavRail();
        UI.getCurrent().add(nav);

        Optional<SideNavRailItem> match = nav.getActiveViewItem();
        assertTrue(match.isEmpty(),
                "must return Optional.empty(), never null");
    }
}
