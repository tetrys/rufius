project(rufius)
cmake_minimum_required(VERSION 2.8)

include(${CMAKE_SOURCE_DIR}/CMakeFunctions.cmake)

add_subdirectory(motion motion)

include_directories(${PROJECT_SOURCE_DIR}/include/)
include_directories(${PROJECT_SOURCE_DIR}/motion/include/)

file(GLOB SOURCE_FILES "${PROJECT_SOURCE_DIR}/src/*.cxx")
file(GLOB HEADER_FILES "${PROJECT_SOURCE_DIR}/include/*.h" "${PROJECT_SOURCE_DIR}/motion/include/motion.h")

initDefinitions()

set(rufius_tmp "\"/tmp/rufius/\"")
set(rufius_socket "\"/tmp/rufius/rufius.socket\"")
set(rufius_default "\"${CMAKE_INSTALL_PREFIX}\"")
set(conf_default "\"${CMAKE_INSTALL_PREFIX}/conf/rufius.conf\"")
set(motion_conf_default "\"${CMAKE_INSTALL_PREFIX}/conf/motion.conf\"")
set(pass_default "\"${CMAKE_INSTALL_PREFIX}/conf/rufius.pswd\"")
set(key_default "\"${CMAKE_INSTALL_PREFIX}/conf/rufius.key\"")
set(nameserver_default "\"${CMAKE_INSTALL_PREFIX}/conf/rufius.nmsr\"")
set(log_default "\"${CMAKE_INSTALL_PREFIX}/conf/rufius.log\"")
set(snapshots_default "\"${CMAKE_INSTALL_PREFIX}/snapshots/\"")
set(triggers_default "\"${CMAKE_INSTALL_PREFIX}/triggers/\"")

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

add_executable(${PROJECT_NAME} ${SOURCE_FILES} ${HEADER_FILES} main.cxx)

add_dependencies(${PROJECT_NAME} motion)

target_link_libraries(${PROJECT_NAME} m resolv dl pthread crypt nsl jpeg z avcodec avformat avutil swresample curl crypto ssl motion ldns)

set_target_properties(${PROJECT_NAME}
    PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin"
)

install(TARGETS rufius DESTINATION /opt/rufius/)
