package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * iCCP chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11iCCP
 */
public class PngChunkICCP extends PngChunkSingle {
	public final static String ID = ChunkHelper.iCCP;

	// http://www.w3.org/TR/PNG/#11iCCP
	private String profileName;
	private byte[] compressedProfile; // copmression/decopmresion is done in getter/setter

	public PngChunkICCP(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_PLTE_AND_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		final ChunkRaw c = createEmptyChunk(profileName.length() + compressedProfile.length + 2, true);
		System.arraycopy(ChunkHelper.toBytes(profileName), 0, c.data, 0, profileName.length());
		c.data[profileName.length()] = 0;
		c.data[profileName.length() + 1] = 0;
		System.arraycopy(compressedProfile, 0, c.data, profileName.length() + 2, compressedProfile.length);
		return c;
	}

	@Override
	public void parseFromRaw(final ChunkRaw chunk) {
		final int pos0 = ChunkHelper.posNullByte(chunk.data);
		profileName = new String(chunk.data, 0, pos0, PngHelperInternal.charsetLatin1);
		final int comp = (chunk.data[pos0 + 1] & 0xff);
		if (comp != 0)
			throw new PngjException("bad compression for ChunkTypeICCP");
		final int compdatasize = chunk.data.length - (pos0 + 2);
		compressedProfile = new byte[compdatasize];
		System.arraycopy(chunk.data, pos0 + 2, compressedProfile, 0, compdatasize);
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkICCP otherx = (PngChunkICCP) other;
		profileName = otherx.profileName;
		compressedProfile = new byte[otherx.compressedProfile.length];
		System.arraycopy(otherx.compressedProfile, 0, compressedProfile, 0, otherx.compressedProfile.length); // deep
																												// copy
	}

	/**
	 * The profile should be uncompressed bytes
	 */
	public void setProfileNameAndContent(final String name, final byte[] profile) {
		profileName = name;
		compressedProfile = ChunkHelper.compressBytes(profile, true);
	}

	public void setProfileNameAndContent(final String name, final String profile) {
		setProfileNameAndContent(name, profile.getBytes(PngHelperInternal.charsetLatin1));
	}

	public String getProfileName() {
		return profileName;
	}

	/**
	 * uncompressed
	 **/
	public byte[] getProfile() {
		return ChunkHelper.compressBytes(compressedProfile, false);
	}

	public String getProfileAsString() {
		return new String(getProfile(), PngHelperInternal.charsetLatin1);
	}

}
