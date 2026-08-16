cmake_minimum_required(VERSION 3.10)
project(HexAudioEngine VERSION 1.0)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)

# Define the library source files
set(SOURCES
    src/HexMath.cpp
    src/DSP.cpp
    src/HexEngine.cpp
)

# Add the library
add_library(HexAudioEngine STATIC ${SOURCES})

# Specify include directories
target_include_directories(HexAudioEngine PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

# Optional: Math library if needed on some platforms
target_link_libraries(HexAudioEngine m)

# Build the test harness executable
add_executable(hex-engine-test src/main.cpp)
target_link_libraries(hex-engine-test PRIVATE HexAudioEngine m)
target_include_directories(hex-engine-test PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)
