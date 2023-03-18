
cmake_minimum_required(VERSION 3.15)

project(tinycc)

file(WRITE ../tinycc/config.h "#define TCC_VERSION \"0.9.27\"")

add_library(tcc STATIC ../tinycc/libtcc.c)

target_compile_definitions(tcc PRIVATE TCC_TARGET_NATIVE_BUILD=1 ONE_SOURCE=1)

file(COPY ../tinycc/libtcc.h DESTINATION ${CMAKE_BINARY_DIR}/include/)

target_include_directories(tcc INTERFACE ${CMAKE_BINARY_DIR}/include)