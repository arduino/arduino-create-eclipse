# arduino-eclipse
[![Build Status](https://dev.azure.com/typefox/Arduino/_apis/build/status/TypeFox.arduino-eclipse?branchName=master)](https://dev.azure.com/typefox/Arduino/_build/latest?definitionId=6&branchName=master)

Eclipse Plug-in for importing projects into Eclipse CDT exported by [Arduino Create](https://create.arduino.cc).

Requirements:
 - Java 8 or newer,
 - Eclipse IDE for C/C++ Developers 2018-12 (4.10) or newer, and
 - [`cmake`](https://cmake.org/download/) on the `PATH`.

Build:
```
./mnvw clean verify
```

p2:
The p2 update site is available for each GitHub release. The `latest` p2 is available [here](https://github.com/TypeFox/arduino-eclipse/releases/latest/download/arduino-eclipse-p2.zip). If you want to use the p2 update URL inside Eclipse, you **must** prefix the URL with `jar:` and suffix it with `!/`.
 - `latest` p2 URL: `jar:https://github.com/TypeFox/arduino-eclipse/releases/latest/download/arduino-eclipse-p2.zip!/`,
 - Any other [`semver`](https://semver.org/) release URL: `jar:https://github.com/TypeFox/arduino-eclipse/releases/download/v${semver}/arduino-eclipse-p2.zip!/`

Notes:
 - 🥇This project is built on the top of [`cmake4eclipse`](https://github.com/15knots/cmake4eclipse).