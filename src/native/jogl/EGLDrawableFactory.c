#include <EGL/egl.h>
#include <KD/kd.h>
#include <KD/NV_extwindowprops.h>
#include "com_sun_opengl_impl_egl_EGLDrawableFactory.h"

// FIXME: move the glViewport call up to Java
#include <GLES/gl.h>

static EGLDisplay display = NULL;
static EGLSurface surface = NULL;
static EGLContext context = NULL;
static EGLConfig config = NULL;
static KDWindow* window = NULL;
static EGLNativeWindowType nativewin = NULL;
static EGLint lastWidth = 0;
static EGLint lastHeight = 0;

// FIXME: need to move this up to Java to conditionalize between ES 1 and ES 2
//static KDint nv_egl_renderable_flags = EGL_OPENGL_ES2_BIT;
static KDint nv_egl_renderable_flags = EGL_OPENGL_ES_BIT;
static KDint nv_egl_surface_flags = EGL_WINDOW_BIT;
static KDust jogPressUST = 0;

typedef struct
{
    KDint index;
    KDboolean wasPressed;
    KDust pressTime;
} DeviceButtonState;
#define NVM_BTNS_MAX 5
typedef enum
{
    NVM_BTN_JOGDIAL,
    NVM_BTN_WIDGET,
    NVM_BTN_BACK,
    NVM_BTN_CAMHALF,
    NVM_BTN_CAMFULL,
};
DeviceButtonState conButtons[NVM_BTNS_MAX] =
{
    {KD_INPUT_JOGDIAL_SELECT, 0, 0},
    {KD_INPUT_BUTTONS_0+0, 0, 0},
    {KD_INPUT_BUTTONS_0+1, 0, 0},
    {KD_INPUT_BUTTONS_0+2, 0, 0},
    {KD_INPUT_BUTTONS_0+3, 0, 0},
};

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_initEGL
  (JNIEnv *env, jobject unused)
{
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        kdLogMessage("Error - EGL get display failed\n");
        return JNI_FALSE;
    }
    if (!eglInitialize(display, 0, 0)) {
        kdLogMessage("Error - EGL init failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_chooseConfig
  (JNIEnv *env, jobject unused)
{
    #define MAX_CONFIGS 64
    EGLConfig confs[MAX_CONFIGS];
    EGLint numConfigs;
    EGLint fbAttrs[] = 
    {
/*
        // FIXME
        // OpenGL ES 2 settings
        EGL_RENDERABLE_TYPE, nv_egl_renderable_flags,
        EGL_DEPTH_SIZE, 16,
        EGL_SURFACE_TYPE, nv_egl_surface_flags,
        EGL_RED_SIZE, 5,
        EGL_GREEN_SIZE, 6,
        EGL_BLUE_SIZE, 5,
        EGL_ALPHA_SIZE, 0,
        EGL_NONE
*/

/*
        // FIXME
        // OpenGL ES 1 settings
        EGL_RED_SIZE,           1,
        EGL_GREEN_SIZE,         1,
        EGL_BLUE_SIZE,          1,
        EGL_ALPHA_SIZE,         EGL_DONT_CARE,
        EGL_DEPTH_SIZE,         1,
        EGL_STENCIL_SIZE,       EGL_DONT_CARE,
        EGL_SURFACE_TYPE,       EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE,    EGL_OPENGL_ES_BIT,
        EGL_NONE
*/

        // FIXME
        // OpenGL ES 1 settings
        EGL_RENDERABLE_TYPE,    EGL_OPENGL_ES_BIT,
        EGL_DEPTH_SIZE,         16,
        EGL_SURFACE_TYPE,       EGL_WINDOW_BIT,
        EGL_RED_SIZE,           5,
        EGL_GREEN_SIZE,         6,
        EGL_BLUE_SIZE,          5,
        EGL_ALPHA_SIZE,         0,
        EGL_NONE
    };

    if (!(eglChooseConfig(display, fbAttrs, confs, MAX_CONFIGS, &numConfigs) && numConfigs)) {
        kdLogMessage("Error - EGL choose config failed\n");
        return JNI_FALSE;
    }
    /* Use the first */
    config = confs[0];
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_checkDisplay
  (JNIEnv *env, jobject unused)
{
    if (display == NULL) {
        kdLogMessage("Error - EGL get display returned null\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_checkConfig
  (JNIEnv *env, jobject unused)
{
    if (config == NULL) {
        kdLogMessage("Error - EGL choose config returned null\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_createWindow
  (JNIEnv *env, jobject unused)
{
    window = kdCreateWindow(display, config, KD_NULL);
    if (!window) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_setWindowVisible
  (JNIEnv *env, jobject unused)
{
    KDboolean visible = KD_TRUE;
    kdSetWindowPropertybv(window,
                          KD_WINDOWPROPERTY_VISIBILITY, &visible);
}

JNIEXPORT void JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_setWindowFullscreen
  (JNIEnv *env, jobject unused)
{
    KDboolean fullscreen = KD_TRUE;
    kdSetWindowPropertybv(window,
                          KD_WINDOWPROPERTY_FULLSCREEN_NV, &fullscreen);
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_realizeWindow
  (JNIEnv *env, jobject unused)
{
    if (kdRealizeWindow(window, &nativewin) != 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_createSurface
  (JNIEnv *env, jobject unused)
{
    surface = eglCreateWindowSurface(display,
        config, nativewin, 0);
    if (!surface)
    {
        kdLogMessage("Error - EGL create window surface failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_createContext
  (JNIEnv *env, jobject unused)
{
    /*a
    static EGLint contextAttrs[] = 
    {
        // FIXME
        // OpenGL ES 2 settings
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };
    */

    const EGLint contextAttrs[] = 
    {
        // FIXME
        // OpenGL ES 1 settings
        EGL_CONTEXT_CLIENT_VERSION, 1,
        EGL_NONE
    };

    context = eglCreateContext(display,
        config, 0, contextAttrs);
    if (!context)
    {
        kdLogMessage("Error - EGL create context failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_makeCurrent
  (JNIEnv *env, jobject unused)
{
    if (!eglMakeCurrent(display, surface, surface, context))
    {
        kdLogMessage("Error - EGL make current failed\n");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_updateWindowSize
  (JNIEnv *env, jobject unused)
{
    EGLint drawWidth;
    EGLint drawHeight;

    eglQuerySurface(display, surface, EGL_WIDTH, &drawWidth);
    eglQuerySurface(display, surface, EGL_HEIGHT, &drawHeight);
    if ((lastWidth != drawWidth) || (lastHeight != drawHeight))
    {
        glViewport(0, 0, drawWidth, drawHeight);
        lastWidth = drawWidth;
        lastHeight = drawHeight;
    }
}

JNIEXPORT void JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_swapBuffers0
  (JNIEnv *env, jobject unused)
{
    eglSwapBuffers(display, surface);
}

JNIEXPORT jboolean JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_shouldExit
  (JNIEnv *env, jobject unused)
{
    const KDEvent* ev = NULL;
    do {
        ev = kdWaitEvent(0);
        if (ev != 0) {
            switch (ev->type) {
                case KD_EVENT_WINDOW_CLOSE:
                case KD_EVENT_QUIT:
                    return JNI_TRUE;
                default:
                    break;
                    /*
                case KD_EVENT_INPUT:
                {
                    if (!s_runningInLauncher)
                    {
                        int btn;
                        for (btn=0; btn<NVM_BTNS_MAX; btn++)
                        {
                            if (InputDown(ev, conButtons[btn].index))
                            {
                                if (!conButtons[btn].wasPressed)
                                {
                                    conButtons[btn].pressTime = ev->timestamp;
                                    conButtons[btn].wasPressed = KD_TRUE;
                                }
                            }
                            else
                            {
                                conButtons[btn].wasPressed = KD_FALSE;
                            }
                        }
                    }
                    break;
                }
                    */
            }
        }
    } while (ev != 0);
    return JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_shutdown
  (JNIEnv *env, jobject unused)
{
    if (context) {
        eglMakeCurrent(display,
            EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroyContext(display, context);
        context = EGL_NO_CONTEXT;
    }
    if (surface) {
        eglDestroySurface(display, surface);
        surface = EGL_NO_SURFACE;
    }
    kdDestroyWindow(window);
    if (display) {
        eglTerminate(display);
        display = EGL_NO_DISPLAY;
    }
}

JNIEXPORT jint JNICALL Java_com_sun_opengl_impl_egl_EGLDrawableFactory_getDirectBufferAddress
  (JNIEnv *env, jobject unused, jobject buffer)
{
    return (jint) (*env)->GetDirectBufferAddress(env, buffer);
}
