# param 1 : the entry function
# param 2 : the source file to be included
if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
pxprpc_rtbridge_add_module(pxprpc_win32helpers::init init.hpp)
#Needed because MinGW's _mingw.h otherwise defaults to 0x0502, hiding
#Vista+ APIs such as QueryFullProcessImageNameW and SHGetKnownFolderPath.
pxprpc_rtbridge_add_compile_definition(_WIN32_WINNT=0x0600 WINVER=0x0600)
pxprpc_rtbridge_add_link_library(gdi32 user32 shell32 advapi32 comdlg32 dwmapi ole32 uuid)
endif()