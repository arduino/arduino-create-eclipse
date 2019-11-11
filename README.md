# arduino-eclipse
[![Build Status](https://dev.azure.com/typefox/Arduino/_apis/build/status/TypeFox.arduino-eclipse?branchName=master)](https://dev.azure.com/typefox/Arduino/_build/latest?definitionId=6&branchName=master)

Eclipse Plug-in for importing projects into Eclipse CDT exported by Arduino Create

Requirements:
 - Java 8 or newer,
 - Eclipse IDE for C/C++ Developers 2018-12 (4.10) or newer, and
 - [`cmake`](https://cmake.org/download/) on the `PATH`.

Build:
```
./mnvw clean verify
```

Notes:
 - 🥇This project is built on the top of [`cmake4eclipse`](https://github.com/15knots/cmake4eclipse).