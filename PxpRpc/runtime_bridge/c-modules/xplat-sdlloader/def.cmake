# param 1 : the entry function
# param 2 : the source file to be included
pxprpc_rtbridge_add_module(xplat_sdlloader::init xplat-sdlloader.hpp)

pxprpc_rtbridge_add_link_library(SDL2 pwart_syslib pwart)