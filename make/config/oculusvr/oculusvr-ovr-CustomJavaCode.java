
    static final DynamicLibraryBundle dynamicLookupHelper;

    static {
        dynamicLookupHelper = new DynamicLibraryBundle(new OVRDynamicLibraryBundleInfo());
        if(null==dynamicLookupHelper) {
          throw new RuntimeException("Null DynamicLibraryBundle");
        }
        /** No native tool library to load
        if(!dynamicLookupHelper.isToolLibLoaded()) {
          System.err.println("Couldn't load native OVR/JNI glue library");
          return null;
        } */
        if(!dynamicLookupHelper.isLibComplete()) {
          System.err.println("Couldn't load native OVR/JNI glue library");
          return null;
        }
        /** Not required nor forced
        if( !initializeImpl() ) {
          System.err.println("Native initialization failure of OVR/JNI glue library");
          return null;
        } */
    }

    /**
     * Accessor.
     * @returns true if OVR library is available on this machine.
     */
    public static boolean isAvailable() { return dynamicLookupHelper != null; }

