cmake_minimum_required(VERSION 3.15)

set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_CMAKE_SHARED_LINKER_FLAGS_RELEASE} -s")
set(CMAKE_EXE_LINKER_FLAGS_RELEASE "${CMAKE_EXE_LINKER_FLAGS_RELEASE} -s")

if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif()

set(CMAKE_POLICY_DEFAULT_CMP0077 NEW)

project(xplat-launcher)

set(BUILD_TESTING OFF)

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

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

if("${DEPS_SOURCE_DIRS}" STREQUAL "")
  if(NOT ("$ENV{DEPS_SOURCE_DIRS}" STREQUAL ""))
    set(DEPS_SOURCE_DIRS $ENV{DEPS_SOURCE_DIRS})
  else()
    get_filename_component(DEPS_SOURCE_DIRS ../deps ABSOLUTE)
  endif()
endif()


if(NOT TARGET uv_a)
add_subdirectory("${DEPS_SOURCE_DIRS}/libuv" ./build-libuv EXCLUDE_FROM_ALL)
endif()

set(PXPRPC_RTBRIDGE_BUILD_EXE OFF)

add_executable(launcher launcher.c)

add_subdirectory(${DEPS_SOURCE_DIRS}/SDL ./build-sdl EXCLUDE_FROM_ALL)
add_subdirectory(${DEPS_SOURCE_DIRS}/pwart ./build-pwart EXCLUDE_FROM_ALL)
add_subdirectory(${DEPS_SOURCE_DIRS}/txiki.js ./build-txiki.js EXCLUDE_FROM_ALL)

list(APPEND PXPRPC_RTBIRDGE_MODULES_DIRS ${CMAKE_CURRENT_LIST_DIR}/../pxprpc-modules)
add_subdirectory(${DEPS_SOURCE_DIRS}/PxpRpc/runtime_bridge ./build-pxprpc_rtbridge)




