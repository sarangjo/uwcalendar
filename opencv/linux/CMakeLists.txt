cmake_minimum_required(VERSION 3.1)

project( ScheduleCV )
set( CMAKE_CXX_STANDARD 11 )
set( CMAKE_RUNTIME_OUTPUT_DIRECTORY bin/ )

find_package( OpenCV REQUIRED )
include_directories( ${OpenCV_INCLUDE_DIRS} include/ )

add_executable( ScheduleCV src/main.cpp src/HourHeight.cpp src/OCR.cpp src/Tools.cpp )
target_compile_options( ScheduleCV PRIVATE -g )
target_link_libraries( ScheduleCV ${OpenCV_LIBS} )

add_custom_target( run bin/ScheduleCV ../testdata/ )
add_custom_target( debug gdb bin/ScheduleCV )
