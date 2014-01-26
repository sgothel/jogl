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

/** 
 * http://en.wikipedia.org/wiki/VideoCore
 * https://github.com/raspberrypi/userland
 */

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


typedef struct tag_VC_RECT_T {
 int32_t x;
 int32_t y;
 int32_t width;
 int32_t height;
} VC_RECT_T;

/* Types of image supported. */
/* Please add any new types to the *end* of this list.  Also update
 * case_VC_IMAGE_ANY_xxx macros (below), and the vc_image_type_info table in
 * vc_image/vc_image_helper.c.
 */
typedef enum
{
   VC_IMAGE_MIN = 0, //bounds for error checking

   VC_IMAGE_RGB565 = 1,
   VC_IMAGE_1BPP,
   VC_IMAGE_YUV420,
   VC_IMAGE_48BPP,
   VC_IMAGE_RGB888,
   VC_IMAGE_8BPP,
   VC_IMAGE_4BPP,    // 4bpp palettised image
   VC_IMAGE_3D32,    /* A separated format of 16 colour/light shorts followed by 16 z values */
   VC_IMAGE_3D32B,   /* 16 colours followed by 16 z values */
   VC_IMAGE_3D32MAT, /* A separated format of 16 material/colour/light shorts followed by 16 z values */
   VC_IMAGE_RGB2X9,   /* 32 bit format containing 18 bits of 6.6.6 RGB, 9 bits per short */
   VC_IMAGE_RGB666,   /* 32-bit format holding 18 bits of 6.6.6 RGB */
   VC_IMAGE_PAL4_OBSOLETE,     // 4bpp palettised image with embedded palette
   VC_IMAGE_PAL8_OBSOLETE,     // 8bpp palettised image with embedded palette
   VC_IMAGE_RGBA32,   /* RGB888 with an alpha byte after each pixel */ /* xxx: isn't it BEFORE each pixel? */
   VC_IMAGE_YUV422,   /* a line of Y (32-byte padded), a line of U (16-byte padded), and a line of V (16-byte padded) */
   VC_IMAGE_RGBA565,  /* RGB565 with a transparent patch */
   VC_IMAGE_RGBA16,   /* Compressed (4444) version of RGBA32 */
   VC_IMAGE_YUV_UV,   /* VCIII codec format */
   VC_IMAGE_TF_RGBA32, /* VCIII T-format RGBA8888 */
   VC_IMAGE_TF_RGBX32,  /* VCIII T-format RGBx8888 */
   VC_IMAGE_TF_FLOAT, /* VCIII T-format float */
   VC_IMAGE_TF_RGBA16, /* VCIII T-format RGBA4444 */
   VC_IMAGE_TF_RGBA5551, /* VCIII T-format RGB5551 */
   VC_IMAGE_TF_RGB565, /* VCIII T-format RGB565 */
   VC_IMAGE_TF_YA88, /* VCIII T-format 8-bit luma and 8-bit alpha */
   VC_IMAGE_TF_BYTE, /* VCIII T-format 8 bit generic sample */
   VC_IMAGE_TF_PAL8, /* VCIII T-format 8-bit palette */
   VC_IMAGE_TF_PAL4, /* VCIII T-format 4-bit palette */
   VC_IMAGE_TF_ETC1, /* VCIII T-format Ericsson Texture Compressed */
   VC_IMAGE_BGR888,  /* RGB888 with R & B swapped */
   VC_IMAGE_BGR888_NP,  /* RGB888 with R & B swapped, but with no pitch, i.e. no padding after each row of pixels */
   VC_IMAGE_BAYER,  /* Bayer image, extra defines which variant is being used */
   VC_IMAGE_CODEC,  /* General wrapper for codec images e.g. JPEG from camera */
   VC_IMAGE_YUV_UV32,   /* VCIII codec format */
   VC_IMAGE_TF_Y8,   /* VCIII T-format 8-bit luma */
   VC_IMAGE_TF_A8,   /* VCIII T-format 8-bit alpha */
   VC_IMAGE_TF_SHORT,/* VCIII T-format 16-bit generic sample */
   VC_IMAGE_TF_1BPP, /* VCIII T-format 1bpp black/white */
   VC_IMAGE_OPENGL,
   VC_IMAGE_YUV444I, /* VCIII-B0 HVS YUV 4:4:4 interleaved samples */
   VC_IMAGE_YUV422PLANAR,  /* Y, U, & V planes separately (VC_IMAGE_YUV422 has them interleaved on a per line basis) */
   VC_IMAGE_ARGB8888,   /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
   VC_IMAGE_XRGB8888,   /* 32bpp with 8bit unused at MS byte, with R, G, B (LS byte) */

   VC_IMAGE_YUV422YUYV,  /* interleaved 8 bit samples of Y, U, Y, V */
   VC_IMAGE_YUV422YVYU,  /* interleaved 8 bit samples of Y, V, Y, U */
   VC_IMAGE_YUV422UYVY,  /* interleaved 8 bit samples of U, Y, V, Y */
   VC_IMAGE_YUV422VYUY,  /* interleaved 8 bit samples of V, Y, U, Y */

   VC_IMAGE_RGBX32,      /* 32bpp like RGBA32 but with unused alpha */
   VC_IMAGE_RGBX8888,    /* 32bpp, corresponding to RGBA with unused alpha */
   VC_IMAGE_BGRX8888,    /* 32bpp, corresponding to BGRA with unused alpha */

   VC_IMAGE_YUV420SP,    /* Y as a plane, then UV byte interleaved in plane with with same pitch, half height */
   
   VC_IMAGE_YUV444PLANAR,  /* Y, U, & V planes separately 4:4:4 */
   
   VC_IMAGE_MAX,     //bounds for error checking
   VC_IMAGE_FORCE_ENUM_16BIT = 0xffff,
} VC_IMAGE_TYPE_T;

