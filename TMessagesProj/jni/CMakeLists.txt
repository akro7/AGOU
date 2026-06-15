cmake_minimum_required(VERSION 3.22.1)
project(tgnet)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -w")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -w -frtti -fexceptions")

# ──────────────────────────────────────────────────────────────────────────────
# BoringSSL  (يضيفه المستخدم يدوياً من الريبو الأصلي)
# ──────────────────────────────────────────────────────────────────────────────
set(BORINGSSL_DIR "${CMAKE_CURRENT_SOURCE_DIR}/boringssl")

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
    "${BORINGSSL_DIR}/lib/${ANDROID_ABI}/libcrypto.a")

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
    "${BORINGSSL_DIR}/lib/${ANDROID_ABI}/libssl.a")

# ──────────────────────────────────────────────────────────────────────────────
# tde2e / tdutils  (prebuilt)
# ──────────────────────────────────────────────────────────────────────────────
add_library(tde2e STATIC IMPORTED)
set_target_properties(tde2e PROPERTIES IMPORTED_LOCATION
    "${CMAKE_CURRENT_SOURCE_DIR}/tde2e/${ANDROID_ABI}/libtde2e.a")

add_library(tdutils STATIC IMPORTED)
set_target_properties(tdutils PROPERTIES IMPORTED_LOCATION
    "${CMAKE_CURRENT_SOURCE_DIR}/tde2e/${ANDROID_ABI}/libtdutils.a")

# ──────────────────────────────────────────────────────────────────────────────
# SQLite  (source)
# ──────────────────────────────────────────────────────────────────────────────
add_library(sqlite STATIC
    sqlite/sqlite3.c
)
target_include_directories(sqlite PUBLIC sqlite)
target_compile_options(sqlite PRIVATE -DSQLITE_DEFAULT_WAL_SYNCHRONOUS=1
    -DSQLITE_ENABLE_FTS5 -DSQLITE_ENABLE_RTREE
    -DSQLITE_OMIT_LOAD_EXTENSION)

# ──────────────────────────────────────────────────────────────────────────────
# tgnet source files
# ──────────────────────────────────────────────────────────────────────────────
file(GLOB TGNET_SOURCES
    "tgnet/*.cpp"
    "tgnet/*.c"
)

# ──────────────────────────────────────────────────────────────────────────────
# Main shared library
# ──────────────────────────────────────────────────────────────────────────────
add_library(tgnet SHARED
    ${TGNET_SOURCES}
    TgNetWrapper.cpp
    jni.c
    akro_stubs.c
)

target_include_directories(tgnet PRIVATE
    tgnet
    tde2e/include
    ${BORINGSSL_DIR}/include
    sqlite
)

target_compile_definitions(tgnet PRIVATE
    ANDROID
    HAVE_PTHREAD
    # ستيلث: نعطّل التقارير اللي ممكن تكشف الـ last-seen
    AKROGRAM_STEALTH=1
)

target_link_libraries(tgnet
    sqlite
    tde2e
    tdutils
    ssl
    crypto
    android
    log
    z
    atomic
)
