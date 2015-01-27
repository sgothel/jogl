#include <stdio.h> /* android */
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>
#include <EGL/egl.h>

/*   Java->C glue code:
 *   Java package: com.jogamp.opengl.egl.EGL
 *    Java method: void eglGetConfigAttributes(long dpy, long config, IntBuffer attributes, IntBuffer values)
 */
JNIEXPORT void JNICALL
Java_com_jogamp_opengl_egl_EGL_dispatch_1eglGetConfigAttributes(JNIEnv *env, jclass _unused, jlong dpy, jlong config, jint attributeCount, jobject attributes, jint attributes_byte_offset, jobject values, jint values_byte_offset, jlong procAddress) {
  typedef EGLBoolean (EGLAPIENTRY*_local_PFNEGLGETCONFIGATTRIBPROC)(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *  value);
  _local_PFNEGLGETCONFIGATTRIBPROC ptr_eglGetConfigAttrib = (_local_PFNEGLGETCONFIGATTRIBPROC) (intptr_t) procAddress;
  assert(ptr_eglGetConfigAttrib != NULL);

  if ( attributeCount > 0 && NULL != attributes ) {
    int i;
    int * attributes_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, attributes)) + attributes_byte_offset);
    EGLint * values_ptr = (EGLint *) (((char*) (*env)->GetDirectBufferAddress(env, values)) + values_byte_offset);
    for(i=0; i<attributeCount; i++) {
        if( 0 == (* ptr_eglGetConfigAttrib) ((EGLDisplay) (intptr_t) dpy, (EGLConfig) (intptr_t) config, (EGLint) attributes_ptr[i], (EGLint *) &values_ptr[i]) ) {
            attributes_ptr[i] = 0;
        }
    }
  }
}

