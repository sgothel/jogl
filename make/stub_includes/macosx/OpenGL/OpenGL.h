/* Typedefs, enums and function prototypes extracted from Apple's
   OpenGL.h to expose portions of the low-level CGL API to Java */

/* Typedefs to get things working */
typedef struct _cglObj* CGLContextObj;
typedef struct _cglObj* CGLShareGroupObj;
typedef struct _cglObj* CGLPBufferObj;
typedef struct _cglObj* CGLPixelFormatObj;

/*
** Attribute names for CGLChoosePixelFormat and CGLDescribePixelFormat.
*/
typedef enum _CGLPixelFormatAttribute {
	kCGLPFAAllRenderers       =   1,	/* choose from all available renderers          */
	kCGLPFADoubleBuffer       =   5,	/* choose a double buffered pixel format        */
	kCGLPFAStereo             =   6,	/* stereo buffering supported                   */
	kCGLPFAAuxBuffers         =   7,	/* number of aux buffers                        */
	kCGLPFAColorSize          =   8,	/* number of color buffer bits                  */
	kCGLPFAAlphaSize          =  11,	/* number of alpha component bits               */
	kCGLPFADepthSize          =  12,	/* number of depth buffer bits                  */
	kCGLPFAStencilSize        =  13,	/* number of stencil buffer bits                */
	kCGLPFAAccumSize          =  14,	/* number of accum buffer bits                  */
	kCGLPFAMinimumPolicy      =  51,	/* never choose smaller buffers than requested  */
	kCGLPFAMaximumPolicy      =  52,	/* choose largest buffers of type requested     */
	kCGLPFAOffScreen          =  53,	/* choose an off-screen capable renderer        */
	kCGLPFAFullScreen         =  54,	/* choose a full-screen capable renderer        */
	kCGLPFASampleBuffers      =  55,	/* number of multi sample buffers               */
	kCGLPFASamples            =  56,	/* number of samples per multi sample buffer    */
	kCGLPFAAuxDepthStencil    =  57,	/* each aux buffer has its own depth stencil    */
	kCGLPFAColorFloat         =  58,	/* color buffers store floating point pixels    */
	kCGLPFAMultisample        =  59,    /* choose multisampling                         */
	kCGLPFASupersample        =  60,    /* choose supersampling                         */
	kCGLPFASampleAlpha        =  61,    /* request alpha filtering                      */

	kCGLPFARendererID         =  70,	/* request renderer by ID                       */
	kCGLPFASingleRenderer     =  71,	/* choose a single renderer for all screens     */
	kCGLPFANoRecovery         =  72,	/* disable all failure recovery systems         */
	kCGLPFAAccelerated        =  73,	/* choose a hardware accelerated renderer       */
	kCGLPFAClosestPolicy      =  74,	/* choose the closest color buffer to request   */
	kCGLPFARobust             =  75,	/* renderer does not need failure recovery      */
	kCGLPFABackingStore       =  76,	/* back buffer contents are valid after swap    */
	kCGLPFAMPSafe             =  78,	/* renderer is multi-processor safe             */
	kCGLPFAWindow             =  80,	/* can be used to render to an onscreen window  */
	kCGLPFAMultiScreen        =  81,	/* single window can span multiple screens      */
	kCGLPFACompliant          =  83,	/* renderer is opengl compliant                 */
	kCGLPFADisplayMask        =  84,	/* mask limiting supported displays             */
	kCGLPFAPBuffer            =  90,	/* can be used to render to a pbuffer           */
        kCGLPFARemotePBuffer	  =  91,    /* can be used to render offline to a pbuffer	*/
	kCGLPFAVirtualScreenCount = 128 	/* number of virtual screens in this format     */
} CGLPixelFormatAttribute;

/*
** Error return values from CGLGetError.
*/
typedef enum _CGLError {
	kCGLNoError            = 0,     /* no error */
	kCGLBadAttribute       = 10000,	/* invalid pixel format attribute  */
	kCGLBadProperty        = 10001,	/* invalid renderer property       */
	kCGLBadPixelFormat     = 10002,	/* invalid pixel format            */
	kCGLBadRendererInfo    = 10003,	/* invalid renderer info           */
	kCGLBadContext         = 10004,	/* invalid context                 */
	kCGLBadDrawable        = 10005,	/* invalid drawable                */
	kCGLBadDisplay         = 10006,	/* invalid graphics device         */
	kCGLBadState           = 10007,	/* invalid context state           */
	kCGLBadValue           = 10008,	/* invalid numerical value         */
	kCGLBadMatch           = 10009,	/* invalid share context           */
	kCGLBadEnumeration     = 10010,	/* invalid enumerant               */
	kCGLBadOffScreen       = 10011,	/* invalid offscreen drawable      */
	kCGLBadFullScreen      = 10012,	/* invalid offscreen drawable      */
	kCGLBadWindow          = 10013,	/* invalid window                  */
	kCGLBadAddress         = 10014,	/* invalid pointer                 */
	kCGLBadCodeModule      = 10015,	/* invalid code module             */
	kCGLBadAlloc           = 10016,	/* invalid memory allocation       */
	kCGLBadConnection      = 10017 	/* invalid CoreGraphics connection */
} CGLError;

typedef enum _CGLContextParameter {
   kCGLCPSwapRectangle    = 200,
   kCGLCPSwapInterval     = 222,
   kCGLCPDispatchTableSize = 224,
   kCGLCPClientStorage    = 226,
   kCGLCPSurfaceTexture    = 228,
   kCGLCPSurfaceOrder      = 235,
   kCGLCPSurfaceOpacity    = 236,
   kCGLCPSurfaceBackingSize = 304,
   kCGLCPSurfaceSurfaceVolatile = 306,
   kCGLCPReclaimResources  = 308,
   kCGLCPCurrentRendererID  = 309,
   kCGLCPGPUVertexProcessing  = 310,
   kCGLCPGPUFragmentProcessing  = 311,
   kCGLCPHasDrawable             = 314,
   kCGLCPMPSwapsInFlight         = 315,
} CGLContextParameter;

/* Pixel format manipulation */
CGLError CGLChoosePixelFormat(const CGLPixelFormatAttribute *attribs,
                              CGLPixelFormatObj *pix,
                              long *npix);
CGLError CGLDestroyPixelFormat(CGLPixelFormatObj pix);
CGLPixelFormatObj CGLGetPixelFormat ( CGLContextObj ctx );

/* Context manipulation */
CGLError CGLCreateContext(CGLPixelFormatObj pix,
                          CGLContextObj share,
                          CGLContextObj* ctx);
void CGLReleaseContext(CGLContextObj ctx);
CGLError CGLDestroyContext(CGLContextObj ctx);
CGLError CGLSetCurrentContext(CGLContextObj ctx);
CGLContextObj CGLGetCurrentContext (void);
CGLError CGLFlushDrawable ( CGLContextObj ctx);
CGLError CGLSetParameter ( CGLContextObj ctx, CGLContextParameter pname, const int *params );
CGLError CGLCopyContext ( CGLContextObj src, CGLContextObj dst, int mask );

CGLShareGroupObj CGLGetShareGroup(CGLContextObj ctx);

/* PBuffer manipulation */
CGLError CGLCreatePBuffer(long width,
                          long height,
                          unsigned long target,
                          unsigned long internalFormat,
                          long max_level,
                          CGLPBufferObj* pbuffer);
CGLError CGLDestroyPBuffer(CGLPBufferObj pbuffer);
CGLError CGLSetPBuffer(CGLContextObj ctx,
                       CGLPBufferObj pbuffer,
                       unsigned long face,
                       long level,
                       long screen);
