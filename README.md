# Luma Launcher

Luma is a customizable, favorites-first launcher for Android TV and Google TV.

## What is included

- Favorites are the first app row. Hold the remote's **OK** button on any app to add or remove it.
- Installed TV apps are found automatically.
- Four built-in backgrounds plus a photo picker for your own wallpaper.
- Mix-and-match app-card surfaces, shapes, background shades, colors, and sizes, with a live preview inside Customize.
- A **Card names** switch lets you show or hide the app name beneath every card. Icons automatically grow when names are hidden.
- Clean Large, Raised, Sticker, Neon, and Color Bubble icon treatments with live previews.
- Compatible installed Android icon-pack support through each pack's standard app filter.
- Full replacement artwork for Nuvio, 8K/TiviMate, MLB, Netflix, Prime Video, YouTube, Downloader, Google Play Store, Hulu, Internet Speed Test, Play Games, Play Movies & TV, Surfshark, Stremio, YouTube TV, and Spotify, plus a photo picker for replacing any favorite app's actual icon.
- Automatic approximate local weather, city presets, and a clickable seven-day forecast.
- Live Wi-Fi/Ethernet/VPN status and local tunnel address.
- A now-playing widget with album art, separate play/pause and next-song controls, and a saved default music app.
- Hold OK on any app, then release when the menu appears, for Favorite, Uninstall, and favorite-bar Move options. The opening button release is ignored so it cannot accidentally choose the first action.
- Uninstall always opens Android's own confirmation screen; Luma cannot silently remove an app.
- A favorite editor for moving apps left or right.
- Full TV remote focus states and landscape-friendly layout.

## Install the ready-made APK

The ready-made file is `Luma-Launcher.apk` in this folder.

Permanent newest-version link:

https://github.com/mcssc25/LumaLauncher/releases/latest/download/Luma-Launcher.apk

Short TV address (forwards to the permanent link):

https://mcssc25.github.io/

1. On the TV, turn on **Developer options** and **USB debugging** or **Wireless debugging**.
2. Copy the APK to the TV and open it, or use `install-to-tv.ps1` from a Windows computer.
3. Press the TV remote's **Home** button.
4. Choose **Luma Launcher**, then choose **Always** if the TV offers that choice.

You can also open **Customize > Android TV home screen > Choose Luma as default Home**. Android controls this protected choice and requires the TV owner to approve it once. The **Android settings** button in Luma's top-right corner opens the TV's system settings.

If the Google TV device ignores that Home choice, open **Customize > Android TV home screen > Enable Home helper**. In Accessibility, select **Luma Home screen helper** and enable it once. The helper only watches the stock TV launcher and returns the Home button to Luma.

## Updates

Luma checks the public GitHub release automatically every six hours while it is running. When a newer version exists, an **Update** button appears in the top-right corner. You can also use **Customize > App updates > Download newest version** at any time. Android still shows an installation approval screen for each APK update unless the TV is specially managed or rooted.

## Customize app cards

Open **Customize > App cards**. The controls and a sample card appear together, so changes to card names, surface, shape, shade, color, and size can be seen immediately without returning to the Home screen.

## Preview it on this PC

Double-click `Open Luma Preview.bat`. It opens a 1920x1080 Android emulator, installs Luma, makes it the preview's default Home screen, and launches it automatically. Use the keyboard arrow keys to move, **Enter** to select, and **Escape** to go back. The first startup can take about one minute; later launches are faster.

## Music widget permission

Select the music widget and press **OK**. Android opens its notification-access screen. Turn on **Luma music widget**. This lets Luma read the active media title, artist, artwork, playback state, and available media controls. It does not upload notification content.

Choose **Customize > Music widget** to select Spotify or another installed TV music app. When nothing is playing, pressing Play opens that app and asks Android to resume its previous queue. If the app has no resumable queue, it opens normally so you can choose something to play.

## Open the source in Android Studio

1. Open Android Studio.
2. Choose **Open** and select this `LumaLauncher` folder.
3. Let Android Studio finish syncing.
4. Use **Build > Build APK(s)**.

The source uses Kotlin, Jetpack Compose for TV, and Android's normal home-app intent. Weather is provided by Open-Meteo and does not require an API key.

## Current prototype limits

- Icon packs vary in coverage. Apps a selected pack does not include use the chosen built-in icon look.
- The included APK uses Android's debug signature. A private release signing key should be added before public distribution.
