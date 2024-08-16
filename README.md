# XPLATJ2

XPLATJ is a trial project aim to make a cross-platform layer to create application (This project is still in early stage.).

this repository is a fork from XPLATJ1(https://github.com/partic2/xplatj1), and gdx backend is dropped([*](#why-dropped-gdx-backend-and-switch-to-xplatj2)).

## How to build

### prerequire

C Compiler(GCC CLANG)

CMake

OpenJDK

Android SDK

Android NDK

SDL2 Source

Python3(to run build script)

### Step1

```
cd $XPLATJ_SOURCE_DIR
export ANDROID_HOME=AndroidSdkDir  #set Android sdk location, android ndk should be installed at $ANDROID_HOME/ndk
cp $SDL_SOURCE_DIR $XPLATJ_SOURCE_DIR/SDL  #copy SDL source
```

### Step2

```
cd launcher

# You should create and customize build_config.txt refer to build_config_sample.txt.
# DESKTOP_TOOLCHAIN_LIST is required to build desktop version release.

python build.py

```


### Step3

You can find the release in ${XPLATJ_SOURCE_DIR}/launcher/build/*



## Two backend
You can switch the backend by modify the config file "$DATA_DIR/xplat-flag.txt") The first word control the backend and can be one of webapp or sdl.

When use SDL backebd, xplatj try to load "$DATA_DIR/boot0.wasm" by using pwart. Then run the exported entry function _start(void *) .
Some symbol(wasi libffi pwart_builtin etc.) will added to the context. View [pwart](pwart/README.md),and [SDLLoader.c](launcher/SDLLoader.c) for more detail. 


## Why dropped gdx backend and switch to XPLATJ2
The dynamic module mechanism of Java/Android is not as convinient as some "script language", due to platform difference and type model. and it is impossible to upgrade Java on Android without os upgrade. 
but there are still possibility for gdx backend to reappear in future.