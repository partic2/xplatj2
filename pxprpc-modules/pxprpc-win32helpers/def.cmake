

# param 1 : the entry function
# param 2 : the source file to be included
if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
pxprpc_rtbridge_add_module(pxprpc_win32helpers::init init.hpp)
pxprpc_rtbridge_add_link_library(Gdi32)
endif()
#pxprpc_rtbridge_add_link_library(uvpp)