/**
 * From https://github.com/raspberrypi/userland/blob/master/interface/vmcs_host/vc_vchi_dispmanx.h
 */
typedef enum {
   DISPMANX_ELEMENT_CHANGE_LAYER         =  (1<<0),
   DISPMANX_ELEMENT_CHANGE_OPACITY       =  (1<<1),
   DISPMANX_ELEMENT_CHANGE_DEST_RECT     =  (1<<2),
   DISPMANX_ELEMENT_CHANGE_SRC_RECT      =  (1<<3),
   DISPMANX_ELEMENT_CHANGE_MASK_RESOURCE =  (1<<4),
   DISPMANX_ELEMENT_CHANGE_TRANSFORM     =  (1<<5)
/**
 * Not working /validated !
   DISPMANX_ELEMENT_CHANGE_MIN         =  0x00,
   DISPMANX_ELEMENT_CHANGE_SOURCE      =  0x01,
   DISPMANX_ELEMENT_INSERT_ABOVE       =  0x80,
   DISPMANX_ELEMENT_CHANGE_FLAGS       =  0x100,
   DISPMANX_ELEMENT_CHANGE_NOTHING     =  0x200,
   DISPMANX_ELEMENT_CHANGE_ALPHA_FLAGS =  0x400,
   DISPMANX_ELEMENT_CHANGE_PROTECTION  =  0x800,
   DISPMANX_ELEMENT_CHANGE_MAX         =  0x1000
 */
} DISPMANX_ELEMENT_CHANGE_T;



extern void bcm_host_init(void);
extern void bcm_host_deinit(void);

extern int32_t graphics_get_display_size( const uint16_t display_number,
                                         uint32_t *width,
                                         uint32_t *height);

extern DISPMANX_DISPLAY_HANDLE_T vc_dispmanx_display_open( uint32_t device );
extern int vc_dispmanx_display_close( DISPMANX_DISPLAY_HANDLE_T display );

extern DISPMANX_RESOURCE_HANDLE_T vc_dispmanx_resource_create(VC_IMAGE_TYPE_T type, uint32_t width, uint32_t height, uint32_t *native_image_handle);
extern int vc_dispmanx_resource_write_data( DISPMANX_RESOURCE_HANDLE_T res, VC_IMAGE_TYPE_T src_type, int src_pitch, void * src_address, const VC_RECT_T * rect );
//extern int vc_dispmanx_resource_write_data_handle( DISPMANX_RESOURCE_HANDLE_T res, VC_IMAGE_TYPE_T src_type, int src_pitch, 
//                                                   VCHI_MEM_HANDLE_T handle, uint32_t offset, const VC_RECT_T * rect );
extern int vc_dispmanx_resource_delete( DISPMANX_RESOURCE_HANDLE_T res );



extern DISPMANX_UPDATE_HANDLE_T vc_dispmanx_update_start( int32_t priority );
extern DISPMANX_ELEMENT_HANDLE_T vc_dispmanx_element_add ( DISPMANX_UPDATE_HANDLE_T update, DISPMANX_DISPLAY_HANDLE_T display,
                                                          int32_t layer, const VC_RECT_T *dest_rect, DISPMANX_RESOURCE_HANDLE_T src,
                                                          const VC_RECT_T *src_rect, DISPMANX_PROTECTION_T protection, 
                                                          VC_DISPMANX_ALPHA_T *alpha,
                                                          DISPMANX_CLAMP_T *clamp, DISPMANX_TRANSFORM_T transform );
extern int vc_dispmanx_element_remove( DISPMANX_UPDATE_HANDLE_T update, DISPMANX_ELEMENT_HANDLE_T element );


extern int vc_dispmanx_update_submit_sync( DISPMANX_UPDATE_HANDLE_T update );

//New function added to VCHI to change attributes, set_opacity does not work there.
extern int vc_dispmanx_element_change_attributes( DISPMANX_UPDATE_HANDLE_T update,
                                                  DISPMANX_ELEMENT_HANDLE_T element,
                                                  uint32_t change_flags,
                                                  int32_t layer,
                                                  uint8_t opacity,
                                                  const VC_RECT_T *dest_rect,
                                                  const VC_RECT_T *src_rect,
                                                  DISPMANX_RESOURCE_HANDLE_T mask,
                                                  DISPMANX_TRANSFORM_T transform );

#ifdef __cplusplus
}
#endif

#endif
