#include <jni.h>
#include <string>
#include <backends/oxorany.h>
#include <sys/stat.h>
#include "backends/ModsLoader.h"

extern "C" JNIEXPORT jstring JNICALL 
Java_com_mk_server_BoxApplication_BoxApp(JNIEnv* env, jobject thiz) {
    return env->NewStringUTF(oxorany("RIYAZ-8826061C-931"));
}
//D0HWLH06K7FF