# XPLATJ2

XPLATJ is a trial project aim to make a cross-platform layer to create application (This project is still in early stage.).

this repository is a fork from XPLATJ1(https://github.com/partic2/xplatj), and gdx backend is dropped.
(but there are still possibility for gdx backend to reappear in future.)

### Why dropped gdx backend and switch to XPLATJ2
Even I try to embed most source into XPLATJ1, But there is still too much prebuilt binary that matched source have missed, and the dynamic module mechanism of Java/Android is not as convinient as some "script language", due to platform difference and type model. and is impossible to upgrade Java on Android without system upgrade. 

## How to build

### prerequire
C Compiler(GCC CLANG)

CMake

GNU Shell(MSYS,MSYS2 or Cygwin on Windows)

OpenJDK

Gradle

Android SDK

Android NDK

SDL2 Source

### step1
```
cd $XPLATJ_SOURCE_ROOT
export ANDROID_NDK=AndroidNdkDir  #set Android ndk location
export ANDROID_HOME=AndroidSdkDir #set Android sdk location
cp $SDL_SOURCE_ROOT $XPLATJ_SOURCE_ROOT/SDL  #copy SDL source

cd launcher
export targetsysname=windows #set target platform, can be one of android-arm,android-aarch64,windows,linux

export CC=GCC
export CXX=G++
export AR=ar
export MAKE=make
export SHELL=sh


$SHELL build.sh
```

If target to windows,linux, you should also modify the config file ${XPLATJ_SOURCE_ROOT}/javase-lwjgl/config.gradle depend on your target platform.

### step2
On Windows/Linux etc... You can find the distrubution in ${XPLATJ_SOURCE_ROOT}/launcher/dist

On Android, You can find the distrubution in ${XPLATJ_SOURCE_ROOT}/android-project/build/outputs/apk/release

### note
Msys1 may miss cygpath which required by gradle, you can simplely implement it by print the first input arguement.


### Two backend
You can switch the backend by modify the config file "$RESOURCE_DIR/flat") The first word control the backend and can be one of webapp or sdl.

When use SDL backebd, xplatj compile "$RESOURCE_DIR/boot0.c" and load this file , then run the entry function _start(void *)
Some symbol will added to the context. View launcher/SDLLoader.c for more detail. 

On windows/linux target:
RESOURCE_DIR=./res 

On android target:
RESOURCE_DIR=/sdcard/xplat 
