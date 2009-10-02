//-----------------------------------------------------------------------------
// Copyright (c) 2006-2009 Intel Corporation
//
// DISTRIBUTABLE AS SAMPLE SOURCE SOFTWARE
//
// This Distributable As Sample Source Software is subject to the terms and
// conditions of the Intel Software License Agreement provided with the Intel(R)
// Media Processor Software Development Kit.
//-----------------------------------------------------------------------------

#ifndef _GDL_H_
#define _GDL_H_

#include "gdl_types.h"
#include "gdl_pd.h"

#if defined(__cplusplus)
extern "C" {
#endif

/*------------------------------------------------------------------------------
 * IMPORTANT!
 *
 * Change of a structure/type or a function prototype requires rolling of an
 * appropriate version number. For more information see gdl_version.h
 *----------------------------------------------------------------------------*/

/**@mainpage
 * This document is applicable to the Intel® Media Processor CE 3100 and the
 * Sodaville processor. In this document, both processors are referred to as
 * the Intel® CE Media Processors. When functional differences occur,
 * applicability to platform/silicon will be delineated.
 */

/**@defgroup general        General */

/**@defgroup disp_mode      Display Mode*/

/**@defgroup plane_management Plane Management

  A plane is a unit of hardware that can supply pixel data to be composited
  with the pixel data of other planes for output to the display.  Different
  kinds of planes have different capabilities.  The Intel® CE Media Processors
  contain two kinds of planes:
  - Universal Pixel Planes (UPPs)
  - Indexed/Alpha Planes (IAPs)

  The Plane Management API provides a generic way to configure a plane and
  populate it with pixel data.  The main components of the API are:

  - <b>A predefined set of plane attributes.</b> Attributes are specified as
    name/value pairs. The enumeration #gdl_plane_attr_t defines the attribute
    names. The documentation of this data type describes each attribute
    including:
    - the plane capability controlled by the attribute.
    - the data type of the attribute's value.
    - the range of valid values that can be assigned to the attribute.
    - the default value of the attribute.

  - <b>The ability to query a plane for its capabilities.</b> Each plane type
    may support a different subset of the defined attributes and different
    set of pixel formats.  gdl_plane_capabilities() returns a description of
    the attributes and pixel formats supported by a specified plane.

  - <b>A set of attribute query functions.</b> These functions return the
    current hardware setting of the specified attribute on the specified plane.
    The only difference between the functions is the data type returned.
    - gdl_plane_get_attr()
    - gdl_plane_get_int()
    - gdl_plane_get_uint()
    - gdl_plane_get_rect()

  - <b>A set of attribute configuration functions.</b> For each of the query
    functions there is a corresponding set function.  All attribute set
    function calls must be bracketed by calls to configuration begin/end
    functions.  The specified attributes values are "batched" and executed
    only when the end function is called.  Batching provides atomicity of
    configuration changes and avoids getting into invalid states when related
    attributes must be changed at the same time.  The sequence of calls made
    to configure a plane is as follows:
    - gdl_plane_config_begin()
    - one or more calls to:
          - gdl_plane_set_attr()
          - gdl_plane_set_int()
          - gdl_plane_set_uint()
          - gdl_plane_set_rect()
    - gdl_plane_config_end()

  - <b>A surface flip function.</b>  A surface is a memory buffer containing
    pixel information.  See @ref surface for more information.  The gdl_flip()
    function "flips" a surface onto a plane, making the pixels in that surface
    visible.

  @warning  Plane defaults (including if they are enabled or disabled) are
            set when the display driver is loaded, and when
            @ref gdl_plane_reset() and @ref gdl_plane_reset_all() are called.
            They are <b>not</b> set by @ref gdl_init().
 */

/**@defgroup surface        Surface Management

  A surface is a memory buffer containing pixel data.
 */

/**@defgroup disp_vbi       VBI and Polarity */

/**@defgroup disp_pd        Port Drivers */

/**@defgroup debug          Debug helpers

  The majority of the functions in this group dump the contents of data
  structures to stdout or return string representations (that can be used in
  messages) for the values of various GDL enumerations.
 */

#ifndef DOXYGEN_SKIP
#define GDL_API __attribute__ ((visibility ("default")))
#else
#define GDL_API " "
#endif

GDL_API gdl_ret_t
gdl_init(
    gdl_void * reserved0
    );
    /**< @ingroup general

    This routine initializes the session and initiates communication with
    the display driver server daemon.  Calls to any other GDL functions will
    fail until this call is made. It must be called once per process. All
    threads in a process are then able to use the GDL API.

    Subsequent calls to gdl_init() (without an intervening call to gdl_close())
    will return #GDL_SUCCESS and increment an internal reference count.

    @param [in] reserved0
    This parameter is reserved for future use and should be set to 0.

    @note   This routine does not restore plane or port driver defaults
            that may have been changed by a previously running GDL
            application.  Plane defaults can be restored by using
            @ref gdl_plane_reset() and @ref gdl_plane_reset_all().  No
            restoration functionality exists for port drivers.
    */

#ifndef DOXYGEN_SKIP /* DOXYGEN_SKIP only defined when docs are generated */
GDL_API gdl_ret_t _gdl_init(gdl_uint32 version);
#define gdl_init(reserved0) _gdl_init((GDL_HEADER_VERSION_MAJOR << 16) \
                                      | GDL_HEADER_VERSION_MINOR)
