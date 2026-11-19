
if(NOT TARGET widcc_jit)
    add_subdirectory("${DEPS_SOURCE_DIRS}/widcc-jit" "./build-widcc-jit" EXCLUDE_FROM_ALL)
endif()
# param 1 : the entry function
# param 2 : the source file to be included
pxprpc_rtbridge_add_module(pxprpc_widcc_jit::init init.hpp)
pxprpc_rtbridge_add_link_library(widcc_jit)
