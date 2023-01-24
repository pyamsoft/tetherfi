TetherFi
--------

Share your Android device's Internet connection with other devices without needing Root.

TetherFi works by creating a Wi-Fi Direct legacy group and an HTTP proxy server. Devices can
connect to the broadcasted WiFi network by locating the WiFi SSID displayed in the application
and using the password. Once connected to the network, a device can connect to the Internet by
setting it's proxy server settings to the server created by the TetherFi application at
`192.168.49.1` port `8228`.

Please note that TetherFi is still a work in progress and not everything will work. For example,
using TetherFi to get an open NAT type on consoles is currently not possible, and TetherFi
also currently fails to connect correctly to the Playstation Network. General "normal" internet
browsing should work fine - however it is dependent on the speed and availability of your Android
device's internet connection.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.pyamsoft.tetherfi)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
     alt="Get it on IzzyOnDroid"
     height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.pyamsoft.tetherfi)

or get the APK from the [Releases Section](https://github.com/pyamsoft/tetherfi/releases/latest).

## TetherFi may be for you if:

**You want to share your Android's WiFi or Mobile Data**

TetherFi can share your Android device's Internet connection with one or more non-connected devices.
It is most useful for a device which has an Unlimited Mobile Data plan with your mobile carrier,
or a strong unmetered WiFi connection. You do not need a Hotspot data plan to use TetherFi.

**You have a Hotspot plan from your Mobile Carrier, but it has a data cap**

TetherFi works by proxying your device traffic through your Android device. Since all the traffic
"looks" like it is coming from your Android device (which is not capped in this case), your
TetherFi hotspot traffic will not be capped either.

**You have a Hotspot plan from your Mobile Carrier, but it has a data throttling**

TetherFi works by proxying your device traffic through your Android device. Since all the traffic
"looks" like it is coming from your Android device (which is not throttled in this case), your
TetherFi hotspot traffic will not be throttled either.

**You do not have a mobile Hotspot plan**

TetherFi creates its own access point and network, so you do not need a carrier provided mobile
hotspot plan to use it.

**You wish to create a LAN between devices**

TetherFi acts as the "router" and provides a access point for your other devices to connect to.
Thus, all devices connected to TetherFi form their own local area network. You can possibly do
interesting things with this, like sharing the VPN connection from your Android with another device
(your VPN app must support accessing Local Area Network devices)

**Your home router has reached the device connection limit**

TetherFi acts as the "router" and provides a access point for your other devices to connect to.
Thus, devices connected to TetherFi do not count towards your real router's connection limit.

## Privacy

TetherFi respects your privacy. TetherFi is open source, and always will be. TetherFi
will never track you, or sell or share your data. TetherFi offers in-app purchases which you
may purchase to support the developer. These purchases are never required to use the application
or any features.

**PLEASE NOTE:** TetherFi is **not a fully FOSS application.** This is due to the fact that it
relies on a proprietary In-App Billing library for in-app purchases in order to stay policy
compliant with the leading marketplace.

## Development

TetherFi is developed in the open on GitHub at:

```
https://github.com/pyamsoft/tetherfi
```

If you know a few things about Android programming and are wanting to help
out with development you can do so by creating issue tickets to squash bugs,
and propose feature requests for future inclusion.

# Issues or Questions

Please post any issues with the code in the Issues section on GitHub. Pull Requests
will be accepted on GitHub only after extensive reading and as long as the request
goes in line with the design of the application.

## License

Apache 2

```
Copyright 2021 Peter Kenji Yamanaka

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
