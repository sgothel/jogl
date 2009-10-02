//-----------------------------------------------------------------------------
// This file is provided under a dual BSD/GPLv2 license.  When using or
// redistributing this file, you may do so under either license.
//
// GPL LICENSE SUMMARY
//
// Copyright(c) 2005-2009 Intel Corporation. All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of version 2 of the GNU General Public License as
// published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St - Fifth Floor, Boston, MA 02110-1301 USA.
// The full GNU General Public License is included in this distribution
// in the file called LICENSE.GPL.
//
// Contact Information:
//      Intel Corporation
//      2200 Mission College Blvd.
//      Santa Clara, CA  97052
//
// BSD LICENSE
//
// Copyright(c) 2005-2009 Intel Corporation. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//   - Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//   - Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in
//     the documentation and/or other materials provided with the
//     distribution.
//   - Neither the name of Intel Corporation nor the names of its
//     contributors may be used to endorse or promote products derived
//     from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//---------------------------------------------------------------------------*/

#ifndef _GDL_TYPES_H_
#define _GDL_TYPES_H_

/*----------------------------------------------------------------------
 *                     G E N E R A L
 *---------------------------------------------------------------------*/

/** @ingroup general */
/**@{*/
typedef unsigned char      gdl_uint8; ///< Unsigned 8 bit integer.
typedef unsigned short     gdl_uint16;///< Unsigned 16 bit integer.
typedef unsigned int       gdl_uint32;///< Unsigned 32 bit integer.
typedef unsigned long long gdl_uint64;///< Unsigned 64 bit integer.
typedef char               gdl_int8;  ///< Signed 8 bit integer.
typedef short              gdl_int16; ///< Signed 16 bit integer.
typedef int                gdl_int32; ///< Signed 32 bit integer.
typedef long long          gdl_int64; ///< Signed 64 bit integer.
typedef void               gdl_void;  ///< Void data type.
typedef float              gdl_f32;   ///< Single precision floating point
typedef double             gdl_f64;   ///< Double precision floating point

typedef unsigned long      physaddr_t; ///< A physical memory address
/**@}*/

/** @ingroup general
    Boolean data type
*/
typedef enum
{
    GDL_FALSE   = 0,        ///< Boolean false.
    GDL_TRUE    = 1         ///< Boolean true.
} gdl_boolean_t;


/** @ingroup general
*/
typedef struct
{
    gdl_int32 x;            ///< Point X-coordinate
    gdl_int32 y;            ///< Point Y-coordinate
} gdl_point_t;


/** @ingroup general
*/
typedef struct
{
    gdl_point_t  origin;    ///< Rectangle origin
    gdl_uint32   width;     ///< Rectangle width
    gdl_uint32   height;    ///< Rectangle height
} gdl_rectangle_t;


// Return 0 if two gdl_rectangle_t variables are not equal, non-zero otherwise
static __inline int
rect_eq( gdl_rectangle_t *rect1, gdl_rectangle_t *rect2)
{
    return  (rect1->origin.x == rect2->origin.x)
        &&  (rect1->origin.y == rect2->origin.y)
        &&  (rect1->width    == rect2->width   )
        &&  (rect1->height   == rect2->height  );
}

/** @ingroup general
    Bit flags returned in the 'flags' field of #gdl_driver_info_t.
*/
typedef enum
{
    GDL_DRIVER_INFO_HDCP_ENABLED        = 1,
        ///< Driver is running on a chip with HDCP enabled.
    GDL_DRIVER_INFO_MACROVISION_ENABLED = 4,
        ///< Driver is running on a chip with Macrovision support enabled.
    GDL_DRIVER_INFO_1080P_ENABLED       = 8
        ///< Driver is running on a chip with 1080p display mode enabled.
} gdl_driver_info_flag_t;


/** @ingroup general
    Display Driver information structure returned by gdl_get_driver_info().
*/
typedef struct
{
    gdl_int16       header_version_major;
                    /**< Major version number of header files. */
    gdl_int16       header_version_minor;
                    /**< Minor version number of header files. */
    gdl_uint32      gdl_version;
                    /**< Driver version number */
    gdl_int32       build_tag;
                    /**< Driver build tag. */
    gdl_driver_info_flag_t flags;
                    /**< Capabilities of chip on which the driver is running. */
    char            name[64];
                    /**< Display driver name as a 0-terminated ASCII string. */
    gdl_uint32      mem_size;
                    /**< Total number of bytes allocated for driver memory.
                     * Extra heaps will be visible only if the caller attached
                     * them previously. See #gdl_attach_heap for details.
                     */
    gdl_uint32      mem_avail;
                    /**< Number of bytes of driver memory currently unused.
                     * Extra heaps will be visible only if the caller attached
                     * them previously. See #gdl_attach_heap for details.
                     */
} gdl_driver_info_t;


/** @ingroup general
    Pixel component extension options.  The blender operates on pixels
    with 10-bit components (i.e., 10-bit R, G, and B values for RGB pixel
    formats; 10-bit chroma/luma values for YUV pixel formats).  However, most
    of the pixel formats supported by the planes have component values of less
    than 10 bits; as the planes output such pixels, they extend them to 10 bits
    using one of the methods described by this enumeration.
*/
typedef enum
{
    GDL_PIXEL_EXTENSION_ZERO_PAD,
            /**<
            Pixel component values are extended from N to 10 bits by
            shifting them left (10-N) bits and appending 0's. E.g., the
            8-bit value 0x7f [0111 1111] becomes 0x1fc [01 1111 1100].
            With this method, the difference between any two consecutive
            output values is a constant, but the highest possible value is
            0x3fc (11 1111 1100).
            */
    GDL_PIXEL_EXTENSION_MSB_SHADOW,
            /**<
            Pixel component values are first extended from N to 10 bits by
            shifting them left (10-N) bits and appending 0's. Then the
            (10-N) most significant bits are OR'd into the low-order (10-N)
            places. E.g., the 8-bit value 0x7f (0111 1111) becomes 0x1fd
            (01 1111 1101).   With this method, the difference between any
            two consecutive output values is NOT constant, but the values at
            either end of the output range are fully saturated (0x000 and 0x3ff).
            */
    GDL_PIXEL_EXTENSION_COUNT
            /**<
            Number of valid entries in this enumeration.
            */
} gdl_pixel_extension_t;


/** @ingroup general
    Pixel format identifiers.  Note that the appearance of a pixel format in
    this list does not guarantee that is is supported by the hardware.
 */
