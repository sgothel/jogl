/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

#ifndef BCM_VC_IV_H
#define BCM_VC_IV_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

typedef uint32_t DISPMANX_PROTECTION_T;
typedef uint32_t DISPMANX_RESOURCE_HANDLE_T;
typedef uint32_t DISPMANX_DISPLAY_HANDLE_T;
typedef uint32_t DISPMANX_UPDATE_HANDLE_T;
typedef uint32_t DISPMANX_ELEMENT_HANDLE_T;

#define DISPMANX_NO_HANDLE 0

#define DISPMANX_PROTECTION_MAX   0x0f
#define DISPMANX_PROTECTION_NONE  0
#define DISPMANX_PROTECTION_HDCP  11   // Derived from the WM DRM levels, 101-300



/* Default display IDs.
   Note: if you overwrite with you own dispmanx_platfrom_init function, you
   should use IDs you provided during dispmanx_display_attach.
*/
#define DISPMANX_ID_MAIN_LCD  0
#define DISPMANX_ID_AUX_LCD   1
#define DISPMANX_ID_HDMI      2
#define DISPMANX_ID_SDTV      3

/* Return codes. Nonzero ones indicate failure. */
typedef enum {
  DISPMANX_SUCCESS      = 0,
  DISPMANX_INVALID      = -1
  /* XXX others TBA */
} DISPMANX_STATUS_T;

typedef enum {
  /* Bottom 2 bits sets the orientation */
  DISPMANX_NO_ROTATE = 0,
  DISPMANX_ROTATE_90 = 1,
  DISPMANX_ROTATE_180 = 2,
  DISPMANX_ROTATE_270 = 3,

  DISPMANX_FLIP_HRIZ = 1 << 16,
  DISPMANX_FLIP_VERT = 1 << 17
} DISPMANX_TRANSFORM_T;

typedef enum {
  /* Bottom 2 bits sets the alpha mode */
  DISPMANX_FLAGS_ALPHA_FROM_SOURCE = 0,
  DISPMANX_FLAGS_ALPHA_FIXED_ALL_PIXELS = 1,
  DISPMANX_FLAGS_ALPHA_FIXED_NON_ZERO = 2,
  DISPMANX_FLAGS_ALPHA_FIXED_EXCEED_0X07 = 3,

  DISPMANX_FLAGS_ALPHA_PREMULT = 1 << 16,
  DISPMANX_FLAGS_ALPHA_MIX = 1 << 17
} DISPMANX_FLAGS_ALPHA_T;

struct VC_IMAGE_T;
typedef struct VC_IMAGE_T VC_IMAGE_T;

typedef struct {
  DISPMANX_FLAGS_ALPHA_T flags;
  uint32_t opacity;
  VC_IMAGE_T *mask;
} DISPMANX_ALPHA_T;

typedef struct {
  DISPMANX_FLAGS_ALPHA_T flags;
  uint32_t opacity;
  DISPMANX_RESOURCE_HANDLE_T mask;
} VC_DISPMANX_ALPHA_T;  /* for use with vmcs_host */


typedef enum {
  DISPMANX_FLAGS_CLAMP_NONE = 0,
  DISPMANX_FLAGS_CLAMP_LUMA_TRANSPARENT = 1,
#if __VCCOREVER__ >= 0x04000000
  DISPMANX_FLAGS_CLAMP_TRANSPARENT = 2,
  DISPMANX_FLAGS_CLAMP_REPLACE = 3
#else
  DISPMANX_FLAGS_CLAMP_CHROMA_TRANSPARENT = 2,
  DISPMANX_FLAGS_CLAMP_TRANSPARENT = 3
#endif
} DISPMANX_FLAGS_CLAMP_T;

typedef enum {
  DISPMANX_FLAGS_KEYMASK_OVERRIDE = 1,
  DISPMANX_FLAGS_KEYMASK_SMOOTH = 1 << 1,
  DISPMANX_FLAGS_KEYMASK_CR_INV = 1 << 2,
  DISPMANX_FLAGS_KEYMASK_CB_INV = 1 << 3,
  DISPMANX_FLAGS_KEYMASK_YY_INV = 1 << 4
} DISPMANX_FLAGS_KEYMASK_T;

typedef union {
  struct {
    uint8_t yy_upper;
    uint8_t yy_lower;
    uint8_t cr_upper;
    uint8_t cr_lower;
    uint8_t cb_upper;
    uint8_t cb_lower;
  } yuv;
  struct {
    uint8_t red_upper;
    uint8_t red_lower;
    uint8_t blue_upper;
    uint8_t blue_lower;
    uint8_t green_upper;
    uint8_t green_lower;
  } rgb;
} DISPMANX_CLAMP_KEYS_T;

typedef struct {
  DISPMANX_FLAGS_CLAMP_T mode;
  DISPMANX_FLAGS_KEYMASK_T key_mask;
  DISPMANX_CLAMP_KEYS_T key_value;
  uint32_t replace_value;
} DISPMANX_CLAMP_T;


typedef struct {
 DISPMANX_ELEMENT_HANDLE_T element;
 int width;   /* This is necessary because dispmanx elements are not queriable. */
 int height;
} EGL_DISPMANX_WINDOW_T;

typedef struct tag_VC_RECT_T {
 int32_t x;
 int32_t y;
 int32_t width;
 int32_t height;
} VC_RECT_T;

extern void bcm_host_init(void);
extern void bcm_host_deinit(void);

extern int32_t graphics_get_display_size( const uint16_t display_number,
                                         uint32_t *width,
                                         uint32_t *height);

extern DISPMANX_DISPLAY_HANDLE_T vc_dispmanx_display_open( uint32_t device );
extern DISPMANX_UPDATE_HANDLE_T vc_dispmanx_update_start( int32_t priority );
extern DISPMANX_ELEMENT_HANDLE_T vc_dispmanx_element_add ( DISPMANX_UPDATE_HANDLE_T update, DISPMANX_DISPLAY_HANDLE_T display,
                                                          int32_t layer, const VC_RECT_T *dest_rect, DISPMANX_RESOURCE_HANDLE_T src,
                                                          const VC_RECT_T *src_rect, DISPMANX_PROTECTION_T protection, 
                                                          VC_DISPMANX_ALPHA_T *alpha,
                                                          DISPMANX_CLAMP_T *clamp, DISPMANX_TRANSFORM_T transform );

extern int vc_dispmanx_update_submit_sync( DISPMANX_UPDATE_HANDLE_T update );

#ifdef __cplusplus
}
#endif

#endif
