#ifndef _XRENDER_H_
#define _XRENDER_H_

#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>

typedef XID PictFormat;

typedef struct {
    short   red;
    short   redMask;
    short   green;
    short   greenMask;
    short   blue;
    short   blueMask;
    short   alpha;
    short   alphaMask;
} XRenderDirectFormat;

typedef struct {
    PictFormat	id;
    int			type;
    int			depth;
    XRenderDirectFormat	direct;
    Colormap		colormap;
} XRenderPictFormat;

#define PictFormatID	    (1 << 0)
#define PictFormatType	    (1 << 1)
#define PictFormatDepth	    (1 << 2)
#define PictFormatRed	    (1 << 3)
#define PictFormatRedMask   (1 << 4)
#define PictFormatGreen	    (1 << 5)
#define PictFormatGreenMask (1 << 6)
#define PictFormatBlue	    (1 << 7)
#define PictFormatBlueMask  (1 << 8)
#define PictFormatAlpha	    (1 << 9)
#define PictFormatAlphaMask (1 << 10)
#define PictFormatColormap  (1 << 11)

#endif /* _XRENDER_H_ */
