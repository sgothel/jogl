  
  private static final long hInstance;

  static {
    NWJNILibLoader.loadNativeWindow("win32");
    hInstance = initIDs0();
    if( 0 == hInstance ) {
        throw new NativeWindowException("GDI: Could not initialized native stub");
    }
  }

  public static synchronized void initSingleton() {
  }
  private static native long initIDs0();

  public static long getModuleHandle() {
    return hInstance;
  }

  public static Point GetRelativeLocation(long src_win, long dest_win, int src_x, int src_y) {
    return (Point) GetRelativeLocation0(src_win, dest_win, src_x, src_y);
  }
  private static native Object GetRelativeLocation0(long src_win, long dest_win, int src_x, int src_y);

