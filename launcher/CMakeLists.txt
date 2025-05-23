cmake_minimum_required(VERSION 3.15)

set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_CMAKE_SHARED_LINKER_FLAGS_RELEASE} -s")
set(CMAKE_EXE_LINKER_FLAGS_RELEASE "${CMAKE_EXE_LINKER_FLAGS_RELEASE} -s")

if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif()

set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)

project(xplat-launcher)

set(BUILD_TESTING OFF)

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
get_filename_component(DEPS_SOURCE_DIRS ../deps ABSOLUTE)

if(NOT TARGET uv_a)
add_subdirectory("${DEPS_SOURCE_DIRS}/libuv" ./build-libuv EXCLUDE_FROM_ALL)
endif()

set(PXPRPC_RTBRIDGE_BUILD_EXE OFF)

if(ANDROID)
add_library(SDLLoader SHARED SDLLoader.c)
SET(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
else()
add_executable(SDLLoader SDLLoader.c)
endif()

add_executable(launcher launcher.c)

add_subdirectory(../deps/SDL ./build-sdl EXCLUDE_FROM_ALL)
add_subdirectory(../deps/pwart ./build-pwart EXCLUDE_FROM_ALL)

list(APPEND PXPRPC_RTBIRDGE_MODULES_DIRS ${DEPS_SOURCE_DIRS}/pxprpc-modules)
add_subdirectory(../deps/PxpRpc/runtime_bridge ./build-pxprpc_rtbridge)
target_link_libraries(SDLLoader SDL2main SDL2)






