# arduino-create-eclipse
[![Build Status](https://dev.azure.com/typefox/Arduino/_apis/build/status/arduino.arduino-create-eclipse?branchName=master)](https://dev.azure.com/typefox/Arduino/_build/latest?definitionId=7&branchName=master)

Eclipse Plug-in for importing projects into Eclipse CDT exported by [Arduino Create](https://create.arduino.cc).

### Requirements:
 - Java 8 or newer,
 - Eclipse IDE for C/C++ Developers 2018-12 (`4.10`) or newer,
 - [`make`](https://www.gnu.org/software/make/), and [`cmake`](https://cmake.org/download/) on the `PATH`.

### Build:
Command line:
```
./mnvw clean verify
```

Eclipse:
 - Clone the [`arduino-create-eclipse`](https://github.com/arduino/arduino-create-eclipse.git) repository and
 - Import the sources into your Eclipse workspace with
   - `Import...` > `General` > `Existing Projects into Workspace`.
   - `Select root directory` and browse the root of the cloned repository.
   - Enable `Search for nested projects`, and
   - Hit `Finish` ✨

### Install:
The easiest way to install, is to drag and drop the `Install` button into your running Eclipse workspace. This plug-in is also [available at Eclipse Marketplace](https://marketplace.eclipse.org/content/arduino-create-eclipse-plug).

[![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=4895924 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

### p2:
The p2 update sites are available for each released version on GitHub as a ZIP archive. The `latest` p2 is available [here](https://github.com/arduino/arduino-create-eclipse/releases/latest/download/arduino-create-eclipse-p2.zip). If you want to use the p2 update URL inside Eclipse, you **must** prefix the URL with `jar:` and suffix it with `!/`.
 - URL of the `latest` p2:
   ```
   jar:https://github.com/arduino/arduino-create-eclipse/releases/latest/download/arduino-create-eclipse-p2.zip!/
   ```
 - Generic p2 URL pattern for any [`semver`](https://semver.org/) releases:
   ```
   jar:https://github.com/arduino/arduino-create-eclipse/releases/download/v${semver}/arduino-create-eclipse-p2.zip!/
   ```
 - Example URL for version `0.0.1`:
   ```
   jar:https://github.com/arduino/arduino-create-eclipse/releases/download/v0.0.1/arduino-create-eclipse-p2.zip!/
   ```

### Arduino Create:
The following Arduino Create export structures are supported:
 - Folder:
   ```
   _cmake
   |____ build (empty)
   |____ core
   |  |____ Arduino.h
   |  |____ (other core .h and .cpp files)
   |____ lib
   |  |____ (3rd party libraries if any)
   |____ sketch
   |  |___ sketch-file.ino.cpp
   |____ CMakeLists.txt   
   ```
 - ZIP:
   ```
   arduino_create-cmake.zip
   |____ _cmake
     |____ build (empty)
     |____ core
     |  |____ Arduino.h
     |  |____ (other core .h and .cpp files)
     |____ lib
     |  |____ (3rd party libraries if any)
     |____ sketch
     |  |___ sketch-file.ino.cpp
     |____ CMakeLists.txt
   ```

### Limitations:
Currently, you can build the imported Arduino Create project on Linux only.
 - Windows:
   ```
   C:\path\to\your\project\_cmake\core\shell.h:8:10: fatal error: sys/wait.h: No such file or directory
    #include <sys/wait.h>
             ^~~~~~~~~~~~
   compilation terminated.
   ```
 - macOS:
   ```
   /path/to/your/project/_cmake/core/DebugSerial.h:56:2: error: unknown type name 'pthread_barrier_t'
       pthread_barrier_t _barrier;
       ^
   1 error generated.
   ```

### FAQ:
 Q: What happens when `cmake` is not installed on the system?\
 A: If `cmake` is not installed, you will see the following console output in Eclipse:

```
Buildfile generation error occurred..
cmake -DCMAKE_BUILD_TYPE:STRING=Debug -DCMAKE_EXPORT_COMPILE_COMMANDS:BOOL=ON -G “Unix Makefiles” /home/user_name/dev/arduino-create-eclipse-ws/arduino_create-cmake
Cannot run program “cmake”: Unknown reason
Error: Program “cmake” not found in PATH
PATH=[/home/user_name/bin:/home/user_name/.local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin]
Build stopped..
```

 Q: What happens when `cmake4eclipse` is not installed in Eclipse?\
 A: This Eclipse plug-in has a hard dependency on `cmake4eclipse`. It will be implicitly installed into Eclipse when this Eclipse plug-in is installed either from a p2 update site or the Eclipse Marketplace.

### License:

 - This plug-in is licensed under the [`EPL-2.0`](https://www.eclipse.org/legal/epl-2.0/).

### Notes:
 - This project is built on the top of [`cmake4eclipse`](https://github.com/15knots/cmake4eclipse)🥇
