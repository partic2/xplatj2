# XPLATJ2

XPLATJ is a trial project aim to make a cross-platform layer to create application. Now this is a loader of pxseed(https://github.com/partic2/pxseed-CorePackages)


## How to build

### PREREQUIRE
```sh 
C Compiler(GCC/CLANG/MINGW) 
CMake 
NodeJS 
Git 

#If you want to build Android Release
Android SDK 
Android NDK 

#NOTE: libSDL may require extra depdendencies on linux.
```
### STEP 1

```sh
export CC="Your C compiler"
export CXX="Your C++ compiler"
#If you want to build Android Release.
export ANDROID_HOME="Your android SDK path."
```

### STEP 2

```sh
npm i -g pxseed-cli
pxseed-cli "await (await import('partic2/packageManager/pxseedloaderbuilder')).defaultBuild()"
```


Then 
You can find the release in ${SOURCE_DIR_ROOT}/launcher/build/*

