
if(NOT TARGET pwart)
add_subdirectory(${DEPS_SOURCE_DIRS}/pwart "./build-pwart" EXCLUDE_FROM_ALL)
endif()

# param 1 : the entry function
# param 2 : the source file to be included
pxprpc_rtbridge_add_module(pxprpc_pwart::init init.hpp)

pxprpc_rtbridge_add_link_library(pwart)