#endif

GDL_API gdl_ret_t
gdl_close(
    void
    );
    /**< @ingroup general

    This function decrements the internal reference count that gdl_open()
    increments.  Once the reference count goes to zero, communication with the
    display driver server daemon is closed and the session is terminated.
    Subsequent calls to the GDL API from within the same process will fail until
    gdl_init() is called again.

    The use of a reference account allows pairs of gdl_open()/gdl_close() calls
    to be used, for instance, in separate threads in the same process without
    interference.

    When the session is terminated, any unfreed surfaces that were allocated by
    the calling process are freed, and warnings are printed to the console. If
    a surface freed in this manner is currently being displayed, the plane it is
    displayed on is first disabled.  No other hardware settings are changed;
    applications launched subsequently will inherit the hardware configuration
    left behind by the closing process.
    */

GDL_API gdl_ret_t
gdl_get_driver_info(
    gdl_driver_info_t * driver_info
    );
    /**< @ingroup general

    This routine returns driver information to the caller.  This information
    can be used to verify the version of the gdl driver.

    @param [out] driver_info
    This parameter is of type #gdl_driver_info_t.  This info contains the
    driver version as well as other info.
    */

GDL_API gdl_ret_t
gdl_closed_caption_source(
    gdl_plane_id_t plane_id
    );
    /**< @ingroup general

    This function allows specification of the video stream to be used as the
    source for closed caption data.

    The vbd_flip() function (of the VBD interface) allows the caller to flip a
    field/frame from a video stream onto a specified Universal Pixel Plane.
    Closed caption data associated with the frame is passed along in the same
    call.

    However, it is possible for multiple video streams to be displayed at the
    same time, on different planes.  The internal TV encoder(s) can only process
    one CC stream at a time -- intermixing streams will result in garbage being
    displayed for CC text.

    The plane ID passed to this function determines which CC stream will be
    passed along to the internal TV encoder(s).  CC data passed for streams
    flipped onto any other planes will be discarded.

    By default, the CC source is GDL_PLANE_ID_UNDEFINED, which causes *all* CC
    data passed to vbd_flip() to be ignored.

    @param [in]  plane_id
    The ID of the Universal Pixel Plane to which the currently selected CC
    stream will be directed.  If the value is GDL_PLANE_ID_UNDEFINED, all CC
    data passed via vbd_flip() calls will be ignored.
    */

GDL_API gdl_ret_t
gdl_get_display_info(
    gdl_display_id_t     display_id,
    gdl_display_info_t * display_info
    );
    /**< @ingroup disp_mode

    This routine returns the current display settings including resolution,
    refresh rate, and so on.

    @param [in] display_id
    The ID of the display whose settings should be retrieved.

    @param [out] display_info
    A pointer to a #gdl_display_info_t structure that will receive the current
    display settings.
    */

GDL_API gdl_ret_t
gdl_set_display_info(
    gdl_display_info_t * display_info
    );
    /**< @ingroup disp_mode

    This routine sets the display to the configuration specified by the
    display_info parameter.

    @param [in] display_info
    The desired display configuration.  The 'id' field of the structure
    indicates the display that is to be configured.
    */

GDL_API gdl_ret_t
gdl_check_tvmode(
    gdl_display_id_t id,
    gdl_tvmode_t *   mode
    );
    /**< @ingroup disp_mode

    Returns #GDL_SUCCESS if and only if the specified display mode is supported
    on all of the port drivers currently active on the specified display.

    @param [in] id
    Display of interest.

    @param [in] mode
    Mode of interest.
    */

