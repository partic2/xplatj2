

#Only libffi need below detect, maybe refact libffi will be better.

message("Guess cross compiling variable for compiler ${CMAKE_C_COMPILER}")

include(CheckSymbolExists)
set(CMAKE_SYSTEM_PROCESSOR "")
set(CMAKE_C_ENABLE)
if (CMAKE_SYSTEM_PROCESSOR STREQUAL "")
    check_symbol_exists(__x86_64__ stdio.h CHECK_COMPILER_DEFINITION___x86_64__)
    if (CHECK_COMPILER_DEFINITION___x86_64__)
    set(CMAKE_SYSTEM_PROCESSOR x86_64)
    endif()
endif()

if (CMAKE_SYSTEM_PROCESSOR STREQUAL "")
    check_symbol_exists(__i386__ stdio.h CHECK_COMPILER_DEFINITION___i386__)
    if (CHECK_COMPILER_DEFINITION___i386__)
    set(CMAKE_SYSTEM_PROCESSOR x86)
    endif()
endif()

if (CMAKE_SYSTEM_PROCESSOR STREQUAL "")
    check_symbol_exists(__arm__ stdio.h CHECK_COMPILER_DEFINITION___arm__)
    if (CHECK_COMPILER_DEFINITION___arm__)
    set(CMAKE_SYSTEM_PROCESSOR arm)
    endif()
endif()

if (CMAKE_SYSTEM_PROCESSOR STREQUAL "")
    check_symbol_exists(__aarch64__ stdio.h CHECK_COMPILER_DEFINITION___aarch64__)
    if (CHECK_COMPILER_DEFINITION___aarch64__)
    set(CMAKE_SYSTEM_PROCESSOR aarch64)
    endif()
endif()

#riscv bit length need detect, So how?
#check_symbol_exists(__riscv_xlen stdio.h CHECK_COMPILER_DEFINITION___riscv_xlen)

set(CMAKE_SYSTEM_NAME_GUESS "")

if (CMAKE_SYSTEM_NAME_GUESS STREQUAL "")
    check_symbol_exists(__linux__ stdio.h CHECK_COMPILER_DEFINITION___linux__)
    if(CHECK_COMPILER_DEFINITION___linux__)
    set(CMAKE_SYSTEM_NAME_GUESS Linux)
    endif()
endif()

if (CMAKE_SYSTEM_NAME_GUESS STREQUAL "")
    check_symbol_exists(_WIN32 stdio.h CHECK_COMPILER_DEFINITION__WIN32)
    if(CHECK_COMPILER_DEFINITION__WIN32)
    set(CMAKE_SYSTEM_NAME_GUESS Windows)
    endif()
endif()

if (NOT CMAKE_SYSTEM_NAME_GUESS STREQUAL "")
    set(CMAKE_SYSTEM_NAME ${CMAKE_SYSTEM_NAME_GUESS})
endif()


