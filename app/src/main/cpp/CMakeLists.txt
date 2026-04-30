cmake_minimum_required(VERSION 3.22.1)
project("lokai")

# ─── llama.cpp submodule ───────────────────────────────────────────────────────
set(LLAMA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")

# Disable everything we don't need
set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS    OFF CACHE BOOL "" FORCE)

# Android CPU-only — no Vulkan, no Metal, no CUDA
set(LLAMA_VULKAN  OFF CACHE BOOL "" FORCE)
set(LLAMA_METAL   OFF CACHE BOOL "" FORCE)
set(LLAMA_CUDA    OFF CACHE BOOL "" FORCE)
set(LLAMA_OPENCL  OFF CACHE BOOL "" FORCE)

# ARM NEON acceleration (available on arm64-v8a and armeabi-v7a)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(LLAMA_ARM_NEON ON CACHE BOOL "" FORCE)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(LLAMA_ARM_NEON ON CACHE BOOL "" FORCE)
endif()

add_subdirectory(${LLAMA_DIR} llama_build)

# ─── JNI bridge ───────────────────────────────────────────────────────────────
add_library(
    lokai_jni
    SHARED
    lokai_jni.cpp
)

target_include_directories(lokai_jni PRIVATE
    ${LLAMA_DIR}
    ${LLAMA_DIR}/common
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/ggml/include
)

target_link_libraries(lokai_jni
    llama
    ggml
    android
    log
)

target_compile_options(lokai_jni PRIVATE
    -std=c++17
    -O3
    -DNDEBUG
    -fvisibility=hidden
)
