# NullSeek Privacy Policy

**Effective date:** October 31, 2026

NullSeek is an open-source Android app for phones and Android TV that lets you manage your own [TorBox](https://torbox.app) account. This policy explains what happens to your information when you use it.

## Summary

- NullSeek does **not** collect, store on any server, sell or share your personal data.
- There is no account with the NullSeek developer, and **no analytics, tracking or ads** in the published app.
- Everything you enter in the app stays on your device.
- The app connects only to the services needed to work, listed below, and to the ones you add yourself.

## Data stored on your device

NullSeek saves the following **only on your device**, in the app's private storage:

- your TorBox API key(s) and the account names you choose;
- the app settings, such as theme, language, download and other preferences;
- media players (Kodi, VLC), indexers and other addons you configure, including the addresses, usernames and passwords you enter for them;
- your favourite titles and recent searches;
- in the Google Play version, whether the widget subscription is active, so the widgets work offline.

The developer has no access to this data. Uninstalling the app or clearing its data deletes it.

**Backups:** if you use *Create backup*, the app writes a file to a location you choose. It contains your settings, accounts and API keys in plain text. The file is yours: it is never uploaded anywhere by the app, and you are responsible for keeping it safe.

## Network connections

To work, NullSeek connects directly from your device to:

| Service | Why | What is sent |
|---|---|---|
| **TorBox** (torbox.app) | Manage your downloads, account and links | Your API key and the requests you make, such as adding or listing downloads |
| **IMDb** (imdb.com, media-imdb.com) | Title search and posters | The text you search for |
| **Cinemeta** (strem.io) | Movie and series details | IMDb IDs of the titles you open |

It also connects, only when you set them up, to them.

These services receive the requests needed to do what you ask, plus technical data any internet connection involves, such as your IP address. They are run by third parties with their own privacy policies, for example [TorBox](https://torbox.app) and [IMDb](https://www.imdb.com/privacy). NullSeek has no control over them. NullSeek does not host or provide any of the content you search for, stream or download.

## Purchases (Google Play version)

The Google Play version offers an optional subscription that unlocks the home screen widgets.

- Payments are processed entirely by **Google Play**. The developer never receives your payment details.
- The app asks Google Play whether your subscription is active and remembers the answer on your device. No purchase information is sent to the developer or anyone else.
- You can manage or cancel the subscription at any time in the Google Play Store, under *Payments & subscriptions*.

Google's handling of your data is described in the [Google Privacy Policy](https://policies.google.com/privacy).

The open-source version from GitHub has no purchases: all widgets are unlocked.

## Permissions

| Permission | Why |
|---|---|
| Internet | Connect to TorBox and the other services listed above |
| Network state | Check whether you are online before making requests |
| Local network access | Connect to media players and indexers on your home network, if you add any |

## Children

NullSeek is not directed at children and does not knowingly collect data from anyone, including children.

## Open source

NullSeek's source code is public at <https://github.com/LivingWithHippos/nullseek>, so anyone can check how the app handles data.

## Changes to this policy

If this policy changes, the updated version will be published in this file with a new effective date.

## Contact

For questions about this policy, open an issue at <https://github.com/LivingWithHippos/nullseek/issues>.
