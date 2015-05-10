#include "jogamp_newt_driver_gbm_DisplayDriver.h"
#include "jogamp_newt_driver_gbm_ScreenDriver.h"
#include "jogamp_newt_driver_gbm_WindowDriver.h"

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_initIDs
  (JNIEnv *env, jclass cls){
  }

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    initGbm
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_initGbm
  (JNIEnv *env, jobject this){

  }

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_init
  (JNIEnv *env, jobject this){
  }

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    destroyDisplay
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_destroyDisplay
  (JNIEnv *env, jobject this){
  }

/*
 * Class:     jogamp_newt_driver_gbm_WindowDriver
 * Method:    createSurface
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_gbm_WindowDriver_createSurface
  (JNIEnv *env, jobject this, jlong gbmDevice){
  }