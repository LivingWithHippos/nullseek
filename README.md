<p align="center">
    <img width="200" src="/extra/logo/logo.svg?raw=true" alt="logo">
</p>

# nullseek

An Android application to interact with [Torbox.](https://torbox.app)

## Features

- view your Torbox account status and statistics
- manage web, torrent and usenet lists
- create new downloads from links/magnets/torrents
- send media to remote Kodi/VLC devices
- two widgets for monitoring/adding links (free for FOSS users, premium on the Play Store (todo))
- support for searching files via Torznab/Stremio addons

| User                                                                                                              | New Download                                                                                                       | IMDB                                                                                                       |
|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" alt="User Screen"> | <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" alt="New Download"> | <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" alt="IMDB"> |

| Settings                                                                                                              | Details                                                                                                       | Services                                                                                                       |
|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" alt="User Screen"> | <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/5.jpg" alt="New Download"> | <img width="150" src="https://raw.githubusercontent.com/LivingWithHippos/nullseek/refs/heads/master/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" alt="IMDB"> |

## Installing

F-Droid and the Play Store have not been set up yet. You can install and keep nullseek updated with [Obtainium](https://github.com/ImranR98/Obtainium), or grab an APK from the [release section](https://github.com/LivingWithHippos/nullseek/releases) (press `Assets` to show them).

| | Phone | Android TV |
| :--- | :---: | :---: |
| **Release** | <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.github.livingwithhippos.nullseek%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLivingWithHippos%2Fnullseek%22%2C%22author%22%3A%22LivingWithHippos%22%2C%22name%22%3A%22nullseek%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5Emobile-foss-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Android%20client%20for%20TorBox%3A%20manage%20your%20torrents%2C%20web%20and%20usenet%20downloads%2C%20search%20indexers%20and%20Stremio%20addons%2C%20and%20stream%20to%20Kodi%20or%20VLC.%5C%22%7D%22%7D"><img src="extra/badges/badge_obtainium.png" alt="Get nullseek on Obtainium" height="72"></a> | <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.github.livingwithhippos.nullseek.tv%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLivingWithHippos%2Fnullseek%22%2C%22author%22%3A%22LivingWithHippos%22%2C%22name%22%3A%22nullseek%20TV%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5Etv-foss-release%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Android%20TV%20client%20for%20TorBox%3A%20manage%20your%20torrents%2C%20web%20and%20usenet%20downloads%2C%20search%20indexers%20and%20Stremio%20addons%2C%20and%20stream%20to%20Kodi%20or%20VLC.%5C%22%7D%22%7D"><img src="extra/badges/badge_obtainium.png" alt="Get nullseek TV on Obtainium" height="72"></a> |
| **Debug** | <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.github.livingwithhippos.nullseek.debug%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLivingWithHippos%2Fnullseek%22%2C%22author%22%3A%22LivingWithHippos%22%2C%22name%22%3A%22nullseek%20debug%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5Emobile-foss-debug%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Debug%20build%2C%20includes%20telemetry%20to%20log%20crashes.%20Installs%20next%20to%20the%20release%20one.%20Android%20client%20for%20TorBox%3A%20manage%20your%20torrents%2C%20web%20and%20usenet%20downloads%2C%20search%20indexers%20and%20Stremio%20addons%2C%20and%20stream%20to%20Kodi%20or%20VLC.%5C%22%7D%22%7D"><img src="extra/badges/badge_obtainium.png" alt="Get nullseek debug on Obtainium" height="48"></a> | <a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.github.livingwithhippos.nullseek.tv.debug%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FLivingWithHippos%2Fnullseek%22%2C%22author%22%3A%22LivingWithHippos%22%2C%22name%22%3A%22nullseek%20TV%20debug%22%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5Etv-foss-debug%5C%5C%5C%5C.apk%24%5C%22%2C%5C%22about%5C%22%3A%5C%22Debug%20build%2C%20includes%20telemetry%20to%20log%20crashes.%20Installs%20next%20to%20the%20release%20one.%20Android%20TV%20client%20for%20TorBox%3A%20manage%20your%20torrents%2C%20web%20and%20usenet%20downloads%2C%20search%20indexers%20and%20Stremio%20addons%2C%20and%20stream%20to%20Kodi%20or%20VLC.%5C%22%7D%22%7D"><img src="extra/badges/badge_obtainium.png" alt="Get nullseek TV debug on Obtainium" height="48"></a> |

Debug builds install next to the release ones, as separate apps, and include telemetry to solve crashes.

## Donate :coffee:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/E1E412NFX7)


You can use [my referral link](https://torbox.app/subscription?referral=dba208d7-f53e-4711-96db-97c7051799bc) to get Torbox premium.

## Credits

Magnet Icon From [Kenan Gundogan](https://www.svgrepo.com/svg/394265/magnet)
