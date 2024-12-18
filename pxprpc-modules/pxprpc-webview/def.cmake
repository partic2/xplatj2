
if (NOT ANDROID)
add_subdirectory(${CMAKE_CURRENT_LIST_DIR}/webview "./c-webview")
# param 1 : the entry function
# param 2 : the source file to be included
pxprpc_rtbridge_add_module(pxprpc_webview::init pxprpc_webview.hpp)

pxprpc_rtbridge_add_link_library(webview_core_static webview_core_headers)

endif()

