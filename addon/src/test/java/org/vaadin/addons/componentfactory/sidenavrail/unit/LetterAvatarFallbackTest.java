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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRail;
import org.vaadin.addons.componentfactory.sidenavrail.SideNavRailItem;

/**
 * Verifies the letter-avatar fallback that {@link SideNavRailItem} auto-generates when it has a
 * label but no prefix component. The fallback is the first (uppercase) letter of the label wrapped
 * in a {@code <span class="side-nav-rail-letter-avatar">}; CSS hides it in normal mode and shows it
 * only in rail mode. These tests exercise the server-side state; the visual surfacing is covered by
 * the E2E suite.
 */
class LetterAvatarFallbackTest {

    private static final String AVATAR_CLASS = "side-nav-rail-letter-avatar";

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void itemWithoutPrefixGetsAvatarOnAttach() {
        SideNavRailItem item = new SideNavRailItem("Dashboard", "/");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        Avatar avatar = assertAvatar(item);
        assertEquals("D", avatar.getAbbreviation());
    }

    @Test
    void avatarUsesUppercaseFirstLetterEvenForLowercaseLabel() {
        SideNavRailItem item = new SideNavRailItem("inbox", "/inbox");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertEquals("I", assertAvatar(item).getAbbreviation());
    }

    @Test
    void userProvidedPrefixIsLeftAlone() {
        Icon icon = VaadinIcon.DASHBOARD.create();
        SideNavRailItem item = new SideNavRailItem("Dashboard", "/", icon);
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertSame(
                icon,
                item.getPrefixComponent(),
                "User icon must remain untouched by the fallback logic");
    }

    @Test
    void setLabelUpdatesAvatarLetter() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertEquals("C", assertAvatar(item).getAbbreviation());
        item.setLabel("Branches");
        assertEquals("B", assertAvatar(item).getAbbreviation());
    }

    @Test
    void userIconReplacesAvatar() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertAvatar(item);
        Icon userIcon = VaadinIcon.CODE.create();
        item.setPrefixComponent(userIcon);
        assertSame(userIcon, item.getPrefixComponent());
    }

    @Test
    void clearingPrefixRegeneratesAvatar() {
        Icon icon = VaadinIcon.CODE.create();
        SideNavRailItem item = new SideNavRailItem("Code", "/code", icon);
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertSame(icon, item.getPrefixComponent());
        item.setPrefixComponent(null);

        Avatar restored = assertAvatar(item);
        assertEquals("C", restored.getAbbreviation());
    }

    @Test
    void blankLabelProducesNoAvatar() {
        SideNavRailItem item = new SideNavRailItem("");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertNull(
                item.getPrefixComponent(),
                "Blank label must not synthesize an avatar — there is no letter to derive");
    }

    @Test
    void blankLabelRemovesStaleAvatarWhenLabelIsCleared() {
        SideNavRailItem item = new SideNavRailItem("Code", "/code");
        SideNavRail rail = new SideNavRail();
        rail.addItem(item);
        UI.getCurrent().add(rail);

        assertAvatar(item);
        item.setLabel("");
        assertNull(
                item.getPrefixComponent(),
                "Clearing the label must also remove the now-meaningless letter avatar");
    }

    private static Avatar assertAvatar(SideNavRailItem item) {
        Component prefix = item.getPrefixComponent();
        assertNotNull(prefix, "Expected the fallback avatar to be set");
        Avatar avatar =
                assertInstanceOf(
                        Avatar.class,
                        prefix,
                        "Fallback must be a vaadin-avatar (Avatar component)");
        if (!avatar.getClassNames().contains(AVATAR_CLASS)) {
            throw new AssertionError(
                    "Prefix avatar is not the auto-generated fallback (missing marker class '"
                            + AVATAR_CLASS
                            + "')");
        }
        return avatar;
    }
}