typedef enum
{
    /* RGB types */
    GDL_PF_ARGB_32,         ///< ARGB 32bpp 8:8:8:8  LE
    GDL_PF_RGB_32,          ///< xRGB 32bpp x:8:8:8  LE
    GDL_PF_RGB_30,          ///<  RGB 30bpp 10:10:10 LE [fully packed]
    GDL_PF_RGB_24,          ///<  RGB 24bpp 8:8:8    LE [fully packed]
    GDL_PF_ARGB_16_1555,    ///< ARGB 16bpp 1:5:5:5  LE
    GDL_PF_ARGB_16_4444,    ///< ARGB 16bpp 4:4:4:4  LE
    GDL_PF_RGB_16,          ///<  RGB 16bpp 5:6:5    LE
    GDL_PF_RGB_15,          ///< xRGB 16bpp x:5:5:5  LE

    /* CLUT types */
    GDL_PF_RGB_8,           ///<  RGB 8bpp - 24bpp palette RGB 8:8:8
    GDL_PF_ARGB_8,          ///< ARGB 8bpp - 32bpp palette ARGB 8:8:8:8
    GDL_PF_AYUV_8,          ///< AYUV 8bpp - 32bpp palette AYUV 8:8:8:8

    /* Packed YUV types */
    GDL_PF_YUY2,            ///< Packed YUV 4:2:2 32-bit: V0:Y1:U0:Y0 LE
    GDL_PF_UYVY,            ///< Packed YUV 4:2:2 32-bit: Y1:V0:Y0:U0 LE
    GDL_PF_YVYU,            ///< Packed YUV 4:2:2 32-bit: U0:Y1:V0:Y0 LE
    GDL_PF_VYUY,

    /* Planar YUV types */
    GDL_PF_YV12,            ///< YVU 4:2:0 Planar (V plane precedes U)
    GDL_PF_YVU9,            ///< YUV 4:2:0 Planar
    GDL_PF_I420,            ///< YUV 4:2:0 Planar (U plane precedes V)
    GDL_PF_IYUV=GDL_PF_I420,///< Synonym for I420

            //************************************************************
            // NOTE!: GDL_PF_IYUV must immediately follow GDL_PF_I420!
            //************************************************************

    GDL_PF_I422,            ///< YUV 4:2:2 Planar (U plane precedes V)
    GDL_PF_YV16,            ///< YVU 4:2:2 Planar (V plane precedes U)

    /* Pseudo-planar YUV types */
    GDL_PF_NV12,            ///< YUV 4:2:0 Pseudo-planar
    GDL_PF_NV16,            ///< YUV 4:2:2 Pseudo-planar
    GDL_PF_NV20,            ///< YUV 4:2:2 Pseudo-planar, 10-bit components

    GDL_PF_A1,              ///< 1-bit Alpha-only surface
    GDL_PF_A4,              ///< 4-bit Alpha-only surface
    GDL_PF_A8,              ///< 8-bit Alpha-only surface

    GDL_PF_AY16,            ///< Alpha-luminance 8:8. Used for video textures
    GDL_PF_COUNT            ///< Number of defined pixel formats
} gdl_pixel_format_t;


/** @ingroup general
    Error codes that can be returned by GDL functions.  Note that more detailed
    information about an error may also be printed on the system console.
*/
typedef enum
{
    GDL_SUCCESS             = 0,
        /**<
        Function executed without errors
        */
    GDL_ERR_INVAL           = 0x01,
        /**<
        An invalid argument was passed.
        */
    GDL_ERR_BUSY            = 0x02,
        /**<
        An operation could not be completed because a needed resource is in use.
        */
    GDL_ERR_DISPLAY         = 0x03,
        /**<
        An invalid display ID was passed.
        */
    GDL_ERR_SURFACE         = 0x04,
        /**<
        An invalid surface ID, or the ID of a surface that is not
        appropriate for the requested operation, was passed.
        */
    GDL_ERR_COMMAND         = 0x05,
        /**<
        An internal command processing error occurred
        */
    GDL_ERR_NULL_ARG        = 0x06,
        /**<
        A required argument was missing.  Either a NULL pointer or a count
        of 0 was passed for a required argument.
        */
    GDL_ERR_NO_MEMORY       = 0x07,
        /**<
        Could not allocate memory.
        */
    GDL_ERR_FAILED          = 0x08,
        /**<
        This is a generic error code that generally means that a system
        call or call to some other software external to the driver
        returned a failure code.
        */
    GDL_ERR_INTERNAL        = 0x09,
        /**<
        A condition that "should not be possible" was detected within the
        driver.  This generally means there is nothing the application can
        do to correct the problem.
        */
    GDL_ERR_NOT_IMPL        = 0x0a,
        /**<
        The function is not currently implemented for the target chip.
        */
    GDL_ERR_MAPPED          = 0x0b,
        /**<
        Operation not permitted on the mapped surface.
        */
    GDL_ERR_NO_INIT         = 0x0c,
        /**<
        A GDL function was called without a preceding call to gdl_init().
        */
    GDL_ERR_NO_HW_SUPPORT   = 0x0d,
        /**<
        The target chip does not support the requested function.  Examples:
        - A graphics rendering option is not supported by the graphics core
          in the target chip.
        - A plane or port driver does not support a requested attribute.
        - An attempt was made to request the attribute list from a port
          driver that does not support any attributes.
        */
    GDL_ERR_INVAL_PF        = 0x0e,
        /**<
        An unknown pixel format, or a pixel format not supported by the
        attempted operation, was passed.
        */
    GDL_ERR_INVAL_RECT      = 0x0f,
        /**<
        An invalid argument of type #gdl_rectangle_t was passed to the function.
        */
    GDL_ERR_ATTR_ID         = 0x10,
        /**<
        An undefined ID was specified for a plane attribute or a port
        driver attribute.
        */
    GDL_ERR_ATTR_NO_SUPPORT = 0x11,
        /**<
        An unsupported ID was specified for a plane attribute or a port
        driver attribute.
        */
    GDL_ERR_ATTR_READONLY   = 0x12,
        /**<
        An attempt was made to set the value of a read-only plane attribute
        or port driver attribute.
        */
    GDL_ERR_ATTR_VALUE      = 0x13,
        /**<
        An invalid value was specified for a plane attribute or a port
        driver attribute.
        */
    GDL_ERR_PLANE_CONFLICT  = 0x14,
        /**<
        An attempt was made to change the display mode to a resolution too
        small to accommodate all of the currently enabled planes at their
        current positions on the display. Move/shrink the affected planes first.
        */
    GDL_ERR_DISPLAY_CONFLICT= 0x15,
        /**<
        An attempt was made to change the size or origin of a plane such
        that part/all of the plane would no longer be on the display.
        Increase the display resolution first.
        */
    GDL_ERR_TIMEOUT        = 0x16,
        /**<
        The requested timeout period occurred before the requested
        operation trigger occurred.
        */
    GDL_ERR_MISSING_BEGIN  = 0x17,
         /**<
         An attempt was made to set a plane attribute without first calling
         gdl_config_begin().
         */
    GDL_ERR_PLANE_ID       = 0x18,
        /**<
        An invalid plane ID was passed.  The ID is undefined, the plane is not
        supported by the target chip, or the plane is not supported by the
        called function.
        */
    GDL_ERR_INVAL_PTR      = 0x19,
        /**<
        On Linux, a copy between user and kernel space failed.  This
        probably indicates an invalid user space (argument) pointer.
        */

    GDL_ERR_INVAL_HEAP = 0x1a,
        /**<
        An invalid heap was passed for addition or removal. Attempt
        to add overlaping heaps will cause this error too.
        */

    GDL_ERR_HEAP_IN_USE = 0x1b,
        /**<
        Heap removal was attempted while at least one surface was allocated
        from that heap.
        */

    GDL_ERR_INVAL_CALLBACK = 0x1c,
        /**<
        Invalid callback (null) was passed to gdl_event_register() function
        */

    GDL_ERR_SCALING_POLICY  = 0x1d,
        /**<
        A single scaling policy is required and was not specified for the
        #gdl_display_info_t structure used, or scaling policies are
        unsupported for the specified display ID.
        */

    GDL_ERR_INVAL_EVENT = 0x1e,
        /**<
        Invalid event was passed to functions expecting #gdl_app_event_t.
        */

    GDL_ERR_INVAL_IOCTL     = 0x1f,
        /**<
        Invalid IOCTL request was sent to kernel module
        */
    GDL_ERR_SCHED_IN_ATOMIC = 0x20,
        /**<
        Scheduling was attempted while being in atomic context.
        */
    GDL_ERR_MMAP            = 0x21,
        /**<
        Memory mapping failed
        */
    GDL_ERR_HDCP            = 0x22,
        /**<
        HDCP failure
        */
    GDL_ERR_CONFIG          = 0x23,
        /**<
        Platform config file error: either a required entry in the
        platform configuration file is missing, or its entry is invalid.
        */
    GDL_ERR_HDMI_AUDIO_PLAYBACK = 0x24,
        /**<
        HDMI Audio start / stop / set buffer / set format command was
        initiated at the wrong time.
        */
    GDL_ERR_HDMI_AUDIO_BUFFER_FULL = 0x25,
        /**<
        Given data does not fit in the internal buffer
        */
    GDL_ERR_PLANE_ORIGIN_ODD   = 0x26,
        /**<
        In interlaced display modes, active planes must be configured with
        their origins on even display lines. This error is returned when:
        - in a progressive display mode: an attempt is made to change to an
          interlaced display mode while there is an active plane does not
          meet this requirement.
        - in an interlaced display mode:
           - an attempt is made to reconfigure an active plane's origin
             to an odd line number, OR
           - an attempt is made to activate (by flipping a surface to) a
             plane that doesn't meet this requirement.
        */
    GDL_ERR_PLANE_HEIGHT_ODD   = 0x27,
        /**<
        In interlaced display modes, active planes must be configured with
        their even heights. This error is returned when:
        - in a progressive display mode: an attempt is made to change to an
          interlaced display mode while there is an active plane does not
          meet this requirement.
        - in an interlaced display mode:
           - an attempt is made to reconfigure an active plane's height
             to an odd value, OR
           - an attempt is made to activate (by flipping a surface to) a
             plane that doesn't meet this requirement.
        */
    GDL_ERR_HANDLE              = 0x28,
        /**<
        Given handle is not valid
        */
    GDL_ERR_TVMODE_UNDEFINED    = 0x29,
        /**<
        Display has undefined tv mode set on it.
        */
    GDL_ERR_PREMULT_CONFLICT    = 0x2a,
        /**<
        An attempt was made to enable the #GDL_PLANE_ALPHA_PREMULT attribute and
        one of the following incompatible features at the same time:
        - Chroma keying on the same plane (#GDL_PLANE_CHROMA_KEY_SRC_ENABLE set
          to #GDL_TRUE).
        - Gamma removal on the same plane (#GDL_PLANE_REVERSE_GAMMA_TYPE set to
          a value other than #GDL_GAMMA_LINEAR.
        - color space conversion (the value of the plane's
          #GDL_PLANE_SRC_COLOR_SPACE attribute is different from the color
          space of the display to which it is connected).
        - a non-RGB pixel format.
        */

    GDL_ERR_SUSPENDED = 0x2b,
        /**<
        An attempt was made to execute a command while the driver was in a
        suspended mode. During the suspended mode driver is in a low-power
        state and no access to hardware is allowed.
        */

    //**********************************************************************
    // ATTENTION!!: WHEN ADDING AN ERROR CODE MAKE SURE TO:
    // - Search for a value marked "Currently unused" in the list above
    //   before adding a new value at the end.
    // - Include inline (doxygen) documentation for the new error.
    // - Add the new error to _error_string() in debug.c
    //**********************************************************************
} gdl_ret_t;

