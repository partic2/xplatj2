
if (NOT ANDROID)
add_subdirectory(${DEPS_SOURCE_DIRS}/webview "./c-webview" EXCLUDE_FROM_ALL)
# param 1 : the entry function
# param 2 : the source file to be included
pxprpc_rtbridge_add_module(pxprpc_webview::init init.hpp)

pxprpc_rtbridge_add_link_library(webview_core_static webview_core_headers)

endif()

