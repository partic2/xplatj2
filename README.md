# XPLATJ2

XPLATJ is a trial project aim to make a cross-platform layer to create application (This project is still in early stage.).


## How to build

### prerequire

C Compiler(GCC/CLANG)

CMake

OpenJDK

Android SDK

Android NDK

Python3(to run build script)

Git

### Step1

```
cd $SOURCE_DIR_ROOT
python deps/pull_deps.py
```

### Step2

```
cd launcher

# You should create and customize build_config.txt refer to build_config_sample.txt.
# DESKTOP_TOOLCHAIN_LIST is required to build desktop version release.

python build.py

```


### Step3

You can find the release in ${SOURCE_DIR_ROOT}/launcher/build/*



## WIP
switch this project to be a pxseed-loader.