/** @ingroup general */
#define GDLFAIL(err) ((err) != GDL_SUCCESS)
/** @ingroup general */
#define GDLPASS(err) (!GDLFAIL(err))

/** @ingroup general

    Types of gamma curves. There are three standard values of gamma:
    - 1.0: (linear) leaves the input luminance unchanged.
    - 2.2: NTSC and HDTV content is typically generated with this value.
    - 2.8: PAL and SECAM standards indicate this value, although 2.2
      is commonly used.
*/
typedef enum
{
    GDL_GAMMA_LINEAR,   ///< 1.0
    GDL_GAMMA_2_2,      ///< 2.2
    GDL_GAMMA_2_8,      ///< 2.8
    GDL_GAMMA_COUNT,    ///< Number of entries in this enumeration
} gdl_gamma_t;


/** @ingroup general
    The occurrence of the VSync signal indicates completion of the updating of
    video output with a new image.  In interlaced display modes, this image
    may be either a top field or a bottom field; in progressive display modes
    it is always a full frame.

    The type of image update cycle that has just been completed may be thought
    of as the 'polarity' of the VSync signal.

    This enumeration defines values for all three image types.
*/
typedef enum
{
    GDL_POLARITY_FRAME=1,       ///< Frame (Progressive)
    GDL_POLARITY_FIELD_TOP=2,   ///< Top field (Interlaced)
    GDL_POLARITY_FIELD_BOTTOM=3 ///< Bottom field (Interlaced)
} gdl_polarity_t;

//----------------------------------------------------------------------
//                 D I S P L A Y   M O D E
//----------------------------------------------------------------------

/** @ingroup disp_mode
    Refresh rates for TV mode definitions.

    Refresh rate is the number of times the display is updated per second.
    This is the number of frames per second for progressive display modes;
    the number of fields (half the number of frames) per second for interlaced
    display modes.

    IF WRITING AN EXTERNAL (non-Intel) PORT DRIVER: if new rates need to be
    defined, create a new enumeration whose first entry has a value equal to
    #GDL_REFRESH_USER_DEFINED.  Since no internal port driver will support those
    modes, applications must make sure that only the external port driver is
    active on a display before setting the display to one of those modes.
*/
typedef enum
{
    GDL_REFRESH_23_98,      /**< 23.98... (24/1.001)    */
    GDL_REFRESH_24,         /**< 24                     */
    GDL_REFRESH_25,         /**< 25                     */
    GDL_REFRESH_29_97,      /**< 29.97... (30/1.001)    */
    GDL_REFRESH_30,         /**< 30 - DEPRECATED: This value is normally only
                                 used on computer systems and should be used
                                 with care, if at all. The corresponding TV
                                 rate is 30/(1.001) (see #GDL_REFRESH_29_97).
                            */
    GDL_REFRESH_50,         /**< 50                     */
    GDL_REFRESH_59_94,      /**< 59.94... (60/1.001)    */
    GDL_REFRESH_60,         /**< 60 - DEPRECATED: This value is normally only
                                 used on computer systems and should be used
                                 with care, if at all. The corresponding TV
                                 rate is 60/(1.001) (see #GDL_REFRESH_59_94).
                            */
    GDL_REFRESH_USER_DEFINED/* External (non-Intel) port drivers may define
                               additional refresh rates that the support. Their
                               IDs must be numbered starting at this value.
                             */
} gdl_refresh_t;


/** @ingroup disp_mode
    Display (pipe) ids.  The Intel® CE Media Processors have two displays.
*/
typedef enum
{
    GDL_DISPLAY_ID_0 = 0,       ///< [Pipe A] Main display/HDMI
    GDL_DISPLAY_ID_1,           ///< [Pipe B] Secondary display/Composite
    GDL_DISPLAY_ID_UNDEFINED    ///< Undefined Pipe Internal Use Only
} gdl_display_id_t;