GDL_API gdl_ret_t
gdl_get_port_tvmode_by_index(
    gdl_pd_id_t    id,
    gdl_uint32     index,
    gdl_tvmode_t * mode
    );
    /**< @ingroup disp_mode

    This routine can be used to enumerate the display modes supported by a
    given port driver.  The supported modes are indexed by 0-based consecutive
    integer values.  Calling this function with index==0 will return
    information about the first supported mode; calling it with index==1
    will return information about the second supported mode; and so on.

    The function returns GDL_SUCCESS as long as 'index' < N, where N is the
    number of modes supported by the port driver.

    @param [in] id
    Port driver of interest.

    @param [in] index
    Mode index.

    @param [out] mode
    Display mode information is returned here if the return value is
    GDL_SUCCESS; otherwise, the contents of this structure are meaningless.
    */

GDL_API gdl_ret_t
gdl_wait_for_vblank(
    gdl_polarity_t * polarity
    );
    /**< @ingroup disp_vbi

    This routine will block until the next Vertical Blanking Interval (VBI) has
    begun, as indicated by the VBlank signal.  Return value other than
    GDL_SUCCESS indicates that no VBI occurred during the expected time
    interval. This may indicate that PIPE A is not enabled.
    @b NOTE: The latency between VBI and return from this call can be affected
    by a system load. 

    @param [out] polarity
    If this argument is NULL it is ignored.  Otherwise, the polarity of the new
    active region is returned, as a value of type #gdl_polarity_t
    */

GDL_API gdl_ret_t
gdl_get_display_polarity(
    gdl_polarity_t * polarity
    );
    /**< @ingroup disp_vbi

    This routine will return immediately the current display polarity.

    @param [out] polarity
    The polarity is returned here.  See #gdl_polarity_t.
    */

GDL_API gdl_ret_t
gdl_alloc_surface(
    gdl_pixel_format_t   pixel_format,
    gdl_uint32           width,
    gdl_uint32           height,
    gdl_uint32           flags,
    gdl_surface_info_t * surface_info
    );
    /**< @ingroup surface

    This routine allocates a surface with the specified characteristics.  The
    driver will determine the required alignment and validate the requested
    surface attributes, and may alter them according to the device status.

    @param [in] pixel_format
    The pixel format of the surface.

    @param [in] width
    The width of the surface in pixels.

    @param [in] height
    The height of the surface in pixels.

    @param [in] flags
    Any number of flags of type #gdl_surface_flag_t OR'd together that
    describe the type of surface and the intended usage.

    @param [out] surface_info
    This parameter is returned to the caller and contains information about the
    surface that was just created.

    @b NOTE
    Exiting the application or terminating the GDL session via a call to
    gdl_close() without freeing the surface will result in its automatic
    deallocation. A warning message will be printed to the system console and
    log.
    */

GDL_API gdl_ret_t
gdl_get_surface_info(
    gdl_surface_id_t     surface_id,
    gdl_surface_info_t * surface_info
    );
    /**< @ingroup surface

    This routine returns the gdl_surface_info_t data structure for the requested
    surface.

    @param [in] surface_id
    The ID of the surface for which information is requested.

    @param [out] surface_info
    A pointer to a #gdl_surface_info_t structure to be filled with information
    about the specified surface.
    */

GDL_API gdl_ret_t
gdl_flush_surface(
    gdl_surface_id_t surface_id
    );
    /**< @ingroup surface

    This routine flushes cached memory used by the surface. Flushing only has any
    effect when surface is allocated with #GDL_SURFACE_CACHED flag.

    @param [in] surface_id
    The ID of the surface to be flushed.
    */

GDL_API gdl_ret_t
gdl_map_surface(
    gdl_surface_id_t surface_id,
    gdl_uint8 **     pointer,
    gdl_uint32 *     pitch
    );
    /**< @ingroup surface

    This routine provides a direct pointer to the surface's buffer mapped into
    the caller's address space. Synchronization of any graphics operations
    pending on that surface must be handled by the caller before using the
    pointer.

    A surface may be mapped by more than one thread.

    The surface must be unmapped prior to freeing it; an attempt to free a
    mapped surface will fail.

    @param [in] surface_id
    The ID of the surface that to be mapped.

    @param [out] pointer
    CPU accessible address of the surface.

    @param [out] pitch
    The pitch of the mapped surface in bytes.  This is an optional output
    parameter and will be ignored if it is NULL.  In the case of a YUV
    planar or pseudo-planar surface, this is the pitch of the Y surface.
    */

GDL_API gdl_ret_t
gdl_unmap_surface(
    gdl_surface_id_t surface_id
    );
    /**< @ingroup surface

    This routine unmaps the surface's buffer from the callers address space.
    All surfaces should be unmapped when the pointer is no longer needed.
    An attempt to free a mapped surface will fail.

    @param [in] surface_id
    The ID of the surface that is to be unmapped.
    */

