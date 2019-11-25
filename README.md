# arduino-eclipse
[![Build Status](https://dev.azure.com/typefox/Arduino/_apis/build/status/TypeFox.arduino-eclipse?branchName=master)](https://dev.azure.com/typefox/Arduino/_build/latest?definitionId=6&branchName=master)

Eclipse Plug-in for importing projects into Eclipse CDT exported by [Arduino Create](https://create.arduino.cc).

### Requirements:
 - Java 8 or newer,
 - Eclipse IDE for C/C++ Developers 2018-12 (`4.10`) or newer,
 - [`make`](https://www.gnu.org/software/make/), and [`cmake`](https://cmake.org/download/) on the `PATH`.

### Build:
```
./mnvw clean verify
```


### p2:
The p2 update sites are available for each released version on GitHub as a ZIP archive. The `latest` p2 is available [here](https://github.com/TypeFox/arduino-eclipse/releases/latest/download/arduino-eclipse-p2.zip). If you want to use the p2 update URL inside Eclipse, you **must** prefix the URL with `jar:` and suffix it with `!/`.
 - `latest` p2:
   ```
   jar:https://github.com/TypeFox/arduino-eclipse/releases/latest/download/arduino-eclipse-p2.zip!/
   ```
 - Generic p2 URL pattern for any [`semver`](https://semver.org/) releases:
   ```
   jar:https://github.com/TypeFox/arduino-eclipse/releases/download/v${semver}/arduino-eclipse-p2.zip!/
   ```
 - Example for `0.0.1`:
   ```
   jar:https://github.com/TypeFox/arduino-eclipse/releases/download/v0.0.1/arduino-eclipse-p2.zip!/
   ```

### License:

The plug-in is licensed under the [`EPL-2.0`](https://www.eclipse.org/legal/epl-2.0/).

### Notes:
 - This project is built on the top of [`cmake4eclipse`](https://github.com/15knots/cmake4eclipse)🥇