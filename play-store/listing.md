# Luma Launcher - Google Play listing draft

## Store details

- App name: Luma Launcher
- Category: Personalization
- Price: Free
- Ads: No
- Audience: 13 and older
- Devices: Android TV and Google TV only

## Short description

A customizable, favorites-first home screen made for Android TV.

## Full description

Make your TV home screen feel like your own.

Luma Launcher puts your favorite apps first and gives you a clean, remote-friendly home screen designed specifically for Android TV and Google TV.

Features include:

- A favorites-first app bar
- Custom backgrounds and personal photos
- Adjustable card shapes, colors, shades, and sizes
- Built-in and custom app icon artwork
- Current weather and a seven-day forecast
- Wi-Fi, Ethernet, and VPN connection status
- A now-playing music widget with play/pause, next-song, and default-app controls
- A long-press app menu for favorites, uninstalling, and favorite-bar movement
- Simple remote navigation with clear focus effects
- An optional Home screen helper for Google TV devices

Luma has no ads and does not require an account. Your favorites, artwork choices, and custom images stay on your device.

The optional Home screen helper uses Android Accessibility only to detect when the stock TV Home screen appears and return you to Luma. It cannot read screen content or typed text, and it does not collect or share Accessibility data.

## Release notes - 0.4.0

- Added a next-song music control and default music app setting
- Added Favorite, Uninstall, and Move options when holding OK on an app
- Prevented the opening OK-button release from accidentally selecting Favorite
- Fixed Uninstall so Android's confirmation screen opens correctly
- Added eight new 3D-style replacement app icons
- Added a Card names on/off switch
- Added a live card preview inside Customize
- Added a Google Play-managed update version
- Added the required Home helper privacy disclosure
- Limited Play distribution to Android TV and Google TV devices
- Improved signing and release readiness

## Review notes

Luma is a TV launcher. No sign-in is required. All main launcher features can be tested immediately.

To review the optional Home screen helper:

1. Open Customize.
2. Select Enable Home helper.
3. Review and accept the separate disclosure.
4. Android Accessibility settings will open.
5. Enable Luma Home screen helper.

The service only listens for window changes from known stock Android TV launcher package names. `canRetrieveWindowContent` is false.

## Data Safety draft

- Approximate location: Collected only for automatic local weather; required for app functionality; not sold or used for advertising.
- App activity / installed apps: Launchable app names and icons are read and displayed locally; not transmitted.
- Music information: Active media title, artist, artwork, and playback state are read locally only after the user grants Notification access; not transmitted.
- Accessibility: The package name of the stock TV Home screen is processed on-device; no Accessibility data is stored or transmitted.
- Network information: Connection type, local IP address, and VPN status are processed and shown on-device; not transmitted.
- User files: A user-selected background or custom icon stays on-device.
- Account creation: None.
- Ads: None.
- Analytics: None.