/** @ingroup disp_mode

    This structure describes a TV display mode.
*/
typedef struct
{
    gdl_uint32    width;      ///< Active display width in pixels
    gdl_uint32    height;     ///< Active display height in pixels
    gdl_refresh_t refresh;    /**< Refresh frame rate: frames/sec in
                                   progressive display modes, fields/sec in
                                   interlaced display modes.
                              */
    gdl_boolean_t interlaced; ///< GDL_TRUE=>interlaced, GDL_FALSE=>progressive
} gdl_tvmode_t;

/** @ingroup disp_mode
    Defines supported color spaces

*/
typedef enum
{
    GDL_COLOR_SPACE_BT601, ///< Normally used for Standard Definition YCbCr content
    GDL_COLOR_SPACE_BT709, ///< Normally used for High Definition YCbCr content
    GDL_COLOR_SPACE_RGB,   ///< Used for all RGB pixel formats
    GDL_COLOR_SPACE_COUNT  ///< Number of entries in this enumeration
} gdl_color_space_t;


/**@ingroup disp_mode

    Values that can be OR'd together to create an entry for the flags field of
    the #gdl_display_info_t structure.
    */
typedef enum
{
    GDL_DISPLAY_ZOOM_TO_FIT      = 0x00000001,
        /**<
        Display 1 can only be configured for SD display modes, which have a
        4:3 aspect ratio. When display 0 is in a 16:9 display mode, the
        setting of this flag is consulted to determine how the display 0 image
        should be scaled for output on display 1.
        
        The default (if this flag is not specified) is to scale the 16:9 image
        to 4:3 -- no data is lost, no black bars are displayed, but the image
        is distorted.  In this case, the application may wish to enable Wide
        Screen Signaling in the port driver.

        Specifying this flag selects a "zoom to fit" (letterboxing) scaling
        policy. This maintains the 16:9 aspect ratio on display 1 by scaling
        the image to the point where its width fills the display area. Black
        bars appear above and below the image.

        @note
        This flag can @b only be specified for display 1.
        */

    GDL_DISPLAY_SHARPEN          = 0x00000002
        /**<
        This flag can be selected to "sharpen" the output on display 1 when
        displaying a static, full-screen graphic frame.

        Setting this flag will @b only produce optimal results if:
        - the graphics plane is configured to @b full-screen resolution (the
          resolution to which display 0 is currently configured), and
        - the original graphics content has the same resolution as the current
          configuration of display 1 (720x480 or 720x576).

        Setting this flag will probably @b degrade the quality of any animated
        or video plane being displayed.

        @note
        This flag can @b only be specified for display 1.
        */
} gdl_display_flag_t;


/** @ingroup disp_mode

    This structure describes the state of one of the displays.
*/
typedef struct
{
    gdl_display_id_t    id;
        /**< The ID of the display. */

    gdl_tvmode_t        tvmode;
        /**< The display mode */

    gdl_uint32          flags;
        /**<
        This is a set of bit flags formed by OR-ing together members of the
        #gdl_display_flag_t enumeration.
        */

    gdl_uint32          bg_color;
        /**<
        An RGB 10/10/10 value designating the "canvas" color that will appear
        on any part of the display that is not completely obscured by one or
        more opaque planes.
        @note
        Display 1's bg_color will never been seen unless the
        #GDL_DISPLAY_ZOOM_TO_FIT flag is set, in which case it will be used as
        the color of the horizontal bars used to "letter-box" 16:9 images on
        display 1. In all other cases, the blended display 0 image is always
        scaled to fit the display 1 exactly, leaving no portion of the canvas
        uncovered; even if there are no planes enabled on display 0, it
        is the display 0 bg_color that will be seen on display 1.
        */

    gdl_gamma_t         gamma;
        /**< Additive gamma to be applied to blended output sent to the display.
         */

    gdl_color_space_t   color_space;
        /**< The colorspace in which pixels are output on this display.
             Planes on this pipe whose source pixels are in a different color
             space will be configured to perform color space conversion before
             outputting pixels to the blender.
        */
} gdl_display_info_t;


/*----------------------------------------------------------------------
 *                 P L A N E   M A N A G E M E N T
 *---------------------------------------------------------------------*/


/** @ingroup plane_management
    Plane IDs
*/
typedef enum
{
    GDL_PLANE_ID_UNDEFINED = 0,     /**< A plane ID guaranteed to be invalid.
                                         Useful for initializing variables.
                                    */
    GDL_PLANE_ID_IAP_A,             ///< Indexed alpha plane A
    GDL_PLANE_ID_IAP_B,             ///< Indexed alpha plane B
    GDL_PLANE_ID_UPP_A,             ///< Universal pixel plane A
    GDL_PLANE_ID_UPP_B,             ///< Universal pixel plane B
    GDL_PLANE_ID_UPP_C,             ///< Universal pixel plane C
    GDL_PLANE_ID_UPP_D,             ///< Universal pixel plane D
    GDL_PLANE_ID_UPP_E,             /**< Universal pixel plane E
                                      *  -- only available in blender
                                      *     configuration 1.
                                      */
    GDL_MAX_PLANES=GDL_PLANE_ID_UPP_E, ///< The total number of planes

#ifndef DOXYGEN_SKIP /* Omit the following when docs are generated */
    /* THESE ARE FOR INTERNAL DRIVER USE ONLY */
    GDL_PLANE_ID_UPP_WB,
    GDL_PLANE_ID_WBP,
    GDL_MAX_PLANES_INTERNAL=GDL_PLANE_ID_WBP
#endif
} gdl_plane_id_t;


/** @ingroup plane_management

    This structure specifies a color in a manner independent of exact pixel
    format.  It is generally used to supply the color component values of a
    pixel in a context in which the pixel format and/or color space are
    indicated separately by enumerators of the appropriate type.

    For formats that include alpha components (or that are alpha-only), the
    alpha_index field holds the alpha value.

    For palettized (CLUT) color formats, only the alpha_index field is used, and
    it holds the 0-based palette index.

    For other RGB formats:
    - r_y holds the Red   color component
    - g_u holds the Green color component
    - b_v holds the Blue  color component

    For YUV (YCbCr) formats:
    - r_y holds the Y component
    - g_u holds the U (Cb) component
    - b_v holds the V (Cr) component
*/
typedef struct
{
    gdl_uint16 alpha_index; ///< Alpha or Index component
    gdl_uint16 r_y;         ///< Red,   Y, or Y' component
    gdl_uint16 g_u;         ///< Green, U, or Cb component
    gdl_uint16 b_v;         ///< Blue,  V, or Cr component
} gdl_color_t;


/** @ingroup plane_management
    Flags indicating how a surface flip should be scheduled by gdl_flip().
*/
typedef enum
{
    GDL_FLIP_ASYNC = 3,
        /**<
        gdl_flip() returns immediately to the caller. The flip begins on the
        next VBlank, as long as it is not over-ridden by a new flip to the same
        plane before then.
        */

    GDL_FLIP_SYNC = 4,
        /**<
        gdl_flip() does not return to the caller until scan-out of the surface
        onto the display has begun. New flip can be submitted at this point
        to the same plane without over-writing the previous flip.
        */
} gdl_flip_t;


