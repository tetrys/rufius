project(motion)
cmake_minimum_required(VERSION 2.8)


include(${CMAKE_SOURCE_DIR}/CMakeFunctions.cmake)

include_directories(${PROJECT_SOURCE_DIR}/include/ ${CMAKE_SOURCE_DIR}/include/)

file(GLOB SOURCE_FILES "${PROJECT_SOURCE_DIR}/src/*.cxx")
file(GLOB HEADER_FILES "${PROJECT_SOURCE_DIR}/include/*.h" "${CMAKE_SOURCE_DIR}/include/Rufius_Motion.h")

initDefinitions()

set(PACKAGE_NAME "\"rufius-motion\"")
set(PACKAGE_STRING "\"rufius-motion 0.01\"")
set(PACKAGE_TARNAME "\"rufius-motion\"")
set(VERSION "\"0.01\"")
set(sysconfdir "\"/opt/rufius/conf/\"")
set(TYPE_32BIT uint32_t)

if(CMAKE_BUILD_TYPE MATCHES DEBUG)
    add_definitions(-DQT_DEBUG)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wno-write-strings")
    message("debug mode")
elseif(CMAKE_BUILD_TYPE MATCHES RELEASE)
    add_definitions(-DQT_RELEASE)
    message("release mode")
endif(CMAKE_BUILD_TYPE MATCHES DEBUG)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -v")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -pedantic-errors")
set(CMAKE_VERBOSE_MAKEFILE ON)
set(CMAKE_COLOR_MAKEFILE ON)

configure_file(${PROJECT_SOURCE_DIR}/config.h.in ${PROJECT_SOURCE_DIR}/include/config.h)

add_library(${PROJECT_NAME} STATIC ${SOURCE_FILES} ${HEADER_FILES})

target_link_libraries(${PROJECT_NAME} m resolv dl pthread crypt nsl jpeg z avcodec avformat avutil swresample)

set_target_properties(${PROJECT_NAME}
    PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/lib/"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/lib/"
)
