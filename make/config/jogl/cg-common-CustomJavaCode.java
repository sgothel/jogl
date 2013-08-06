private static final DynamicLibraryBundle cgDynamicLookupHelper;
private static final CgProcAddressTable cgProcAddressTable;

static {
    cgProcAddressTable = new CgProcAddressTable();
    if(null==cgProcAddressTable) {
      throw new RuntimeException("Couldn't instantiate CgProcAddressTable");
    }

    cgDynamicLookupHelper = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                                public DynamicLibraryBundle run() {
                                    return new DynamicLibraryBundle(new CgDynamicLibraryBundleInfo());
                                } } );
    if(null==cgDynamicLookupHelper) {
      throw new RuntimeException("Null CgDynamicLookupHelper");
    }
    if(!cgDynamicLookupHelper.isToolLibLoaded()) {
      throw new RuntimeException("Couln't load native Cg or CgGL library");
    }
    if(!cgDynamicLookupHelper.isGlueLibLoaded(CgDynamicLibraryBundleInfo.getCgGlueLibIndex())) {
      throw new RuntimeException("Couln't load native GLue/JNI library");
    }
    cgProcAddressTable.reset(cgDynamicLookupHelper);
}

public  static CgProcAddressTable getCgProcAddressTable() { return cgProcAddressTable; }

/** A convenience method which reads all available data from the InputStream and then calls cgCreateProgram. */
public static CGprogram cgCreateProgramFromStream(CGcontext ctx, int program_type, java.io.InputStream stream, int profile, java.lang.String entry, java.lang.String[] args) throws java.io.IOException {
  if (stream == null) {
    throw new java.io.IOException("null stream");
  }
  stream = new java.io.BufferedInputStream(stream);
  int avail = stream.available();
  byte[] data = new byte[avail];
  int numRead = 0;
  int pos = 0;
  do {
    if (pos + avail > data.length) {
      byte[] newData = new byte[pos + avail];
      System.arraycopy(data, 0, newData, 0, pos);
      data = newData;
    }
    numRead = stream.read(data, pos, avail);
    if (numRead >= 0) {
      pos += numRead;
    }
    avail = stream.available();
  } while (avail > 0 && numRead >= 0);
  String program = new String(data, 0, pos, "US-ASCII");
  return cgCreateProgram(ctx, program_type, program, profile, entry, args);
}
