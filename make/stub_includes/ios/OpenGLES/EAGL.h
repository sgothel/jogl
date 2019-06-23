typedef struct _EAGLContext EAGLContext;
typedef struct _EAGLSharegroup EAGLSharegroup;
typedef struct _EAGLDrawable EAGLDrawable;

typedef enum _EAGLRenderingAPI
{
    kEAGLRenderingAPIOpenGLES1 = 1,
    kEAGLRenderingAPIOpenGLES2 = 2,
    kEAGLRenderingAPIOpenGLES3 = 3,
} EAGLRenderingAPI;

void EAGLGetVersion(unsigned int* major, unsigned int* minor);

