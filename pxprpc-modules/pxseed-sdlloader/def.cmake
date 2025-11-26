# param 1 : the entry function
# param 2 : the source file to be included
if(TARGET SDL3-shared)

pxprpc_rtbridge_add_module(pxseed_sdlloader::init init.hpp)
pxprpc_rtbridge_add_link_library(SDL3-shared pwart_syslib pwart tjs)

endif()