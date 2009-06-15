public JAWT_PlatformInfo platformInfo() {
  return newPlatformInfo(platformInfo0(getBuffer()));
}

private native ByteBuffer platformInfo0(Buffer jthis0);

private static java.lang.reflect.Method platformInfoFactoryMethod;

private static JAWT_PlatformInfo newPlatformInfo(ByteBuffer buf) {
  if (platformInfoFactoryMethod == null) {
    String osName = (String) AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return System.getProperty("os.name").toLowerCase();
        }
      });
    try {
      Class factoryClass;
      if (osName.startsWith("wind")) {
        factoryClass = Class.forName("com.sun.nativewindow.impl.jawt.windows.JAWT_Win32DrawingSurfaceInfo");
      } else if (osName.startsWith("mac os x")) {
        factoryClass = Class.forName("com.sun.nativewindow.impl.jawt.macosx.JAWT_MacOSXDrawingSurfaceInfo");
      } else {
        // Assume Linux, Solaris, etc. Should probably test for these explicitly.
        factoryClass = Class.forName("com.sun.nativewindow.impl.jawt.x11.JAWT_X11DrawingSurfaceInfo");
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