GDL_API gdl_ret_t
gdl_free_surface(
    gdl_surface_id_t surface_id
    );
    /**< @ingroup surface

    This routine destroys the memory associated with the specified surface.

    @param [in] surface_id
    The ID of the surface that to be destroyed.

    @b NOTE
    - Freeing a surface that is currently being displayed on a plane, will cause
      the plane to be disabled. A warning will be printed to the system console
      and log.

    - Attempting to free a surface that is currently mapped by anyone will
      result in a #GDL_ERR_MAPPED error.

    - Passing an invalid surface ID or the ID GDL_SURFACE_INVALID will result in
      an error return.
    */

GDL_API gdl_ret_t
gdl_get_palette(
    gdl_surface_id_t surface_id,
    gdl_palette_t *  palette
    );
    /**< @ingroup surface

    This routine returns the palette entries for the specified surface.

     @param [in] surface_id
     The ID of the surface whose palette entries are to be returned.

     @param [in,out] palette
     The length field of the palette structure contains the number of palette
     entries requested by the user. Upon completion of this call it may be
     updated with the actual number of palette entries (if less than the
     specified length) or with zero(if there is no palette associated with the
     surface). The data field will be filled with palette entries.
     */

GDL_API gdl_ret_t
gdl_set_palette(
    gdl_surface_id_t surface_id,
    gdl_palette_t *  palette
    );
    /**< @ingroup surface

    This routine assigns the specified palette to the specified surface.
    An error will be returned if the pixel format of the surface is not a
    palettized format.

    Note that the palette info is copied into the driver: changing entries in
    a palette has no effect until the next time gdl_set_palette() is called.

    Also note that changing the palette of a surface that is currently flipped
    onto a plane does <b>not</b> change the displayed colors: the surface
    must be flipped onto the plane again after the palette is set.

    @param [in] surface_id
    The ID of the surface whose palette entries will be set.

    @param [in] palette
    A pointer to a palette structure containing color data as well as number
    of active palette elements
    */

GDL_API gdl_ret_t
gdl_get(
    gdl_surface_id_t  surface_id,
    gdl_rectangle_t * rect,
    gdl_uint32        pitch,
    gdl_void *        data,
    gdl_uint32        flags
    );
    /**< @ingroup surface

    This routine copies a rectangular region of pixel data out of a surface to
    a buffer.

    @param [in] surface_id
    The ID of the surface from which the pixel data is read. This function is
    currently only implemented for the following pixel formats, and will return
    an error code if the surface has any other pixel format:
    - GDL_PF_ARGB_32
    - GDL_PF_RGB_32
    - GDL_PF_RGB_24
    - GDL_PF_ARGB_16_4444
    - GDL_PF_RGB_16
    - GDL_PF_RGB_15

    @param [in] rect
    The rectangle within the surface from which the data is to be read.
    The origin is relative to the origin of the surface.  Both the origin
    coordinates and the dimensions of the rectangle are specified in pixels.
    A NULL pointer indicates that the rectangle is the entire surface.

    @param [in] pitch
    The pitch of the output data buffer. It must be greater than or equal to
    @code
    rect->width * <bytes-per-pixel>
    @endcode

    @param [out] data
    The address of the buffer to which the pixel data should be copied.
    The size of the buffer must be greater than or equal to
    @code
    rect->height * pitch
    @endcode

    @param [in] flags
    None currently defined -- pass as 0.
    */

GDL_API gdl_ret_t
gdl_put(
    gdl_surface_id_t  surface_id,
    gdl_rectangle_t * rect,
    gdl_uint32        pitch,
    gdl_void*         data,
    gdl_uint32        flags
    );
    /**< @ingroup surface

    This routine copies a rectangular region of pixel data into a surface from
    a buffer.

    @param [in] surface_id
    The ID of the surface to which the pixel data will be written.  This
    function is currently only implemented for the following pixel formats, and
    will return an error code if the surface has any other pixel format:
    - GDL_PF_ARGB_32
    - GDL_PF_RGB_32
    - GDL_PF_RGB_24
    - GDL_PF_ARGB_16_4444
    - GDL_PF_RGB_16
    - GDL_PF_RGB_15

    @param [in] rect
    The rectangle within the surface to which the data is to be written.
    The origin is relative to the origin of the surface.  Both the origin
    coordinates and the dimensions of the rectangle are specified in pixels.
    A NULL pointer indicates that the rectangle is the entire surface.

    @param [in] pitch
    The pitch of the input data buffer. It must be greater than or equal to
    @code
    rect->width * <bytes-per-pixel>
    @endcode

    @param [in] data
    The address of the buffer from which the pixel data should be copied.
    The pixel format of the data is assumed to be that of the surface.
    The size of the buffer must be greater than or equal to
    @code
    rect->height * pitch
    @endcode

    @param [in] flags
    None currently defined -- pass as 0.
    */

