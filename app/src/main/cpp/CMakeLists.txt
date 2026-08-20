cmake_minimum_required(VERSION 3.22.1)

project("hex-audio-jni")

set(CMAKE_CXX_STANDARD 17)

# Include the external core engine
add_subdirectory(${CMAKE_SOURCE_DIR}/../../../../hex-audio-engine ${CMAKE_BINARY_DIR}/hex-audio-engine)

# Add Oboe from prefab
find_package(oboe REQUIRED CONFIG)

# Create the JNI wrapper library
add_library(
    hex-audio-jni
    SHARED
    jni_bridge.cpp
)

# Link the core engine, Oboe, Android logging, and JNI
target_link_libraries(
    hex-audio-jni
    HexAudioEngine
    oboe::oboe
    log
)

target_include_directories(hex-audio-jni PRIVATE ${CMAKE_SOURCE_DIR}/../../../../hex-audio-engine/include)
