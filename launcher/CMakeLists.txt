cmake_minimum_required(VERSION 3.15)

if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif()

set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)

project(xplat-launcher)



message("!!!Compiling Note!!!")
message("If you failed to compile with libffi, you can rename/remove \"./libffi\" directory to diable libffi relative feature.")

#Seemed shared object on linux require this options to build shared object.
if(CMAKE_SYSTEM_NAME STREQUAL Linux)
set(CMAKE_POSITION_INDEPENDENT_CODE TRUE)
endif()

if(XPLATJ_GUESS_TOOLCHAIN_VARIABLE)
include(guess_by_compiler.toolchain.cmake)
endif()

set(PWART_SYSLIB_ENABLED ON)
get_filename_component(DEPS_SOURCE_DIRS ../ ABSOLUTE)

if(NOT TARGET uv_a)
add_subdirectory("${DEPS_SOURCE_DIRS}/libuv" ./build-libuv)
endif()

set(PXPRPC_RTBRIDGE_BUILD_EXE OFF)

if(ANDROID)
add_library(SDLLoader SHARED SDLLoader.c)
SET(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
else()
add_executable(SDLLoader SDLLoader.c)
add_executable(launcher launcher.c)
endif()

add_subdirectory(../SDL ./build-sdl)
add_subdirectory(../pwart ./build-pwart)
#xplat_sdlloader_start is in PxpRpc/runtime_bridge/c-modules/xplat-sdlloader
add_subdirectory(../PxpRpc/runtime_bridge ./build-pxprpc_rtbridge)
target_link_libraries(SDLLoader SDL2main SDL2)