GDL_API gdl_ret_t
gdl_clear_surface(
    gdl_surface_id_t surface_id,
    gdl_color_t *    color
    );
    /**< @ingroup surface

    This routine clears a surface by setting all of its pixels to the specified
    color.

    @param [in] surface_id
    The ID of the surface to be cleared.  Only packed pixel formats are
    supported by this function;  if the surface has a pseudo-planar video pixel
    format, the call will fail.

    @param [in] color
    The input color for the operation. Note that:
    - The pixel format of the surface will determine which fields and bits
      of the gdl_color_t item are used.  For example, if the pixel format is
      GDL_PF_RGB_16, then:
      - none of the alpha_index field will be used.
      - the low-order 5 bits of the r_y field will be used.
      - the low-order 6 bits of the g_u field will be used.
      - the low-order 5 bits of the b-v field will be used.
      .
    - If the surface is supposed to contain pixels with premultiplied alpha
      it is the programmer's responsibility to pass a color whose components
      have been premultiplied.
    */

GDL_API gdl_ret_t
gdl_plane_capabilities(
    gdl_plane_id_t     plane_id,
    gdl_plane_info_t * info
    );
    /**< @ingroup plane_management

    This function returns information about the hardware capabilities of the
    specified display plane.

    The returned data structure will report the plane name, the
    programmer-configurable attributes, and the pixel formats supported by the
    plane.  See #gdl_plane_info_t to determine how to interpret the returned
    information.

    @param [in]  plane_id
    The ID of the plane for which information should be returned.

    @param [out] info
    Plane information is returned here.
    */

GDL_API gdl_ret_t
gdl_plane_config_begin(
    gdl_plane_id_t plane_id
    );
    /**< @ingroup plane_management

    This function begins a configuration transaction for the specified plane.

    After making this call, the application program can call the gdl_plane_set_*
    functions to change the attributes of interest.  No new attribute values
    are actually set in the hardware until a call is made to
    gdl_plane_config_end().

    The "batching" of attribute settings reduces the number of round trips
    made to the driver and helps avoid invalid intermediate states when
    interrelated attributes are being changed.

    Only one transaction can be open at a time.  If a corresponding call to
    gdl_plane_config_end() is not made before the next call to
    gdl_plane_config_begin(), an error will be returned.

    @param [in]  plane_id
        The ID of the plane for which a configuration transaction should be
        begun.

    @see gdl_plane_config_end(), gdl_plane_set_uint(), gdl_plane_set_int(),
         gdl_plane_set_rect(), gdl_plane_set_attr(),
    */


GDL_API gdl_ret_t
gdl_plane_config_end(
    gdl_boolean_t abort
    );
    /**< @ingroup plane_management

    This function delimits the end of a configuration transaction.  All
    attributes specified via gdl_plane_set_* calls since the last call to
    gdl_plane_config_begin() are validated and applied.

    The 'abort' argument causes all pending configuration settings to be
    discarded without being applied.

    It is an error to call this function without a preceding call to
    gdl_plane_config_begin().  If this function is called with abort=#GDL_TRUE,
    the return value can be safely ignored and the caller is guaranteed that
    there are no configuration operations pending or in progress for the calling
    application.

    @param [in] abort
        If #GDL_FALSE, all plane attributes set since the last call to
        gdl_plane_config_begin() are validated and applied.  Otherwise, all
        pending settings are discarded and the transaction is terminated
        without any change to the hardware.

    @see gdl_plane_config_begin()
    */

GDL_API gdl_ret_t
gdl_plane_set_int(
    gdl_plane_attr_t name,
    gdl_int32        value
    );
    /**< @ingroup plane_management
         See gdl_plane_set_attr()
    */

GDL_API gdl_ret_t
gdl_plane_set_uint(
    gdl_plane_attr_t name,
    gdl_uint32       value
    );
    /**< @ingroup plane_management
         See gdl_plane_set_attr()
    */

GDL_API gdl_ret_t
gdl_plane_set_rect(
    gdl_plane_attr_t  name,
    gdl_rectangle_t * value
    );
    /**< @ingroup plane_management
         See gdl_plane_set_attr()
    */