/** @ingroup plane_management
    This enumeration defines the various attributes that may be implemented by
    a hardware plane.  These values are used in three different ways:

    - to determine plane capabilities\n\n
      The function gdl_plane_capabilities() returns a plane capability array.
      To determine if an attribute is supported by the plane, use the
      corresponding enumeration value as an index into the array.  If the
      indexed entry is non-zero, the attribute is supported; otherwise it isn't.

    - to configure a plane\n\n
      The various set functions (e.g., gdl_plane_set_int()) allow the setting
      of individual attributes of the plane by specifying attribute name-value
      pairs.  The "names" in these pairs are members of this enumeration.

    - to examine the configuration of a plane\n\n
      The various get functions (e.g., gdl_plane_get_int()) allow the current
      value of an attribute to be queried by name.  The "names" used in these
      queries are members of this enumeration.

    The descriptions of the individual enumerated values contain the following
    fields:
    - @b Capability: what a non-zero entry in the corresponding entry of
         the capabilities array indicates.\n
    - @b Attribute: what the attribute controls.\n
    - @b Value: the data type of the attribute, which determines which get/set
         function should be used to manipulate it.  Where relevant, the
         range of legal values for the attribute is listed.  Note that
         enumerations are treated as uint values with a range restricted by the
         enum definition.
    - @b Default: the default setting of the attribute when the system is
         powered up.

    On the Intel® CE Media Processors, the capabilities supported by the planes
    are as follows:
    - All planes:
        - #GDL_PLANE_ALPHA_GLOBAL
        - #GDL_PLANE_ALPHA_PREMULT
        - #GDL_PLANE_DISPLAYED_SURFACE
        - #GDL_PLANE_DST_RECT
        - #GDL_PLANE_FLIP
        - #GDL_PLANE_LINE_REPLICATION
        - #GDL_PLANE_PIXEL_EXTENSION
        - #GDL_PLANE_PIXEL_FORMAT
        - #GDL_PLANE_PIXEL_REPLICATION
        - #GDL_PLANE_SRC_COLOR_SPACE
        - #GDL_PLANE_SRC_RECT
        - #GDL_PLANE_ZORDER
    - Universal pixel planes:
        - #GDL_PLANE_CHROMA_KEY_SRC_ENABLE,
        - #GDL_PLANE_CHROMA_KEY_SRC_HIGH,
        - #GDL_PLANE_CHROMA_KEY_SRC_LOW,
        - #GDL_PLANE_NUM_GFX_SURFACES,
        - #GDL_PLANE_REVERSE_GAMMA_TYPE,
        - #GDL_PLANE_UPSCALE
        - #GDL_PLANE_VBD_DEST_OVERRIDE
        - #GDL_PLANE_VBD_MUTE
    - Indexed/alpha planes:
        - #GDL_PLANE_ALPHA_OUT
*/
typedef enum
{
    GDL_PLANE_ALPHA_GLOBAL,
        /**<
        @b Capability: the plane supports a settable global alpha value.\n
        @b Attribute: the current global alpha value.\n
        @b Value: uint in [0, 255]\n
        @b Default: 255 (opaque)\n
        \n
        */

    GDL_PLANE_ALPHA_OUT,
        /**<
        @b Capability: the plane can provide alpha values for another plane.\n
        @b Attribute: the ID of the plane to which the alpha values should be
            sent.\n
        @b Value: gdl_plane_id_t\n
        @b Default: #GDL_PLANE_ID_UNDEFINED\n
        <b>Programming Notes</b>
        - This attribute is available on IAPs (Indexed/Alpha planes) only.
        - When the value of this attribute is set to #GDL_PLANE_ID_UNDEFINED,
          the IAP is configured to output 8-bit palette-based pixels, and its
          #GDL_PLANE_PIXEL_FORMAT attribute must be set to specify an 8-bit
          CLUT pixel format.
        - When the value of this attribute is set to a valid plane ID, the IAP
          is configured to provide alpha values for the specified plane, and its
          #GDL_PLANE_PIXEL_FORMAT attribute must specify an alpha format.
          Each pixel in the IAP provides the alpha value for the pixel it
          overlaps on-screen in the specified destination plane.
        - On the Intel® CE Media Processors,
            - IAP A can provide alpha values for any one of UPPs A, B, and C.
            - IAP B can only provide alpha values for UPP D.
            .
        - If the destination rectangle of an IAP is different from that of
          its associated UPP, the UPP pixels where there is no overlap will
          receive alpha values of @b zero and will become transparent.
        - Similarly, if an IAP is @b disabled without removing the assignment,
          the associated UPP will become completely transparent.
        .
        \n
        */

    GDL_PLANE_ALPHA_PREMULT,
        /**<
        @b Capability: the plane's pixel values can be interpreted as containing
        premultiplied alpha values.\n
        @b Attribute: enables/disables use of premultiplied alpha.\n
        @b Value: uint (#gdl_boolean_t)\n
        @b Default: #GDL_FALSE (disabled)\n
        <b>Programming Notes</b>
        - This value is ignored if the plane's current pixel format does not
          include a per-pixel alpha value.
        - @b CAUTION: This attribute cannot operate correctly in conjunction
          with the following features.  An attempt to enable premultiplied alpha
          and any of the following at the same time will return an error of type
          #GDL_ERR_PREMULT_CONFLICT.
          - chroma keying on the same plane (#GDL_PLANE_CHROMA_KEY_SRC_ENABLE
            set to #GDL_TRUE)
          - gamma removal from the same plane (#GDL_PLANE_REVERSE_GAMMA_TYPE set
            to a value other than #GDL_GAMMA_LINEAR).
          - color space conversion (the value of the plane's
            #GDL_PLANE_SRC_COLOR_SPACE attribute is different from the color
            space of the display to which it is connected).
          - a non-RGB pixel format.
          .
        .
        \n
        */

    GDL_PLANE_CHROMA_KEY_SRC_ENABLE,
        /**<
        @b Capability: The plane supports source luma/chroma keying.\n
        @b Attribute: Enables/disables luma/chroma keying on the plane.\n
                      Actual chroma key values are set through the
                      #GDL_PLANE_CHROMA_KEY_SRC_HIGH and
                      #GDL_PLANE_CHROMA_KEY_SRC_LOW attributes.\n
        @b Value: #gdl_boolean_t\n
        @b Default: #GDL_FALSE (Chroma keying is disabled)\n
        \n
        */

    GDL_PLANE_CHROMA_KEY_SRC_HIGH,
        /**<
        @b Capability: Plane supports source chroma key range.\n
        @b Attribute: The upper limit of the luma/chroma keying range. This
                      value is ignored unless the
                      #GDL_PLANE_CHROMA_KEY_SRC_ENABLE is set to #GDL_TRUE\n
        @b Value: #gdl_color_t\n
        @b Default: 0,0,0\n
        <b>Programming Notes</b>
        - The value is interpreted as RGB or YCbCr according to the plane's
          current #GDL_PLANE_SRC_COLOR_SPACE attribute setting.
        - The number of bits used in each field of the structure should
          correspond to the current #GDL_PLANE_PIXEL_FORMAT attribute.  E.g.,
          if the pixel format is #GDL_PF_RGB_16, y_g should be a 6-bit value and
          cb_b/cr_r should be 5-bit values.
        - The programmer is responsible for correctly coordinating the values of
          #GDL_PLANE_CHROMA_KEY_SRC_HIGH and #GDL_PLANE_CHROMA_KEY_SRC_LOW with
          the settings of #GDL_PLANE_SRC_COLOR_SPACE, #GDL_PLANE_PIXEL_FORMAT,
          and #GDL_PLANE_ALPHA_PREMULT.
        .
        \n
        */

    GDL_PLANE_CHROMA_KEY_SRC_LOW,
        /**<
        Same as #GDL_PLANE_CHROMA_KEY_SRC_HIGH, except that it identifies the
        lower end of the luma/chroma keying range.\n\n
        */

    GDL_PLANE_DISPLAYED_SURFACE,
        /**<
        @b Capability: Surfaces can be flipped onto the plane with the
           gdl_flip() function. See also #GDL_PLANE_FLIP.\n
        @b Attribute: The ID of the surface currently being scanned out by the
           plane.  The surface is at least partially visible.  If the value of
           this attribute is #GDL_PLANE_ID_UNDEFINED, the plane is disabled.\n
        @b Value: uint (#gdl_surface_id_t)\n
        @b Default: N/A. <b>THIS VALUE IS READ-ONLY</b>.\n
        */

    GDL_PLANE_DST_RECT,
        /**<
        @b Capability: The plane's dimensions and its location on the display
            can be changed.\n
        @b Attribute: A rectangle defining the dimensions of the plane and its
            location on the display.\n
        @b Value: rect (#gdl_rectangle_t)\n
        @b Default: 320x240\n
        <b>Programming Notes</b>
        - The rectangle's origin:
            - is relative to the upper left corner of the display.
            - must have non-negative coordinates.
            - must lie within the active display region.
        - The rectangle's height and width must be such that the lower right
          corner of the rectangle also lies within the active display region.
          Line and pixel replication (if enabled) must be taken into account
          when determining the rectangle's size.
        - On the Intel® CE Media Processors, the plane can not be less than 16
        pixels wide by 2 pixels high.
        - If the display is in an interlaced mode:
            - the y-coordinate of the origin must specify an even-numbered
              display line (the top line of the display is line 0).
            - the height must be even.
            .
        .
        \n
        */

    GDL_PLANE_FLIP,
        /**<
        @b Capability: Surfaces can be flipped onto the plane with the
            gdl_flip() function. See also #GDL_PLANE_DISPLAYED_SURFACE.\n
        @b Attribute: The ID of the surface currently flipped onto the plane.\n

           "Flipping" a surface is the act of programming the VDC registers
           with the address of the surface buffer.  However, the plane does not
           begin to scan out the contents of the surface until the next
           Framestart signal after the programming is done. (Framestart occurs
           sometime between VBlank and VSync). Therefore, the surface whose
           ID is returned as this attribute may not actually be visible
           yet.  See #GDL_PLANE_DISPLAYED_SURFACE for the ID of the
           currently displayed surface.  If the value of GDL_PLANE_FLIP
           is #GDL_PLANE_ID_UNDEFINED, the plane will be @b disabled at the
           next Framestart (if it isn't already).\n
        @b Value: uint (#gdl_surface_id_t)\n
        @b Default: N/A. <b>THIS VALUE IS READ-ONLY</b>.\n
        \n
        */

    GDL_PLANE_LINE_REPLICATION,
        /**<
        @b Capability: The plane can output each scan line multiple times.\n
        @b Attribute: The number of times each line should be output.  A value
           of 1 indicates normal output; a value of 2 indicates line-doubling.\n
        @b Value: uint in [1,2]\n
        @b Default: 1\n
        <b>Programming Notes</b>
           - The #GDL_PLANE_DST_RECT attribute should be set to the
             non-replicated dimensions.  E.g, a plane whose dimensions are
             720x240 will be displayed as 720x240 when line replication is 1,
             and 720x480 when line replication is 2. #GDL_ERR_DISPLAY_CONFLICT
             will be returned if turning on line replication would cause the
             plane not to fit entirely within the display.
           - Pixel and line replication are not supported while
             #GDL_PLANE_UPSCALE attribute is enabled on that plane.
           .
        \n
        */

    GDL_PLANE_NUM_GFX_SURFACES,
        /**<
        @b Capability: The plane can support flip chains for the graphics
           driver APIs.\n
        @b Attribute: Indicates whether the plane will be double-, or
           triple-buffered for flip operations performed via the graphics APIs.
           The value of this attribute is only used when the plane's ID is
           passed to eglWindowSurfaceCreate() (see the Intel® CE Media
           Processors Graphics Driver Programming Guide):
           - The EGL function examines the attribute to determine the number of
             flippable surfaces to request from the display driver (subject to
             memory availability).
           - The pixel format and dimensions of the surfaces will be based on
             the plane's pixel format and dimensions at the time of the call.
           .
        @b Value: uint in [2,3]\n
        @b Default: 3\n
        \n
        */

    GDL_PLANE_PIXEL_EXTENSION,
        /**<
        @b Capability: The plane supports pixel component extension.
            Internally, all planes extend their pixels to 10-bits per component
            prior to blending.  See the description of #gdl_pixel_extension_t
            for further details.\n
        @b Attribute: Method used to extend pixel component values.\n
        @b Value: uint (#gdl_pixel_extension_t)\n
        @b Default: #GDL_PIXEL_EXTENSION_ZERO_PAD\n
        \n
        */

    GDL_PLANE_PIXEL_FORMAT,
        /**<
        @b Capability: The plane's pixel format can be changed.\n
        @b Attribute: The plane's current pixel format.\n
        @b Value: uint (#gdl_pixel_format_t)\n
        @b Default:
           - for UPPs: #GDL_PF_NV16
           - for IAPs: #GDL_PF_ARGB_8
           .
        \n
        */

    GDL_PLANE_PIXEL_REPLICATION,
        /**<
        @b Capability: The plane can output each pixel on a line multiple
            times.\n
        @b Attribute: The number of times each pixel on a line is output.  A
            value of 1 indicates normal output; a value of 2 indicates
            pixel-doubling.\n
        @b Value: uint in [1,2]\n
        @b Default: 1\n
        <b>Programming Notes</b>
           - The #GDL_PLANE_DST_RECT attribute should be set to the
             non-replicated dimensions.  E.g, a plane whose dimensions are
             360x480 will be displayed as 360x480 when pixel replication is 1,
             and 720x480 when pixel replication is 2. #GDL_ERR_DISPLAY_CONFLICT
             will be returned if turning on pixel replication would cause the
             plane not to fit entirely within the display.\n
           - Pixel and line replication are not supported while
             #GDL_PLANE_UPSCALE attribute is enabled on that plane.
           .
        \n
        */

    GDL_PLANE_REVERSE_GAMMA_TYPE,
        /**<
        @b Capability: The plane can remove gamma correction from source
        material prior to outputting pixels for blending with other planes.\n
        @b Attribute: The type of reverse gamma correction currently in use.\n
        @b Value: uint (#gdl_gamma_t)\n
        @b Default: #GDL_GAMMA_LINEAR\n
        <b>Programming Notes</b>
            - Gamma removal will work correctly only if the gamma was originally
              applied in the same color space as the one in which blending is
              currently being done (the current color space of Display 0).
            - Note that it is impossible to detect from a video clip the color
              space in which gamma was applied.  The color space of the clip
              can be determined, e.g. BT601, but the gamma may have been
              applied in RGB color space by the camera before it output BT601.
            - It is the application's responsibility to determine that the
              feature will work correctly in a given display mode for a given
              clip.  If the results do not look correct, it is likely that this
              requirement was not met.
        \n
        */

    GDL_PLANE_UPSCALE,
        /**<
        @b Capability: The plane can perform upscaling of the surfaces flipped
            to the plane based on the relative sizes of the plane's source and
            destination rectangles.\n
        @b Attribute: Enables/disables upscaling on the plane.\n
        @b Value: #gdl_boolean_t\n
        @b Default: #GDL_FALSE\n
        <b>Programming Notes</b>
           - Only one UPP can support upscaling at a time. If upscaling is
             already enabled for a UPP, it must be disabled on that UPP, before
             it can be enabled on a new one.
           - It is an error to enable plane upscaling and pixel/line doubling
             at the same time. The programmer must explicitly disable one when
             (or before) enabling the other.
           - It may be necessary to change the source and/or destination
             rectangles of the plane in the same configuration transaction as
             the enabling / disabling of the upscaling in order to achieve the
             desired results. For example, since UPPs are only capable of
             upscaling, it is an error to enable scaling with the dimensions of
             the source rectangle greater than those of the destination
             rectangle.\n
           - It is assumed that upscale will be enabled only for progressive
             content such as graphics. Upscaling an interlaced content may
             result in a bad output image quality.
           - When upscaling is enabled, the width of the #GDL_PLANE_SRC_RECT
             cannot exceed 1280.
           .
        \n
        */

    GDL_PLANE_SRC_COLOR_SPACE,
        /**<
        @b Capability: the plane can perform color space conversion when its
            pixel data is output to the blender.\n
        @b Attribute: an enumerated value indicating the color space of the
            plane's source pixels (the color space of pixels in surfaces that
            will be flipped onto the plane).  The pixels will be converted from
            that color space to the current blender color space when they are
            output by the plane.  NOTE: on IAPs, this attribute is ignored when
            the plane is used in alpha output mode (i.e., when the
            #GDL_PLANE_ALPHA_OUT attribute is set to a value other than
            #GDL_PLANE_ID_UNDEFINED)\n
        @b Value: uint (#gdl_color_space_t)\n
        @b Default:
            - For UPPs: #GDL_COLOR_SPACE_BT601
            - For IAPs: #GDL_COLOR_SPACE_RGB
            .
        \n
        */

    GDL_PLANE_SRC_RECT,
        /**<
        @b Capability: The plane supports the ability to pan and/or scale source
          surfaces\n
        @b Attribute: A rectangle applied to all surfaces flipped onto the
             plane, where:
             - The @b origin allows panning over a surface that is larger than
               the plane. The origin is specified as non-negative coordinates
               relative to the upper left corner of the surface. It indicates
               the surface pixel that will appear at the origin (upper left
               corner) of the plane.
             - The @b height and @b width are interpreted as follows:
                - If the plane is a UPP on which scaling has been enabled (i.e.,
                  the #GDL_PLANE_UPSCALE attribute has been set to #GDL_TRUE),
                  the region of the surface specified by these dimensions is
                  scaled to fit the destination rectangle.  Since downscaling
                  is not supported, the dimensions of the source rectangle may
                  not exceed those of the destination rectangle.
                - Otherwise, the height and width of the source rectangle
                  @b are @b ignored.  The region of the surface that is
                  displayed is determined by the height and width of the
                  destination rectangle.
             .
        @b Value: rect (#gdl_rectangle_t)\n
        @b Default: Initial value of #GDL_PLANE_DST_RECT.\n
        <b>Programming Notes</b>
            - If an interlaced display mode is in effect, the low order bit of
              the y-coordinate is ignored (the origin will always be on an
              even-numbered display line).
            - A flip will fail if the plane's source rectangle results in an
              origin or a lower right corner that does not lie within the
              surface being flipped.
            - When #GDL_PLANE_UPSCALE is enabled, the width of the source
              rectangle cannot exceed 1280.
            .
        \n
        */

    GDL_PLANE_VBD_DEST_OVERRIDE,
        /**<
        @b Capability:
            The plane can be made a video sink for the SMD video
            pipeline (by passing its ID to ismd_vidrend_set_video_plane()).
            \n
        @b Attribute:
            Once a plane has been designated a video sink, video frames are
            displayed on it automatically by the SMD pipeline via the
            kernel-level VBD interface to the display driver.  By default, if
            the dimensions of a frame passed in this way do not match the
            plane's current destination rectangle, an opaque black frame is
            displayed instead.  This is because events such as channel changes
            might cause an application to reconfigure the plane for a new
            resolution while there are still one or two video frames with the
            old resolution still in the pipeline. This behavior can be
            overridden by setting GDL_PLANE_VBD_DEST_OVERRIDE to GDL_TRUE; in
            this case, the plane's destination rectangle will be automatically
            reprogrammed to accommodate any change in the sizes of frames
            passed via the VBD interface.\n
        @b Value: gdl_boolean_t\n
        @b Default: GDL_FALSE\n
        @b Note:
             - If the value is set to GDL_TRUE and video is resized through VBD
             interface a "GDL_APP_EVENT_VBD_RECT_CHANGE" event will be sent to
             all clients registered for that event. See #gdl_app_event_t for
             more details on application events.\n
        */

    GDL_PLANE_VBD_MUTE,
        /**<
        @b Capability:
            The plane can be made a video sink for the SMD video
            pipeline (by passing its ID to ismd_vidrend_set_video_plane()).\n
        @b Attribute:
            Once a plane has been designated a video sink, video frames are
            displayed on it automatically by the SMD video pipeline. Whenever
            this attribute is set to #GDL_TRUE, the display driver will ignore
            all frames flipped to the plane by the video pipeline, and the
            plane can be used for any purpose by the application.\n
        @b Value: gdl_boolean_t\n
        @b Default: GDL_FALSE\n
        */

    GDL_PLANE_ZORDER,
        /**<
        @b Capability: Planes have different "depth" levels, and enabled planes
            are blended in order from lowest to top-most.\n
        @b Attribute: The plane's Z-order.  <b>THIS VALUE IS READ-ONLY</b>.\n
        @b Value: uint [0,NumberOfPlanes-1], where:
        - N is the number of planes in the system, and
        - 0 is the top-most plane.
        .
        @b Default: The values returned for each plane are:
        - IAPB: 0
        - IAPA: 1
        - UPPE: 2
        - UPPD: 3
        - UPPC: 4
        - UPPB: 5
        - UPPA: 6
        .
        \n
        */

    GDL_PLANE_ATTRIBUTE_COUNT
        /**<
        The total number of defined plane attributes (not an actual attribute
        ID).
        */
} gdl_plane_attr_t;


