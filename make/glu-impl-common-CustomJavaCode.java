/** Indicates whether the given GLU routine is available to be called. */
public boolean isFunctionAvailable(String gluFunctionName)
{
  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

private GLUProcAddressTable gluProcAddressTable;
public GLUImpl(GLUProcAddressTable gluProcAddressTable)
{
  this.gluProcAddressTable = gluProcAddressTable;
}
