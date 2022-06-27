//
// Created by 김기태 on 2022/06/21.
//

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h> // 비트맵 메모리에 접근
#include "convertUtils.h"

char* str[4] = {"Original", "Gray", "Light", "Dark"};

extern "C" {
JNIEXPORT jobjectArray JNICALL
Java_com_example_androidndk_MainActivity_GetConvertList(JNIEnv *env, jobject) {
    jobjectArray list= (jobjectArray)env->NewObjectArray(
            4,
            env->FindClass("java/lang/String"),
            env->NewStringUTF(""));
    for (int i = 0; i < 4; i++)
        env->SetObjectArrayElement(list,i,env->NewStringUTF(str[i]));
    return list;
}

JNIEXPORT void JNICALL
Java_com_example_androidndk_MainActivity_bitmapToGrayScale(
        JNIEnv *env,
        jobject,
        jobject
        bitmap
) {
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        // fail
        __android_log_print(ANDROID_LOG_ERROR,"bitmapInfoErr", "bitmap info error");
    }

    //        __android_log_print(ANDROID_LOG_INFO, "bitmapInfo","bitmap stride = %d",bitmapInfo.stride);
    // 픽셀 버퍼를 잠그고 버퍼의 포인터를 할당함
    // byte단위로 버퍼가 들어있음 -> 8bit 짜리 주소값으로 접근함
    // bitmap 픽셀들은 byte단위로 메모리에 저장됨 -> 8bit의 포인터로
    uint8_t *pbmp = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap,(void **) &pbmp) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmaplockErr", "bitmap lock error");
    }

    for (int row = 0; row < bitmapInfo.height; row++) {
        uint8_t *pixel = pbmp + (row * bitmapInfo.stride);
        for (int col = 0; col < bitmapInfo.width; col++) {
            uint8_t gray = (uint8_t) (((float) pixel[0] * 0.299f) + ((float) pixel[1] * 0.587f) + ((float) pixel[2] * 0.114f));
            pixel[0] =gray;
            pixel[1] =gray;
            pixel[2] =gray;

            pixel += 4;
        }
    }
    if (AndroidBitmap_unlockPixels(env, bitmap) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmapUnlockErr", "bitmap unlock error");
    }
}

JNIEXPORT void JNICALL
Java_com_example_androidndk_MainActivity_bitmapBright(
        JNIEnv *env,
        jobject,
        jobject bitmap,
        jdouble val
) {
    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        // fail
        __android_log_print(ANDROID_LOG_ERROR,"bitmapInfoErr", "bitmap info error");
        return;
    }

    //        __android_log_print(ANDROID_LOG_INFO, "bitmapInfo","bitmap stride = %d",bitmapInfo.stride);
    // 픽셀 버퍼를 잠그고 버퍼의 포인터를 할당함
    // byte배열을 표현함
    uint8_t *pbmp = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap,(void **) &pbmp) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmaplockErr", "bitmap lock error");
        return;
    }

    for(int i = 0; i < bitmapInfo.height*bitmapInfo.width*4; i += 4){
        if(*(pbmp+i+0) > 210 || *(pbmp+i+0) < 15) continue;
        hsv tmp = rgb2hsv( rgb ((double )*(pbmp+i+0)/255.0, (double )*(pbmp+i+1)/255.0,(double )*(pbmp+i+2)/255.0));
        if(tmp.v > 0.0-val && tmp.v < 1.0-val) tmp.v += val;
        rgb result = hsv2rgb(tmp);

        *(pbmp+i+0) = (uint8_t)(result.r*255.0);
        *(pbmp+i+1) = (uint8_t)(result.g*255.0);
        *(pbmp+i+2) = (uint8_t)(result.b*255.0);
    }

    if (AndroidBitmap_unlockPixels(env, bitmap) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmapUnlockErr", "bitmap unlock error");
    }
}


/**
 * Details
 * The ssd_mobilenet_v1_1_metadata_1.
 * tflite file's input takes normalized 300x300x3 shape image.
 * */
