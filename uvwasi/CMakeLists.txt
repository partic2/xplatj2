cmake_minimum_required(VERSION 3.11)
project (uvwasi LANGUAGES C)

# Point CMake at any custom modules we may ship
list(APPEND CMAKE_MODULE_PATH "${PROJECT_SOURCE_DIR}/cmake")

# This can be a commit hash or tag
set(LIBUV_VERSION v1.44.2)

include(CMakeDependentOption)
cmake_dependent_option(UVWASI_BUILD_TESTS
  "Build the unit tests when uvwasi is the root project" ON
  "CMAKE_SOURCE_DIR STREQUAL PROJECT_SOURCE_DIR" OFF)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/out)

if(CMAKE_C_COMPILER_ID MATCHES "AppleClang|Clang|GNU")
  list(APPEND uvwasi_cflags -fvisibility=hidden --std=gnu89)
  list(APPEND uvwasi_cflags -Wall -Wsign-compare -Wextra -Wstrict-prototypes)
  list(APPEND uvwasi_cflags -Wno-unused-parameter)
endif()

if(APPLE)
   set(CMAKE_MACOSX_RPATH ON)
endif()

if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
  list(APPEND uvwasi_defines _GNU_SOURCE _POSIX_C_SOURCE=200112)
endif()

option(WITH_SYSTEM_LIBUV "Link to a system libuv library instead of bundling" OFF)

if (WITH_SYSTEM_LIBUV)
  find_package(LibUV REQUIRED)
  include_directories(${LIBUV_INCLUDE_DIR})
else (WITH_SYSTEM_LIBUV)
  include(FetchContent)
  ## https://libuv.org
  FetchContent_Declare(
          libuv
          GIT_REPOSITORY https://github.com/libuv/libuv.git
          GIT_TAG ${LIBUV_VERSION})

  FetchContent_GetProperties(libuv)
  # We use embeded libuv
  set(libuv_POPULATED ON)
  if(NOT libuv_POPULATED)
      FetchContent_Populate(libuv)
      include_directories("${libuv_SOURCE_DIR}/include")
      add_subdirectory(${libuv_SOURCE_DIR} ${libuv_BINARY_DIR} EXCLUDE_FROM_ALL)
  endif()
  set(LIBUV_LIBRARIES uv_a)
endif (WITH_SYSTEM_LIBUV)

## uvwasi source code files.
set(uvwasi_sources
    src/clocks.c
    src/fd_table.c
    src/path_resolver.c
    src/poll_oneoff.c
    src/uv_mapping.c
    src/uvwasi.c
    src/wasi_rights.c
    src/wasi_serdes.c
)

option(UVWASI_DEBUG_LOG "Enable debug logging" OFF)
if(UVWASI_DEBUG_LOG)
    list(APPEND uvwasi_cflags -DUVWASI_DEBUG_LOG)
endif()

# Code Coverage Configuration
add_library(coverage_config INTERFACE)

option(CODE_COVERAGE "Enable coverage reporting" OFF)
if(CODE_COVERAGE AND CMAKE_C_COMPILER_ID MATCHES "AppleClang|GNU|Clang")
    # Add required flags (GCC & LLVM/Clang)
    target_compile_options(coverage_config INTERFACE
        -O0        # no optimization
        -g         # generate debug info
        --coverage # sets all required flags
    )
    if(CMAKE_VERSION VERSION_GREATER_EQUAL 3.13)
        target_link_options(coverage_config INTERFACE --coverage)
    else()
        target_link_libraries(coverage_config INTERFACE --coverage)
    endif()
endif()

# ASAN Support
option(ASAN "Enable code asan" OFF)
if(ASAN AND CMAKE_C_COMPILER_ID MATCHES "AppleClang|GNU|Clang")
    set (CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -fno-omit-frame-pointer -fsanitize=address")
    set (CMAKE_LINKER_FLAGS_DEBUG "${CMAKE_LINKER_FLAGS_DEBUG} -fno-omit-frame-pointer -fsanitize=address")
endif()

## Static library target.
add_library(uvwasi_a STATIC ${uvwasi_sources})
target_compile_definitions(uvwasi_a PRIVATE ${uvwasi_defines})
target_compile_options(uvwasi_a PRIVATE ${uvwasi_cflags})
target_include_directories(uvwasi_a PRIVATE ${PROJECT_SOURCE_DIR}/include)
if(CODE_COVERAGE)
    target_link_libraries(uvwasi_a PUBLIC ${LIBUV_LIBRARIES} coverage_config)
else()
    target_link_libraries(uvwasi_a PRIVATE ${LIBUV_LIBRARIES})
endif()

## Shared library target.
#add_library(uvwasi SHARED ${uvwasi_sources})
#target_compile_definitions(uvwasi PRIVATE ${uvwasi_defines})
#target_compile_options(uvwasi PRIVATE ${uvwasi_cflags})
#target_include_directories(uvwasi PRIVATE ${PROJECT_SOURCE_DIR}/include)
#if(CODE_COVERAGE)
#    target_link_libraries(uvwasi PUBLIC ${LIBUV_LIBRARIES} coverage_config)
#else()
#    target_link_libraries(uvwasi PRIVATE ${LIBUV_LIBRARIES})
#endif()


## Test targets.
if(UVWASI_BUILD_TESTS)
    enable_testing()
    file(GLOB test_files "test/test-*.c")
    foreach(file ${test_files})
        get_filename_component(test_name ${file} NAME_WE)
        add_executable(${test_name} ${file})
        add_test(NAME ${test_name}
                    COMMAND ${test_name})
        target_include_directories(${test_name}
                                    PRIVATE
                                    ${PROJECT_SOURCE_DIR}/include)
        target_link_libraries(${test_name} PRIVATE ${LIBUV_LIBRARIES} uvwasi_a)
        list(APPEND test_list ${test_name})
    endforeach()

    add_custom_target(check
        COMMAND ctest -VV -C Debug -R test-
        DEPENDS ${test_list}
    )
endif()

message(STATUS "summary of uvwasi build options:

    Install prefix:  ${CMAKE_INSTALL_PREFIX}
    Target system:   ${CMAKE_SYSTEM_NAME}
    Compiler:
      C compiler:    ${CMAKE_C_COMPILER}
      CFLAGS:        ${CMAKE_C_FLAGS_${_build_type}} ${CMAKE_C_FLAGS}

    System libuv:    ${WITH_SYSTEM_LIBUV}
    Debug logging:   ${UVWASI_DEBUG_LOG}
    Code coverage:   ${CODE_COVERAGE}
    ASAN:            ${ASAN}
    Build tests:     ${UVWASI_BUILD_TESTS}
")
