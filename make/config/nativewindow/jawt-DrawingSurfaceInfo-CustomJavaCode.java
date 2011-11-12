public JAWT_PlatformInfo platformInfo(final JAWT jawt) {
  return newPlatformInfo(jawt, platformInfo0(getBuffer()));
}

private native ByteBuffer platformInfo0(Buffer jthis0);

private static java.lang.reflect.Method platformInfoFactoryMethod;

private static JAWT_PlatformInfo newPlatformInfo(JAWT jawt, ByteBuffer buf) {
  if (platformInfoFactoryMethod == null) {
    try {
        Class<?> factoryClass;
        if (Platform.OS_TYPE == Platform.OSType.WINDOWS) {
          factoryClass = Class.forName("jogamp.nativewindow.jawt.windows.JAWT_Win32DrawingSurfaceInfo");
        } else if (Platform.OS_TYPE == Platform.OSType.MACOS) {
          if( 0 != ( jawt.getCachedVersion() & JAWT.JAWT_MACOSX_USE_CALAYER ) ) {
              factoryClass = Class.forName("jogamp.nativewindow.jawt.macosx.JAWT_SurfaceLayers");
          } else {
              factoryClass = Class.forName("jogamp.nativewindow.jawt.macosx.JAWT_MacOSXDrawingSurfaceInfo");
          }
        } else {
          // Assume Linux, Solaris, etc. Should probably test for these explicitly.
          factoryClass = Class.forName("jogamp.nativewindow.jawt.x11.JAWT_X11DrawingSurfaceInfo");
        }
        platformInfoFactoryMethod = factoryClass.getMethod("create",
                                                           new Class[] { ByteBuffer.class });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  try {
    return (JAWT_PlatformInfo)
      platformInfoFactoryMethod.invoke(null, new Object[] { buf });
  } catch (Exception e) {
    throw new RuntimeException(e);
  }
}