JNIEXPORT jbyteArray JNICALL
Java_com_example_androidndk_tensorFlow_CameraFragment_bitmapToByteArray(
        JNIEnv *env,
        jobject,
        jobject bitmap,
        jint w,jint h,jint targetW,jint targetH
){
    double ratioW = (double)w/(double )targetW;
    double ratioH = (double )h/(double )targetH;

    jbyteArray result = env->NewByteArray(targetW*targetH*3);
    jbyte *resultP = env->GetByteArrayElements(result,NULL);

    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
        // fail
        __android_log_print(ANDROID_LOG_ERROR,"bitmapInfoErr", "bitmap info error");
        return result;
    }

    uint8_t *pbmp = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap,(void **) &pbmp) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmaplockErr", "bitmap lock error");
        return result;
    }

    // for normalization
    uint8_t R=0,G=0,B=0; // MAX
    uint8_t r=255,g=255,b=255; // MIN
    for(int row = 0; row < targetH; row++){
        uint8_t *pixel = pbmp + ((int)((double )row*ratioH) * bitmapInfo.stride);
        for(int col = 0; col < targetW; col++){
            uint8_t *tmp = pixel + (int)((double )col*ratioW)*4;
            tmp[0] = tmp[0] > R ? tmp[0] : R;
            tmp[1] = tmp[1] > G ? tmp[1] : G;
            tmp[2] = tmp[2] > B ? tmp[2] : B;
            tmp[0] = tmp[0] < r ? tmp[0] : r;
            tmp[1] = tmp[1] < g ? tmp[1] : g;
            tmp[2] = tmp[2] < b ? tmp[2] : b;
        }
    }
    uint8_t ratioR = (uint8_t)(255.0/(double )(R-r+1));
    uint8_t ratioG = (uint8_t)(255.0/(double )(G-g+1));
    uint8_t ratioB = (uint8_t)(255.0/(double )(B-b+1));

    for(int row = 0; row < targetH; row++){
        uint8_t *pixel = pbmp + ((int)((float)row*ratioH) * bitmapInfo.stride);
        for(int col = 0; col < targetW; col++){
            uint8_t *tmp = pixel + (int)((float )col*ratioW)*4;
            uint8_t *targetP = reinterpret_cast<uint8_t *>(resultP + targetW * row + col * 3);
            targetP[0] = (tmp[0]-r)*ratioR;
            targetP[1] = (tmp[1]-g)*ratioG;
            targetP[2] = (tmp[2]-b)*ratioB;
//            resultP[targetW*row + col*3 + 0] = tmp[0];
//            resultP[targetW*row + col*3 + 1] = tmp[1];
//            resultP[targetW*row + col*3 + 2] = tmp[2];
        }
    }
    env->ReleaseByteArrayElements(result,resultP,0);

    if (AndroidBitmap_unlockPixels(env, bitmap) != ANDROID_BITMAP_RESULT_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR,"bitmapUnlockErr", "bitmap unlock error");
    }

    return result;
}

}

//typedef struct {
//    /** The bitmap width in pixels. */
//    uint32_t    width;
//    /** The bitmap height in pixels. */
//    uint32_t    height;
//    /** The number of byte per row. */
//    uint32_t    stride;
//    /** The bitmap pixel format. See {@link AndroidBitmapFormat} */
//    int32_t     format;
//    /** Unused. */
//    uint32_t    flags;      // 0 for now
//} AndroidBitmapInfo;
//출처: https://jamssoft.tistory.com/113 [What should I do?:티스토리]

//enum AndroidBitmapFormat {
//    /** No format. */
//    ANDROID_BITMAP_FORMAT_NONE      = 0,
//    /** Red: 8 bits, Green: 8 bits, Blue: 8 bits, Alpha: 8 bits. **/
//    ANDROID_BITMAP_FORMAT_RGBA_8888 = 1,
//    /** Red: 5 bits, Green: 6 bits, Blue: 5 bits. **/
//    ANDROID_BITMAP_FORMAT_RGB_565   = 4,
//    /** Deprecated in API level 13.
//    Because of the poor quality of this configuration,
//    it is advised to use ARGB_8888 instead. **/
//    ANDROID_BITMAP_FORMAT_RGBA_4444 = 7,
//    /** Alpha: 8 bits. */
//    ANDROID_BITMAP_FORMAT_A_8       = 8,
//};
//출처: https://jamssoft.tistory.com/113 [What should I do?:티스토리]