/** @ingroup plane_management
    The total number of characters that may be present in a plane's
    name string, including the zero terminator.
*/
#define GDL_PLANE_NAME_LENGTH   32

/** @ingroup plane_management

    Structure that describes the hardware of a display plane.
    This information is retrieved by a call to gdl_plane_capabilities().
*/
typedef struct
{
    gdl_plane_id_t  id;
        /**< The ID of the plane described by this structure. */

    char            name[ GDL_PLANE_NAME_LENGTH ];
        /**<
        The name of the plane as a 0-terminated string, for use in messages
        and debugging.
        */

    gdl_uint8       attr[ GDL_PLANE_ATTRIBUTE_COUNT ];
        /**<
        The programmable attributes of the plane.

        The values of the #gdl_plane_attr_t enumeration are used to index this
        array: if the corresponding entry in the array is non-zero, the
        attribute is supported by the plane.

        See the descriptions of the members of #gdl_plane_attr_t for further
        information about individual attributes.
        */

    gdl_uint8       pixel_formats[ GDL_PF_COUNT ];
        /**<
        The pixel formats supported by the plane.

        The values of the #gdl_pixel_format_t enumeration are used to index this
        array: if the corresponding entry in the array is non-zero, that pixel
        format is supported by the plane.
        */

    gdl_rectangle_t max_dst_rect;
        /**<
        The width/height fields of this rectangle specify the maximum dimensions
        allowed by the hardware for the plane's destination rectangle
        (on-display window).
        */

    gdl_rectangle_t min_dst_rect;
        /**<
        The width/height fields of this rectangle specify the minimum dimensions
        allowed by the hardware for the plane's destination rectangle
        (on-display window).
        */

} gdl_plane_info_t;