GDL_API gdl_ret_t
gdl_plane_set_attr(
    gdl_plane_attr_t name,
    void *           value
    );
    /**< @ingroup plane_management
    These functions queue the specified attribute setting for execution in the
    currently open configuration transaction.  They vary from each other only
    in the type of value they accept, which must be appropriate for the
    specified attribute.

    It is an error to call any of these functions with the "name" of an
    attribute that has a type different from that of the function.

    It is an error to call any of these functions without a preceding call to
    gdl_plane_config_begin().

    The plane is not actually reconfigured until the next call to
    gdl_plane_config_end().  It is possible that an error caused by passing
    erroneous or conflicting values to the "set" functions will not be
    detected until then.

    Note that data values passed to these routines are cached -- the
    application does @b not need to preserve the data or pointers until the
    gdl_plane_config_end() is called.

    @param [in] name
        The attribute to be changed.

    @param [in] value
        The new setting (or, for multi-word values, pointer to the new setting)
        for the attribute.

    @see gdl_plane_config_begin(),  gdl_plane_config_end()
    */

GDL_API gdl_ret_t
gdl_plane_get_int(
    gdl_plane_id_t   plane,
    gdl_plane_attr_t name,
    gdl_int32 *      value
    );
    /**< @ingroup plane_management
     See gdl_plane_get_attr()
    */

GDL_API gdl_ret_t
gdl_plane_get_uint(
    gdl_plane_id_t   plane,
    gdl_plane_attr_t name,
    gdl_uint32 *     value
    );
    /**< @ingroup plane_management
      See gdl_plane_get_attr()
    */

GDL_API gdl_ret_t
gdl_plane_get_rect(
    gdl_plane_id_t    plane,
    gdl_plane_attr_t  name,
    gdl_rectangle_t * value
    );
    /**< @ingroup plane_management
      See gdl_plane_get_attr()
    */

GDL_API gdl_ret_t
gdl_plane_get_attr(
    gdl_plane_id_t   plane,
    gdl_plane_attr_t name,
    void *           value
    );
    /**< @ingroup plane_management
    These functions retrieve the current value of the specified attribute
    for the specified plane.  These functions vary from each other only in the
    type of value they return.

    It is an error to call any of these functions with the "name" of an
    attribute that has a type different from that of the function.

    @param [in] plane
        The plane to be queried.

    @param [in] name
        The attribute whose value is to be retrieved.

    @param [out] value
        The current value of the attribute is returned here.
    */

GDL_API gdl_ret_t
gdl_plane_reset(
    gdl_plane_id_t plane
    );
    /**< @ingroup plane_management
    Disables the specified plane and resets its attributes to their defaults.

    @param [in] plane
        The plane to be reset.
    */

GDL_API gdl_ret_t
gdl_plane_reset_all(
    void
    );
    /**< @ingroup plane_management
    Disables all planes and reset their attributes to their defaults.
    */

GDL_API gdl_ret_t
gdl_flip(
    gdl_plane_id_t   plane_id,
    gdl_surface_id_t surface_id,
    gdl_flip_t       sync
    );
    /**< @ingroup plane_management

    This function makes a specified surface visible on the specified plane.
    The pixel format and dimensions of the surface must match the current
    configuration of the plane.

    @param [in] plane_id
    The plane onto which the surface should be flipped.

    @param [in] surface_id
    The ID of the surface to be flipped onto the plane, normally returned by
    gdl_alloc_surface().  If the surface ID is #GDL_SURFACE_INVALID, the plane
    will be disabled; otherwise, the plane is enabled if necessary.

    @param [in] sync
    Determines how flipping of the surface is synchronized with the caller.
    See the description of #gdl_flip_t for details.

    @b NOTE
    - If the client that allocated a displayed surface exits,
      deallocates the surface or calls #gdl_close, then the plane on which
      the surface was flipped will automatically be disabled.
    */


GDL_API gdl_ret_t
gdl_port_pd_load(
    char *     pd_name,
    gdl_uint32 args_num,
    char **    args
    );
    /**< @ingroup  disp_pd

    Load a port driver and optionally pass it arguments.

    @param [in] pd_name
    Name of port driver (specified as path to the port driver .so file).

    @param [in] args_num
    Number of supplied arguments.

    @param [in] args
    Array of arguments, where each argument is a string of the form
    "<attr>=<value>".
    */

GDL_API gdl_ret_t
gdl_port_pd_unload(
    char *pd_name
    );

    /**< @ingroup  disp_pd

    Unload a port driver.

    @param [in] pd_name
    Name of the port driver (specified as the name of its .so file, but
    without the .so extension).
    */

GDL_API gdl_ret_t
gdl_port_set_attr(
    gdl_pd_id_t           pd_id,
    gdl_pd_attribute_id_t attribute_id,
    void *                attribute_value
    );
    /**< @ingroup disp_pd

    Set the current value of a port driver attribute.

    @param [in] pd_id
    Port driver ID.

    @param [in] attribute_id
    Attribute ID.

    @param [in] attribute_value
    Pointer to value to which attribute should be set.
    */

