#include <jni.h>
#include <string>

#include <android/log.h>

// extern "C" 는 함수의 이름이 변형되지 않음을 보장해줌
// jstring 은 java string의 포인터를 가르키는 포인터 타입
extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_example_androidndk_MainActivity_stringFromJNI( // MainActivity의 절대경로_사용할 함수 이름
            JNIEnv *env, // Virtual Machine을 가르키는 포인
            jobject /* this */) { // jobject는 자바에서 this를 가르키는 포인터이다.
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str()); // virtual machine에 string을 return
    }

    JNIEXPORT void JNICALL
    Java_com_example_androidndk_MainActivity_ArrayLogTest(
            JNIEnv *env,
            jobject,
            jobject testArr
            ){
        // java.util.ArrayList class를 가져옴
//        jclass java_util_ArrayList = static_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
        jclass java_util_ArrayList = env->GetObjectClass(testArr);

        // 정의한 Class, 함수 이름, (함수의 인자)return값 //jni의 sig정보 참조
        jmethodID java_util_ArrayList_size = env->GetMethodID(java_util_ArrayList,"size","()I");

        jint length = env->CallIntMethod(testArr,java_util_ArrayList_size);

        __android_log_print(ANDROID_LOG_INFO,"TEST","array size is %d",length);
    }
}