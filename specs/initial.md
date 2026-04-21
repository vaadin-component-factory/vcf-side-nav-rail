# Collapsible SideNav

## Description 

The SideNav component is already used by the customer’s application. However it is missing two main features:
Being collapsible to an “icon only” state
Showing submenus as a popover, when the category is closed
This feature integration will produce a new subclass that extends the SideNav and SideNavItem.

## Popover behavior

The popover will be shown when a side nav item, that has children, is collapsed or when the side nav itself is collapsed to the “icon only” state.
It will automatically show on hover of the mouse.

### Multi levels

Side nav items can contain other items to produce a multi level navigation.
However, the collapsible side nav will always only produce one popover, containing the non-root side nav items. Inside that popover, side nav items, that contain children on their own, can be expanded and collapsed like within the normal side nav.

## Publish as a Vaadin Component Factory addon

The new component will be published in the Vaadin directory under the ownership of the Vaadin Component Factory. With this, it will officially count as a 3rd party library and can be used by anyone.
It will be licensed under the Apache License 2.0

## Out of Scope

### Collapse button

The collapsible side nav will not automatically add a collapse button, but only provide the api to collapse / expand it. This ensures that applications can integrate the “collapse side nav” button as needed.

### Auto collapse

The collapsible side nav will not provide an auto collapse feature. This shall ensure, that it can be used in other spots than the “main layout” and that the collapse can be fully controlled by the application (e.g. using the Page window resize listener)

### Auto hide

The collapsible side nav will not provide an auto hide feature like the AppLayout drawer.