GDL_API gdl_ret_t
gdl_port_get_attr(
    gdl_pd_id_t           pd_id,
    gdl_pd_attribute_id_t attribute_id,
    gdl_pd_attribute_t *  attribute
    );
    /**< @ingroup disp_pd

    Retrieve the current value of a port driver attribute.

    @param [in] pd_id
    Port driver ID.

    @param [in] attribute_id
    Attribute ID.

    @param [out] attribute
    Attribute value returned here.
    */

GDL_API gdl_ret_t
gdl_port_send(
    gdl_pd_id_t   pd_id,
    gdl_pd_send_t data_id,
    void *        data,
    unsigned int  data_size
    );
    /**< @ingroup disp_pd

    Send port-specific data to a port driver. This function allows the sending
    of port-specific data structures, which in turn may kick off device-specific
    operations.

    @param [in] pd_id
    Port driver ID.

    @param [in] data_id
    An identifier indicating the kind of data structure being sent.  Based
    on this ID, the port driver will determine the format of the data and the
    operation to be performed.  See the description of #gdl_pd_send_t for more
    details.

    @param [in] data
    Pointer to the beginning of the data structure to be sent.

    @param [in] data_size
    Size of the data structure in bytes.
    */

GDL_API gdl_ret_t
gdl_port_recv(
    gdl_pd_id_t   pd_id,
    gdl_pd_recv_t data_id,
    void *        data,
    unsigned int  data_size
    );
    /**< @ingroup disp_pd

    Retrieve port-specific data from a port driver.

    @param [in] pd_id
    Port driver ID.

    @param [in] data_id
    An identifier indicating the kind of data structure to be returned.  Based
    on this ID, the port driver will determine how to retrieve the data and
    how to format it for output.  See the description of #gdl_pd_recv_t for
    more details.

    @param [out] data
    The data structure is returned here.

    @param [in] data_size
    Size, in bytes, of the output buffer pointed at by 'data'.
    */

GDL_API char *
gdl_get_error_string(
    gdl_ret_t gdl_ret
    );
    /**< @ingroup debug

    This function returns a character string representation of the passed
    enumerator.

    @param [in] gdl_ret
    A GDL return value.
    */

GDL_API void
gdl_dbg_dump_display_info(
    gdl_display_info_t * d
    );
    /**< @ingroup debug

    This function prints the content of the specified #gdl_display_info_t to
    stdout.

    @param [in] d
    The structure whose contents are to be printed
    */

GDL_API void
gdl_dbg_dump_surface_info(
    gdl_surface_info_t * s
    );
    /**< @ingroup debug

    This function prints the content of the specified #gdl_surface_info_t to
    stdout.

    @param [in] s
    The structure whose contents are to be printed
    */

