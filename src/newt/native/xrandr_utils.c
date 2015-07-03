/**
 * This file contains code from xrandr.c,
 * see <http://cgit.freedesktop.org/xorg/app/xrandr/tree/xrandr.c>:
 *
 * ++++
 *
 * Copyright © 2001 Keith Packard, member of The XFree86 Project, Inc.
 * Copyright © 2002 Hewlett Packard Company, Inc.
 * Copyright © 2006 Intel Corporation
 * Copyright © 2013 NVIDIA Corporation
 *
 * Permission to use, copy, modify, distribute, and sell this software and its
 * documentation for any purpose is hereby granted without fee, provided that
 * the above copyright notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting documentation, and
 * that the name of the copyright holders not be used in advertising or
 * publicity pertaining to distribution of the software without specific,
 * written prior permission.  The copyright holders make no representations
 * about the suitability of this software for any purpose.  It is provided "as
 * is" without express or implied warranty.
 *
 * THE COPYRIGHT HOLDERS DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO
 * EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY SPECIAL, INDIRECT OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THIS SOFTWARE.
 *
 * Thanks to Jim Gettys who wrote most of the client side code,
 * and part of the server code for randr.
 *
 * ++++
 *
 * Modifications / Additions are from:
 *
 * Copyright 2015 JogAmp Community. All rights reserved.
 *
 * License text: Same as above! 
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

#include "X11Common.h"

#include <math.h>

typedef struct {
    int         x1, y1, x2, y2;
} box_t;
typedef struct {
    int         x, y;
} point_t;
typedef struct {
    XTransform  transform;
    char        *filter;
    int         nparams;
    XFixed      *params;
} transform_t;
typedef struct _crtc {
    struct _crtc  *next;
    RRCrtc        crtc_id;
    Rotation      rotation;
    transform_t   transform;
    int           x, y;
    RRMode        mode_id;
    // float      refresh;
    // Bool       primary;

    XRRModeInfo   *mode_info;
    XRRCrtcInfo   *crtc_info;
    XRRPanning    *panning_info;
} crtc_t;
static int mode_height (XRRModeInfo *mode_info, Rotation rotation) {
    switch (rotation & 0xf) {
        case RR_Rotate_0:
        case RR_Rotate_180:
          return mode_info->height;
        case RR_Rotate_90:
        case RR_Rotate_270:
          return mode_info->width;
        default:
          return 0;
    }
}
static int mode_width (XRRModeInfo *mode_info, Rotation rotation) {
    switch (rotation & 0xf) {
        case RR_Rotate_0:
        case RR_Rotate_180:
          return mode_info->width;
        case RR_Rotate_90:
        case RR_Rotate_270:
          return mode_info->height;
        default:
          return 0;
    }
}
static Bool transform_point (XTransform *transform, double *xp, double *yp) {
    double  vector[3];
    double  result[3];
    int     i, j;
    double  v;

    vector[0] = *xp;
    vector[1] = *yp;
    vector[2] = 1;
    for (j = 0; j < 3; j++)
    {
      v = 0;
      for (i = 0; i < 3; i++) {
          v += (XFixedToDouble (transform->matrix[j][i]) * vector[i]);
      }
      if (v > 32767 || v < -32767) {
          return False;
      }
      result[j] = v;
    }
    if (!result[2]) {
      return False;
    }
    for (j = 0; j < 2; j++) {
      vector[j] = result[j] / result[2];
    }
    *xp = vector[0];
    *yp = vector[1];
    return True;
}
static void path_bounds (XTransform *transform, point_t *points, int npoints, box_t *box) {
    int   i;
    box_t point;

    for (i = 0; i < npoints; i++) {
      double x, y;
      x = points[i].x;
      y = points[i].y;
      transform_point (transform, &x, &y);
      point.x1 = floor (x);
      point.y1 = floor (y);
      point.x2 = ceil (x);
      point.y2 = ceil (y);
      if (i == 0) {
          *box = point;
      } else {
          if (point.x1 < box->x1) { box->x1 = point.x1; }
          if (point.y1 < box->y1) { box->y1 = point.y1; }
          if (point.x2 > box->x2) { box->x2 = point.x2; }
          if (point.y2 > box->y2) { box->y2 = point.y2; }
      }
    }
}
static void mode_geometry (XRRModeInfo *mode_info, Rotation rotation,
                           XTransform *transform, box_t *bounds) {
    point_t rect[4];
    int width = mode_width (mode_info, rotation);
    int height = mode_height (mode_info, rotation);

    rect[0].x = 0;
    rect[0].y = 0;
    rect[1].x = width;
    rect[1].y = 0;
    rect[2].x = width;
    rect[2].y = height;
    rect[3].x = 0;
    rect[3].y = height;
    path_bounds (transform, rect, 4, bounds);
}
static void get_screen_size0(Display * dpy, Window root, 
                             crtc_t * root_crtc, int *io_scrn_width, int *io_scrn_height) {
    int fb_width = *io_scrn_width;
    int fb_height = *io_scrn_height;
    crtc_t *crtc;
    for (crtc = root_crtc; NULL != crtc; crtc = crtc->next) {
        if( None == crtc->mode_id || NULL == crtc->mode_info || 0 == crtc->crtc_info->noutput ) {
            // disabled
            continue;
        }
        XRRModeInfo *mode_info = crtc->mode_info;
        int       x, y, w, h;
        box_t     bounds;

        mode_geometry (mode_info, crtc->rotation,
                     &crtc->transform.transform, &bounds);
        x = crtc->x + bounds.x1;
        y = crtc->y + bounds.y1;
        w = bounds.x2 - bounds.x1;
        h = bounds.y2 - bounds.y1;

        /* fit fb to crtc */
        XRRPanning *pan;
        if (x + w > fb_width) {
            fb_width = x + w;
        }
        if (y + h > fb_height) {
            fb_height = y + h;
        }
        pan = crtc->panning_info;
        if (pan && pan->left + pan->width > fb_width) {
            fb_width = pan->left + pan->width;
        }
        if (pan && pan->top + pan->height > fb_height) {
            fb_height = pan->top + pan->height;
        }
    }
    int minWidth=0, minHeight=0, maxWidth=0, maxHeight=0;
    if( 1 != XRRGetScreenSizeRange (dpy, root, &minWidth, &minHeight, &maxWidth, &maxHeight) ) {
        // Use defaults in case of error ..
        minWidth=8; minHeight=8; maxWidth=16384; maxHeight=16384;
    }
    if( fb_width < minWidth ) {
        fb_width = minWidth;
    } else if( fb_width > maxWidth ) {
        fb_width = maxWidth;
    }
    if( fb_height < minHeight ) {
        fb_height = minHeight;
    } else if( fb_height > maxHeight ) {
        fb_height = maxHeight;
    }
    *io_scrn_width = fb_width;
    *io_scrn_height = fb_height;
}
static crtc_t* createCrtcChain(Display *dpy,
                    XRRScreenResources *resources,
                    RRCrtc customCrtc, XRRCrtcInfo *customCrtcInfo,
                    Rotation customRotation, int customX, int customY, 
                    XRRModeInfo *customModeInfo) 
{
    crtc_t *root_crtc = NULL;
    crtc_t *iter_crtc = NULL;
    int i;
    for(i=0; i<resources->ncrtc; i++) {
        crtc_t *next_crtc = calloc(1, sizeof(crtc_t));
        if( NULL == iter_crtc ) {
            root_crtc = next_crtc;
        } else {
            iter_crtc->next = next_crtc;
        }
        iter_crtc = next_crtc;

        RRCrtc crtcId = resources->crtcs[i];
        iter_crtc->crtc_id = crtcId;
        if( crtcId == customCrtc && 0 != customCrtc ) {
            iter_crtc->rotation = customRotation;
            iter_crtc->x = customX;
            iter_crtc->y = customY;
            iter_crtc->mode_info = customModeInfo;
            iter_crtc->mode_id = customModeInfo->id;
            iter_crtc->crtc_info = customCrtcInfo;
        } else {
            XRRCrtcInfo *xrrCrtcInfo = XRRGetCrtcInfo (dpy, resources, crtcId);
            iter_crtc->rotation = xrrCrtcInfo->rotation;
            iter_crtc->x = xrrCrtcInfo->x;
            iter_crtc->y = xrrCrtcInfo->y;
            iter_crtc->mode_id = xrrCrtcInfo->mode;
            iter_crtc->mode_info = findMode(resources, iter_crtc->mode_id);
            iter_crtc->crtc_info = xrrCrtcInfo;
        }
        iter_crtc->panning_info = XRRGetPanning(dpy, resources, crtcId);
    }
    return root_crtc;
}
static void destroyCrtcChain(crtc_t *root_crtc, RRCrtc customCrtc) {
    crtc_t * iter_crtc = root_crtc;
    while(NULL!=iter_crtc) {
        if( NULL != iter_crtc->crtc_info ) {
            if( iter_crtc->crtc_id != customCrtc || 0 == customCrtc ) {
                XRRFreeCrtcInfo(iter_crtc->crtc_info);
            }
            iter_crtc->crtc_info = NULL;
        }
        if( NULL != iter_crtc->panning_info ) {
            XRRFreePanning(iter_crtc->panning_info);
            iter_crtc->panning_info = NULL;
        }
        {
            crtc_t * last = iter_crtc;
            iter_crtc = iter_crtc->next;
            last->next = NULL;
            free(last);
        }
    }
}
static crtc_t *get_screen_size1(Display * dpy, Window root, 
                                int *io_scrn_width, int *io_scrn_height,
                                XRRScreenResources *resources,
                                RRCrtc customCrtc, XRRCrtcInfo *customCrtcInfo,
                                Rotation customRotation, int customX, int customY, 
                                XRRModeInfo *customModeInfo) {
    crtc_t *root_crtc = createCrtcChain(dpy, resources, customCrtc, customCrtcInfo, 
                                        customRotation, customX, customY, customModeInfo);
    get_screen_size0(dpy, root, root_crtc, io_scrn_width, io_scrn_height);
    return root_crtc;
}
static crtc_t *get_screen_size2(Display * dpy, Window root, 
                                int *io_scrn_width, int *io_scrn_height,
                                XRRScreenResources *resources) {
    crtc_t *root_crtc = createCrtcChain(dpy, resources, 0, NULL, 0, 0, 0, NULL);
    get_screen_size0(dpy, root, root_crtc, io_scrn_width, io_scrn_height);
    return root_crtc;
}
static Bool get_screen_sizemm(Display *dpy, int screen_idx,
                              int fb_width, int fb_height,
                              int *fb_width_mm, int *fb_height_mm,
                              int *pre_fb_width, int *pre_fb_height) {
    *pre_fb_width = DisplayWidth (dpy, screen_idx);
    *pre_fb_height = DisplayHeight (dpy, screen_idx);
    Bool fb_change;
    if (fb_width != *pre_fb_width || fb_height != *pre_fb_height ) {
        float dpi = (25.4 * *pre_fb_height) / DisplayHeightMM(dpy, screen_idx);
        *fb_width_mm = (25.4 * fb_width) / dpi;
        *fb_height_mm = (25.4 * fb_height) / dpi;
        fb_change = True;
    } else {
        *fb_width_mm = DisplayWidthMM (dpy, screen_idx);
        *fb_height_mm = DisplayHeightMM (dpy, screen_idx);
        fb_change = False;
    }
    return fb_change;
}