/*----------------------------------------------------------------------
 *              S U R F A C E   M A N A G E M E N T
 *---------------------------------------------------------------------*/

/** @ingroup surf
 *
    A structure describing an individual palette element.

*/
typedef struct
{
    gdl_uint8 a;               ///< Alpha component
    gdl_uint8 r_y;             ///< Red (or Y) component
    gdl_uint8 g_u;             ///< Green (or U/Cb) component
    gdl_uint8 b_v;             ///< Blue (or V/Cr) component
} gdl_palette_entry_t;


/** @ingroup surf
    A structure describing an 8-bit indexed palette (CLUT).
*/
typedef struct
{
    gdl_palette_entry_t data[256]; ///< Palette elements
    gdl_uint32          length;    ///< Number of valid entries in palette
} gdl_palette_t;


/** @ingroup surf

    Surfaces are referenced by IDs.  The IDs are unsigned integers.
*/
typedef enum
{
    GDL_SURFACE_INVALID = 0
        /**<
        This value is never allocated for a valid surface.  It can be used
        in data structures to indicate an invalid or uninitialized surface ID.
        */
} gdl_surface_id_t;


/**@ingroup surf

    These flags are used with gdl_alloc_surface() to indicate how the surface
    is intended to be used.
*/
typedef enum
{
    GDL_SURFACE_CACHED      = 0x00000002,
        /**<
        This flag should be set on a surface that the caller would like to
        be in 'cached' memory. If the surface is passed to any other execution
        unit besides cpu, cached surface must be flushed first using
        #gdl_flush_surface API call. Automatic flush of the cache will occur
        internally during #gdl_flip call.
        */
    GDL_SURFACE_DISABLE_AUTOCLEANUP = 0x00000004,
        /**
        This flag when set disables surface from participating in automatic
        cleanup. Upon exit of applcation remaining surfaces marked with this
        flag will not be automatically deleted.
        @bNote: This flag should only be set by applications that perform their
        own memory management.
        */
} gdl_surface_flag_t;


