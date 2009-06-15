#ifndef _X_H_
#define _X_H_

typedef struct {} *   XID;
typedef int           Bool;
typedef struct {}     Display;
typedef int           Status;
typedef struct {}     Visual;
typedef unsigned long VisualID;
typedef XID           Colormap;
typedef XID           Cursor;
typedef XID           Drawable;
typedef XID           Font;
typedef XID           GContext;
typedef XID           KeySym;
typedef XID           Pixmap;
typedef XID           Window;

typedef struct __GLXcontextRec *GLXContext;
//typedef void *GLXContext;
typedef XID GLXPixmap;
typedef XID GLXDrawable;
/* GLX 1.3 and later */
typedef struct __GLXFBConfigRec *GLXFBConfig;
//typedef void *GLXFBConfig;
typedef XID GLXFBConfigID;
typedef XID GLXContextID;
typedef XID GLXWindow;
typedef XID GLXPbuffer;

// Hacks for glXGetProcAddress
typedef void (*__GLXextFuncPtr)(void);
typedef unsigned char   GLubyte;        /* 1-byte unsigned */

#endif /* defined _X_H_ */
