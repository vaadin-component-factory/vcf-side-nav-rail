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
package org.vaadin.addons.componentfactory.sidenavrail.demo;

import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Deliberately declares <em>no</em> {@code @StyleSheet(Aura.STYLESHEET)} theme.
 *
 * <p>In Vaadin 25, when no {@code AppShellConfigurator} exists, Aura is auto-loaded as a
 * non-removable {@code <link>}. By defining this (empty) app shell we suppress that auto-load, so
 * {@link MainLayout} can own the theme entirely and swap Aura&harr;Lumo at runtime via {@code
 * Page.addStyleSheet(...)} + {@code Registration.remove()}. The default theme (Aura) is applied by
 * {@code MainLayout} on attach.
 */
public class DemoAppShell implements AppShellConfigurator {}