/** @ingroup surf
    This structure describes a surface.
*/
typedef struct
{
    gdl_surface_id_t    id;
        /**< Surface ID, used in GDL calls that operate on this surface */

    gdl_uint32          flags;
        /**< Any combination of OR'd values of type #gdl_surface_flag_t. */

    gdl_pixel_format_t  pixel_format;
        /**< Pixel format of the surface of type #gdl_pixel_format_t. */

    gdl_uint32          width;
        /**<
        Surface width in pixels.  In the case of a YUV format surface, this is
        also the number of active pixels in a line of the Y-plane.
        */

    gdl_uint32          height;
        /**<
        Surface height in pixels.  In the case of a YUV format surface, this is
        also the number of lines in the Y-plane.
        */

    gdl_uint32          size;
        /**<
        Total number of bytes allocated for the surface, including any padding
        added for stride alignment.
        */

    gdl_uint32          pitch;
        /**<
        Byte stride; i.e., the byte offset that must be added to the address
        of pixel in order to access the pixel with the same x-offset in the
        line below it. In the case of a YUV format surface, this is the pitch
        of the Y plane.
        */

    gdl_uint32         phys_addr;
        /**<
        Physical address of the surface.
        */

    gdl_uint32         heap_phys_addr;
        /**<
        Physical address of the heaps base.
        */

    gdl_uint32          y_size;
        /**<
         NOT USED FOR PACKED PIXEL FORMATS.
         For YUV planar and pseudo-planar formats, this is the total number of
         bytes allocated for the Y plane, including any padding for stride
         alignment.  Note that the Y plane always begins at offset 0 into the
         surface.
         */

    gdl_uint32          u_offset;
        /**<
        NOT USED FOR PACKED PIXEL FORMATS.
        For YUV planar formats, this is the (non-zero) offset from the
        start of the surface to the start of the U plane. For YUV
        pseudo-planar surfaces, this is the (non-zero) offset of the
        interleaved UV samples.
        */

    gdl_uint32          v_offset;
        /**<
        NOT USED FOR PACKED PIXEL FORMATS.
        For YUV planar formats, this is the (non-zero) offset from the start of
        the surface to the start of the V plane.  For YUV pseudo-planar
        surfaces, this value is meaningless and will be set to 0.
        */

    gdl_uint32          uv_size;
        /**<
        NOT USED FOR PACKED PIXEL FORMATS.
        The total number of bytes allocated for each of the subsampled
        chrominance planes, including any padding for stride alignment.  For
        YUV planar formats, both the U & V planes are this size. For YUV
        pseudo-planar formats, this is the total allocated for the interleaved
        UV samples.
        */

    gdl_uint32          uv_pitch;
        /**<
        NOT USED FOR PACKED PIXEL FORMATS.
        The byte stride of the subsampled chrominance planes. For YUV planar
        formats, both the U and V planes have this pitch. For pseudo-planar
        formats, this is the pitch of the interleaved UV samples
        */

} gdl_surface_info_t;


/** @ingroup general
    This enumeration represents events that can be used for callback
    registration with the gdl_event_register() API.
*/
typedef enum
{
    GDL_APP_EVENT_HDCP_FAILURE=0,    ///< HDCP has failed
    GDL_APP_EVENT_HDCP_SUCCESS,      ///< HDCP authentication has succeeded
    GDL_APP_EVENT_HOTPLUG_DETECT,    ///< HDMI cable was connected
    GDL_APP_EVENT_HOTPLUG_LOSS,      ///< HDMI cable was disconnected
    GDL_APP_EVENT_VBD_RECT_CHANGE,   ///< VBD rectangle dimensions changed
    GDL_APP_EVENT_COUNT,
} gdl_app_event_t;


/** @ingroup general
    Type for a callback function used with #gdl_event_register API
    call. See #gdl_event_register for more information
*/
typedef void (*gdl_event_callback_t)(gdl_app_event_t event, void * user_data);


/** @ingroup debug
    These flags are used with gdl_debug_log_start() to indicate the type of
    information to be logged.
*/

typedef enum
{
    GDL_LOG_THREAD_ID   = 0x00000001, ///< Log Thread ids
    GDL_LOG_TIMESTAMP   = 0x00000002, ///< Log timestamp for function entry/exit
    GDL_LOG_SURFACES    = 0x00000004, ///< Log surface-related information
    GDL_LOG_PLANES      = 0x00000008, ///< Log plane-related information
} gdl_log_flag_t;

#endif
