# param 1 : the entry function
# param 2 : the source file to be included
if(TARGET SDL2)

pxprpc_rtbridge_add_module(xplat_sdlloader::init init.hpp)
pxprpc_rtbridge_add_link_library(SDL2 SDL2main pwart_syslib pwart tjs)

endif()