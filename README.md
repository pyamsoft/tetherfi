TetherFi
--------

Share your Android device's Internet connection with other devices without needing Root.

### Get TetherFi

[<img
src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
alt="Get it on Google Play"
height="80">](https://play.google.com/store/apps/details?id=com.pyamsoft.tetherfi)
[<img
src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="Get it on IzzyOnDroid"
height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.pyamsoft.tetherfi)
[<img
src="https://www.openapk.net/images/openapk-badge.png"
alt="Get it on OpenAPK"
height="80">](https://www.openapk.net/tetherfi/com.pyamsoft.tetherfi/)

or get the APK from the
[Releases Section](https://github.com/pyamsoft/tetherfi/releases/latest).

### Screenshots

#### Hotspot Status

[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306185.png"
alt="Light Mode: Hotspot Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306185.png)
[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306400.png"
alt="Light Mode: Hotspot Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306400.png)

#### Setup Instructions

[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306191.png"
alt="Light Mode: Instructions Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306191.png)
[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306448.png"
alt="Light Mode: Instructions Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306448.png)

#### Manage Connections

[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306373.png"
alt="Light Mode: Connections Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306373.png)
[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306450.png"
alt="Light Mode: Connections Screen"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306450.png)

#### Hotspot Active

[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306341.png"
alt="Light Mode: Hotspot Active"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306341.png)
[<img
src="https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306443.png"
alt="Light Mode: Hotspot Active"
height="200">](https://raw.githubusercontent.com/pyamsoft/tetherfi/main/art/screens/phone/raw/Screenshot_1715306443.png)

### What is TetherFi

TetherFi works by creating a Wi-Fi Direct legacy group and an HTTP proxy server. Other
devices can connect to the broadcasted Wi-Fi network, and connect to the Internet by
setting the proxy server settings to the server created by TetherFi. You do not need a
Hotspot data plan to use TetherFi, but the app works best with "unlimited" data plans.

#### TetherFi may be for you if:

- You want to share your Android's Wi-Fi or Cellular Data
- You have an Unlimited Data and a Hotspot plan from your Carrier, but Hotspot
  has a data cap
- You have an Unlimited Data and a Hotspot plan from your Carrier, but Hotspot
  has throttling
- You do not have a mobile Hotspot plan
- You wish to create a LAN between devices
- Your home router has reached the device connection limit

### How

TetherFi uses a Foreground Service to create a long-running Wi-Fi Direct Network that
other devices can connect to. Connected devices can exchange network data between each other.
The user is in full control of this Foreground Service and can explicitly choose when to
turn it on and off.

TetherFi is still a work in progress and not everything will work. For example, using the
app to get an open NAT type on consoles is currently not possible. Using TetherFi for certain
online apps, chat apps, video apps, and gaming apps is currently not possible. Some services
such as email may be unavailable. General "normal" internet browsing should work fine - however,
it is dependent on the speed and availability of your Android device's internet connection.

To see a list of apps that are known to not work currently, see the
[Wiki](https://github.com/pyamsoft/tetherfi/wiki/Known-Not-Working)

### Privacy

TetherFi respects your privacy. TetherFi is open source, and always will be. TetherFi
will never track you, or sell or share your data. TetherFi offers in-app purchases,
which you may purchase to support the developer. These purchases are never
required to use the application or any features.

### Development

TetherFi is developed in the open on GitHub at:

[https://github.com/pyamsoft/tetherfi](https://github.com/pyamsoft/tetherfi)

If you know a few things about Android programming and want to help out with
development, you can do so by creating issue tickets to squash bugs, and
proposing feature requests.

## License

Apache 2

```
Copyright 2024 pyamsoft

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