GDL_API char *
gdl_dbg_string_attribute(
    gdl_plane_attr_t attr
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of a
    plane attribute ID.

    @param [in] attr
    The ID for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_pixel_format(
    gdl_pixel_format_t pf
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of a
    pixel format ID.

    @param [in] pf
    The ID for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_plane_id(
    gdl_plane_id_t pid
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of a
    plane ID.

    @param [in] pid
    The plane ID for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_refresh(
    gdl_refresh_t r
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of a
    display refresh rate enumerator.

    @param [in] r
    The enumerator for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_sampling_rate(
    gdl_hdmi_audio_fs_t fs
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of an
    HDMI audio sampling rate enumerator.

    @param [in] fs
    The enumerator for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_sample_size(
    gdl_hdmi_audio_ss_t ss
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of an
    HDMI audio sample size enumerator.

    @param [in] ss
    The enumerator for which the string should be returned.
    */


GDL_API char *
gdl_dbg_string_audio_format(
    gdl_hdmi_audio_fmt_t fmt
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of an
    HDMI audio format enumerator.

    @param [in] fmt
    The enumerator for which the string should be returned.
    */

GDL_API char *
gdl_dbg_string_speaker_map(
    gdl_hdmi_audio_speaker_map_t map
    );
    /**< @ingroup debug

    This function returns a pointer to a 0-terminated string representation of an
    HDMI speaker allocation enumerator.

    @param [in] map
    The enumerator for which the string should be returned.
    */

GDL_API gdl_ret_t
gdl_event_register(
    gdl_app_event_t      event,
    gdl_event_callback_t callback,
    void *               user_data
    );
    /**< @ingroup general
    This function registers a callback function for the specified event, 
    causing the function to be called every time the event occurs.

    Only one callback may be registered for a particular event.
    however, the same callback function can be registered for multiple events.
    The event which triggered the callback will be passed to the function
    when it is evoked.

    The callback function will be called asynchronously and should take the
    necessary precautions to prevent contention for global data structures.
    Callback functions do not have to be re-entrant: once any callback has
    been invoked for an event, all other events are put on hold until the
    callback returns. This could require the function to register the event
    with the application for synchronous processing and return to the caller
    ASAP.
    
    Every time a callback function is called it will be passed 'user_data'.
    
    @param [in] event
    Event of interest.

    @param [in] callback
    Callback function

    @param [in] user_data
    User data that will be passed into a callback function.
    */

GDL_API gdl_ret_t
gdl_event_unregister(
    gdl_app_event_t    event
    );
    /**< @ingroup general
    This function unregisters the callback from being called for a specified event.
    Since the same callback function can be registered for more than one event, 
    other events will continue triggering an execution of the callback function.
    
    @param [in] event
    Event of interest
    */

GDL_API gdl_ret_t
gdl_debug_log_start(
    char *       filename,
    unsigned int flags
    );
    /**< @ingroup debug
    This function begins logging of function calls within the GDL daemon.
    This type of logging is primarily intended for use by GDL driver developers
    in trouble-shooting problems.
    
    @param [in] filename
    The pathname of the file to which the log information will be written.  If
    a file with the same pathname already exists, it will be deleted before
    logging begin.

    @param [in] flags
    The type of information that should be logged, passed as the OR-ed
    combination members of the #gdl_log_flag_t enumeration.
    */

GDL_API gdl_ret_t
gdl_debug_log_stop(
    void
    );
    /**< @ingroup debug
    This function terminates driver-side logging previously started with
    gdl_debug_log_start().
    */


GDL_API gdl_ret_t
gdl_create_surface(
    gdl_surface_info_t * surface_info
    );
    /**< @ingroup surface
    This function allows a surface to be created from memory allocated outside of
    the display driver.
    
    The memory must be @b physically contiguous, and must remain available until
    after the surface is freed.
 
    @param [in,out] surface_info
    The surface information used to create the GDL surface.  The @b physical
    address of the start of the memory must be passed in the
    'surface_info.phys_addr' field.  @b All other fields must be correctly
    filled before the structure is passed to this function, with the
    exception of the following:
    - id:  will be filled in with a value surface ID upon successful return
      from this function.
    - heap_phys_addr: will be ignored and overwritten by this function.
    */


GDL_API gdl_ret_t
gdl_attach_heap(
    const char * heap_name
    );
    /**< @ingroup surface
    This function makes the specified heap available for allocation of surfaces
    in the calling process via #gdl_alloc_surface().
    
    heap_name must appear in the platform configuration file under the node 
    'platform.startup.memory.layout'. The memory whose physical
    base address and size are specified there will be made available to the
    calling process.

    Once a heap is attached to a process, the display driver may satisfy surface
    allocation requests from either its default (static) heap or the attached
    one.

    Multiple processes may attach the same heap. A process may attach multiple
    heaps. An error will be returned if a process tries to attach to two
    differnt heaps that overlap.

    There is no need to de-attach a heap; it is sufficient to free all surfaces
    allocated by the process. The display driver will automatically free any
    remaining surfaces allocated by a proces when #gdl_close() is called.

    An attached heap is available to a process from the moment it is attached
    until the GDL session for the process is terminated.

    This function allows device memory (physical memory beyond the end of the
    operating system) normally reserved for video playback to be reallocated
    at runtime for use by graphics-intensive operations. It is expected that
    the controlling application will terminate video playback to make the 
    memory available before starting a graphics-intensive application that
    will attach the heap. The controlling application itself should not
    attach the memory as there will be no way to relinquish it when the 
    graphics application stops and it is time to play video again
    
    @param [in] heap_name
    The name of the heap as it appears in the platform configuration file under
    the 'platform.startup.memory.layout' node.
    */


GDL_API gdl_ret_t
gdl_heap_in_use(
    const char    * heap_name,
    gdl_boolean_t * status
    );
    /**< @ingroup surface
    This function determines whether any surfaces are currently allocated from 
    the specified heap.

    Returns status GDL_FALSE if no surfaces are currently allocated from the
    specified heap or GDL_TRUE otherwise. Fails if the heap name does not
    appear in the platform configuration file.

    @param [in] heap_name
    A name of the heap as it appears in platform configuration file under the
    'platform.startup.memory.layout'. 

    @param [out] status
    Returned status.
    */


#if defined(__cplusplus)
};
#endif

#endif
