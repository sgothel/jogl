package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;

/*
 */
public class PngChunkICCP extends PngChunk {
	// http://www.w3.org/TR/PNG/#11iCCP
	private String profileName;
	private byte[] compressedProfile; // copmression/decopmresion is done in getter/setter

	public PngChunkICCP(ImageInfo info) {
		super(ChunkHelper.iCCP, info);
	}

	@Override
	public boolean mustGoBeforeIDAT() {
		return true;
	}

	@Override
	public boolean mustGoBeforePLTE() {
		return true;
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = createEmptyChunk(profileName.length() + compressedProfile.length + 2, true);
		System.arraycopy(ChunkHelper.toBytes(profileName), 0, c.data, 0, profileName.length());
		c.data[profileName.length()] = 0;
		c.data[profileName.length() + 1] = 0;
		System.arraycopy(compressedProfile, 0, c.data, profileName.length() + 2, compressedProfile.length);
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw chunk) {
		int pos0 = ChunkHelper.posNullByte(chunk.data);
		profileName = new String(chunk.data, 0, pos0, PngHelper.charsetLatin1);
		int comp = (chunk.data[pos0 + 1] & 0xff);
		if (comp != 0)
			throw new RuntimeException("bad compression for ChunkTypeICCP");
		int compdatasize = chunk.data.length - (pos0 + 2);
		compressedProfile = new byte[compdatasize];
		System.arraycopy(chunk.data, pos0 + 2, compressedProfile, 0, compdatasize);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkICCP otherx = (PngChunkICCP) other;
		profileName = otherx.profileName;
		compressedProfile = new byte[otherx.compressedProfile.length];
		System.arraycopy(otherx.compressedProfile, 0, compressedProfile, 0, otherx.compressedProfile.length); // deep
																												// copy
	}

	/**
	 * The profile should be uncompressed bytes
	 */
	public void setProfileNameAndContent(String name, byte[] profile) {
		profileName = name;
		compressedProfile = ChunkHelper.compressBytes(profile, true);
	}

	public void setProfileNameAndContent(String name, String profile) {
		setProfileNameAndContent(name, profile.getBytes(PngHelper.charsetLatin1));
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
		return new String(getProfile(), PngHelper.charsetLatin1);
	}

}
