cmake_minimum_required(VERSION 3.15)

if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif()



project(xplat-launcher)


add_subdirectory(../SDL ./build-sdl)
add_subdirectory(../tinycc-build ./build-tcc)


if(ANDROID)
add_library(SDLLoader SHARED SDLLoader.c)
SET(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY BOTH)
else()
add_executable(launcher launcher.c)
add_executable(SDLLoader SDLLoader.c)
endif()




if(WIN32)
    list(APPEND link_list mingw32)
endif()


list(APPEND link_list tcc SDL2main SDL2)

target_link_libraries(SDLLoader ${link_list})


