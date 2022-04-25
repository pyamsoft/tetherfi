# widefi

Internet sharing without Root

[![Get it on Google Play](https://raw.githubusercontent.com/pyamsoft/widefi/main/art/google-play-badge.png)][1]

# What

Share your Android device's Internet connection with other devices without needing Root.

WideFi works by creating a Wi-Fi Direct legacy group and an HTTP proxy server. Devices can
connect to the broadcasted WiFi network by locating the WiFi SSID displayed in the application
and using the password. Once connected to the network, a device can connect to the Internet by
setting it's proxy server settings to the server created by the WideFi application at
`192.168.49.1` port `8228`.

Please note that WideFi is still a work in progress and not everything will work. For example,
using WideFi to get an open NAT type on consoles is currently not possible, and WideFi
also currently fails to connect correctly to the Playstation Network. General "normal" internet
browsing should work fine - however it is depedent on the speed and availability of your Android
device's internet connection.

## Privacy

WideFi respects your privacy. WideFi is open source, and always will be. WideFi
will never track you, or sell or share your data. WideFi offers in-app purchases which you
may purchase to support the developer. These purchases are never required to use the application
or any features.

**PLEASE NOTE:** WideFi is **not a fully FOSS application.** This is due to the fact that it
relies on a proprietary In-App Billing library for in-app purchases in order to stay policy
compliant with the leading marketplace.

## Development

WideFi is developed in the open on GitHub at:

```
https://github.com/pyamsoft/widefi
```

If you know a few things about Android programming and are wanting to help
out with development you can do so by creating issue tickets to squash bugs,
and propose feature requests for future inclusion.

# Issues or Questions

Please post any issues with the code in the Issues section on GitHub. Pull Requests
will be accepted on GitHub only after extensive reading and as long as the request
goes in line with the design of the application.

[1]: https://play.google.com/store/apps/details?id=com.pyamsoft.widefi

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
