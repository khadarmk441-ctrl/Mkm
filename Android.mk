LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libcurl
LOCAL_SRC_FILES := backends/external/curl-android-$(TARGET_ARCH_ABI)/lib/libcurl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libssl
LOCAL_SRC_FILES := backends/external/openssl-android-$(TARGET_ARCH_ABI)/lib/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libcrypto
LOCAL_SRC_FILES := backends/external/openssl-android-$(TARGET_ARCH_ABI)/lib/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := MCoreEsp

# 🔥 SOURCE FILES - main.cpp में admin key code add किया है
LOCAL_SRC_FILES :=  main.cpp \
           backends/oxorany.cpp

LOCAL_C_INCLUDES := backends/external/curl-android-$(TARGET_ARCH_ABI)/include
LOCAL_C_INCLUDES += backends/external/openssl-android-$(TARGET_ARCH_ABI)/include

# 🔥 COMPILER FLAGS - optimization and security
LOCAL_CFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -std=c++17 -O2
LOCAL_CPPFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -std=c++17 -frtti -fexceptions -O2
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all
LOCAL_ARM_MODE := arm

LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_LDLIBS := -llog -landroid -lz -ldl

LOCAL_STATIC_LIBRARIES := libcurl libssl libcrypto

include $(BUILD_SHARED_LIBRARY)