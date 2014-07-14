package jogamp.opengl.util.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import jogamp.opengl.util.pngj.ImageLine.SampleType;
import jogamp.opengl.util.pngj.chunks.ChunkHelper;
import jogamp.opengl.util.pngj.chunks.ChunkLoadBehaviour;
import jogamp.opengl.util.pngj.chunks.ChunkRaw;
import jogamp.opengl.util.pngj.chunks.ChunksList;
import jogamp.opengl.util.pngj.chunks.PngChunk;
import jogamp.opengl.util.pngj.chunks.PngChunkIDAT;
import jogamp.opengl.util.pngj.chunks.PngChunkIHDR;
import jogamp.opengl.util.pngj.chunks.PngChunkSkipped;
import jogamp.opengl.util.pngj.chunks.PngMetadata;

/**
 * Reads a PNG image, line by line.
 * <p>
 * The reading sequence is as follows: <br>
 * 1. At construction time, the header and IHDR chunk are read (basic image
 * info) <br>
 * 2. Afterwards you can set some additional global options. Eg.
 * {@link #setUnpackedMode(boolean)}, {@link #setCrcCheckDisabled()}.<br>
 * 3. Optional: If you call getMetadata() or getChunksLisk() before start
 * reading the rows, all the chunks before IDAT are automatically loaded and
 * available <br>
 * 4a. The rows are read onen by one of the <tt>readRowXXX</tt> methods:
 * {@link #readRowInt(int)}, {@link PngReader#readRowByte(int)}, etc, in order,
 * from 0 to nrows-1 (you can skip or repeat rows, but not go backwards)<br>
 * 4b. Alternatively, you can read all rows, or a subset, in a single call:
 * {@link #readRowsInt()}, {@link #readRowsByte()} ,etc. In general this
 * consumes more memory, but for interlaced images this is equally efficient,
 * and more so if reading a small subset of rows.<br>
 * 5. Read of the last row auyomatically loads the trailing chunks, and ends the
 * reader.<br>
 * 6. end() forcibly finishes/aborts the reading and closes the stream
 */
public class PngReader {

	/**
	 * Basic image info - final and inmutable.
	 */
	public final ImageInfo imgInfo;
	/**
	 * not necesarily a filename, can be a description - merely informative
	 */
	protected final String filename;
	private ChunkLoadBehaviour chunkLoadBehaviour = ChunkLoadBehaviour.LOAD_CHUNK_ALWAYS; // see setter/getter
	private boolean shouldCloseStream = true; // true: closes stream after ending - see setter/getter
	// some performance/defensive limits
	private long maxTotalBytesRead = 200 * 1024 * 1024; // 200MB
	private int maxBytesMetadata = 5 * 1024 * 1024; // for ancillary chunks - see setter/getter
	private int skipChunkMaxSize = 2 * 1024 * 1024; // chunks exceeding this size will be skipped (nor even CRC checked)
	private String[] skipChunkIds = { "fdAT" }; // chunks with these ids will be skipped (nor even CRC checked)
	private HashSet<String> skipChunkIdsSet; // lazily created from skipChunksById
	protected final PngMetadata metadata; // this a wrapper over chunks
	protected final ChunksList chunksList;
	protected ImageLine imgLine;
	// line as bytes, counting from 1 (index 0 is reserved for filter type)
	protected final int buffersLen; // nominal length is imgInfo.bytesPerRow + 1 but it can be larger
	protected byte[] rowb = null;
	protected byte[] rowbprev = null; // rowb previous
	protected byte[] rowbfilter = null; // current line 'filtered': exactly as in uncompressed stream
	// only set for interlaced PNG
	private final boolean interlaced;
	private final PngDeinterlacer deinterlacer;
	private boolean crcEnabled = true;
	// this only influences the 1-2-4 bitdepth format
	private boolean unpackedMode = false;
	private Inflater inflater = null;	// can be reused among several objects. see reuseBuffersFrom()
	/**
	 * Current chunk group, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected int currentChunkGroup = -1;
	protected int rowNum = -1; // last read row number, starting from 0
	private long offset = 0; // offset in InputStream = bytes read
	private int bytesChunksLoaded; // bytes loaded from anciallary chunks
	protected final InputStream inputStream;
	protected InflaterInputStream idatIstream;
	protected PngIDatChunkInputStream iIdatCstream;
	protected CRC32 crctest; // If set to non null, it gets a CRC of the unfiltered bytes, to check for images equality

	/**
	 * Constructs a PngReader from an InputStream.
	 * <p>
	 * See also <code>FileHelper.createPngReader(File f)</code> if available.
	 *
	 * Reads only the signature and first chunk (IDHR)
	 *
	 * @param filenameOrDescription
	 *            : Optional, can be a filename or a description. Just for
	 *            error/debug messages
	 *
	 */
	public PngReader(final InputStream inputStream, final String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.inputStream = inputStream;
		this.chunksList = new ChunksList(null);
		this.metadata = new PngMetadata(chunksList);
		// starts reading: signature
		final byte[] pngid = new byte[8];
		PngHelperInternal.readBytes(inputStream, pngid, 0, pngid.length);
		offset += pngid.length;
		if (!Arrays.equals(pngid, PngHelperInternal.getPngIdSignature()))
			throw new PngjInputException("Bad PNG signature");
		// reads first chunk
		currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;
		final int clen = PngHelperInternal.readInt4(inputStream);
		offset += 4;
		if (clen != 13)
			throw new PngjInputException("IDHR chunk len != 13 ?? " + clen);
		final byte[] chunkid = new byte[4];
		PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
		if (!Arrays.equals(chunkid, ChunkHelper.b_IHDR))
			throw new PngjInputException("IHDR not found as first chunk??? [" + ChunkHelper.toString(chunkid) + "]");
		offset += 4;
		final PngChunkIHDR ihdr = (PngChunkIHDR) readChunk(chunkid, clen, false);
		final boolean alpha = (ihdr.getColormodel() & 0x04) != 0;
		final boolean palette = (ihdr.getColormodel() & 0x01) != 0;
		final boolean grayscale = (ihdr.getColormodel() == 0 || ihdr.getColormodel() == 4);
		// creates ImgInfo and imgLine, and allocates buffers
		imgInfo = new ImageInfo(ihdr.getCols(), ihdr.getRows(), ihdr.getBitspc(), alpha, grayscale, palette);
		interlaced = ihdr.getInterlaced() == 1;
		deinterlacer = interlaced ? new PngDeinterlacer(imgInfo) : null;
		buffersLen = imgInfo.bytesPerRow + 1;
		// some checks
		if (ihdr.getFilmeth() != 0 || ihdr.getCompmeth() != 0 || (ihdr.getInterlaced() & 0xFFFE) != 0)
			throw new PngjInputException("compression method o filter method or interlaced unrecognized ");
		if (ihdr.getColormodel() < 0 || ihdr.getColormodel() > 6 || ihdr.getColormodel() == 1
				|| ihdr.getColormodel() == 5)
			throw new PngjInputException("Invalid colormodel " + ihdr.getColormodel());
		if (ihdr.getBitspc() != 1 && ihdr.getBitspc() != 2 && ihdr.getBitspc() != 4 && ihdr.getBitspc() != 8
				&& ihdr.getBitspc() != 16)
			throw new PngjInputException("Invalid bit depth " + ihdr.getBitspc());
	}

	private boolean firstChunksNotYetRead() {
		return currentChunkGroup < ChunksList.CHUNK_GROUP_1_AFTERIDHR;
	}

	private void allocateBuffers() { // only if needed
		if (rowbfilter == null || rowbfilter.length < buffersLen) {
			rowbfilter = new byte[buffersLen];
			rowb = new byte[buffersLen];
			rowbprev = new byte[buffersLen];
		}
	}

	/**
	 * Reads last Internally called after having read the last line. It reads
	 * extra chunks after IDAT, if present.
	 */
	private void readLastAndClose() {
		// offset = iIdatCstream.getOffset();
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_5_AFTERIDAT) {
			try {
				idatIstream.close();
			} catch (final Exception e) {
			}
			readLastChunks();
		}
		close();
	}

	private void close() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END) { // this could only happen if forced close
			try {
				idatIstream.close();
			} catch (final Exception e) {
			}
			currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
		}
		if (shouldCloseStream) {
			try {
				inputStream.close();
			} catch (final Exception e) {
				throw new PngjInputException("error closing input stream!", e);
			}
		}
	}

	// nbytes: NOT including the filter byte. leaves result in rowb
	private void unfilterRow(final int nbytes) {
		final int ftn = rowbfilter[0];
		final FilterType ft = FilterType.getByVal(ftn);
		if (ft == null)
			throw new PngjInputException("Filter type " + ftn + " invalid");
		switch (ft) {
		case FILTER_NONE:
			unfilterRowNone(nbytes);
			break;
		case FILTER_SUB:
			unfilterRowSub(nbytes);
			break;
		case FILTER_UP:
			unfilterRowUp(nbytes);
			break;
		case FILTER_AVERAGE:
			unfilterRowAverage(nbytes);
			break;
		case FILTER_PAETH:
			unfilterRowPaeth(nbytes);
			break;
		default:
			throw new PngjInputException("Filter type " + ftn + " not implemented");
		}
		if (crctest != null)
			crctest.update(rowb, 1, buffersLen - 1);
	}

	private void unfilterRowAverage(final int nbytes) {
		int i, j, x;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= nbytes; i++, j++) {
			x = j > 0 ? (rowb[j] & 0xff) : 0;
			rowb[i] = (byte) (rowbfilter[i] + (x + (rowbprev[i] & 0xFF)) / 2);
		}
	}

	private void unfilterRowNone(final int nbytes) {
		for (int i = 1; i <= nbytes; i++) {
			rowb[i] = (rowbfilter[i]);
		}
	}

	private void unfilterRowPaeth(final int nbytes) {
		int i, j, x, y;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= nbytes; i++, j++) {
			x = j > 0 ? (rowb[j] & 0xFF) : 0;
			y = j > 0 ? (rowbprev[j] & 0xFF) : 0;
			rowb[i] = (byte) (rowbfilter[i] + PngHelperInternal.filterPaethPredictor(x, rowbprev[i] & 0xFF, y));
		}
	}

	private void unfilterRowSub(final int nbytes) {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++) {
			rowb[i] = (rowbfilter[i]);
		}
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= nbytes; i++, j++) {
			rowb[i] = (byte) (rowbfilter[i] + rowb[j]);
		}
	}

	private void unfilterRowUp(final int nbytes) {
		for (int i = 1; i <= nbytes; i++) {
			rowb[i] = (byte) (rowbfilter[i] + rowbprev[i]);
		}
	}

	/**
	 * Reads chunks before first IDAT. Normally this is called automatically
	 * <p>
	 * Position before: after IDHR (crc included) Position after: just after the
	 * first IDAT chunk id
	 * <P>
	 * This can be called several times (tentatively), it does nothing if
	 * already run
	 * <p>
	 * (Note: when should this be called? in the constructor? hardly, because we
	 * loose the opportunity to call setChunkLoadBehaviour() and perhaps other
	 * settings before reading the first row? but sometimes we want to access
	 * some metadata (plte, phys) before. Because of this, this method can be
	 * called explicitly but is also called implicititly in some methods
	 * (getMetatada(), getChunksList())
	 */
	private final void readFirstChunks() {
		if (!firstChunksNotYetRead())
			return;
		int clen = 0;
		boolean found = false;
		final byte[] chunkid = new byte[4]; // it's important to reallocate in each iteration
		currentChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		while (!found) {
			clen = PngHelperInternal.readInt4(inputStream);
			offset += 4;
			if (clen < 0)
				break;
			PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
			offset += 4;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				found = true;
				currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
				// add dummy idat chunk to list
				chunksList.appendReadChunk(new PngChunkIDAT(imgInfo, clen, offset - 8), currentChunkGroup);
				break;
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				throw new PngjInputException("END chunk found before image data (IDAT) at offset=" + offset);
			}
			if (Arrays.equals(chunkid, ChunkHelper.b_PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
			readChunk(chunkid, clen, false);
			if (Arrays.equals(chunkid, ChunkHelper.b_PLTE))
				currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
		}
		final int idatLen = found ? clen : -1;
		if (idatLen < 0)
			throw new PngjInputException("first idat chunk not found!");
		iIdatCstream = new PngIDatChunkInputStream(inputStream, idatLen, offset);
		if(inflater == null) {
			inflater = new Inflater();
		} else {
		inflater.reset();
		}
		idatIstream = new InflaterInputStream(iIdatCstream, inflater);
		if (!crcEnabled)
			iIdatCstream.disableCrcCheck();
	}

	/**
	 * Reads (and processes) chunks after last IDAT.
	 **/
	void readLastChunks() {
		// PngHelper.logdebug("idat ended? " + iIdatCstream.isEnded());
		currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		if (!iIdatCstream.isEnded())
			iIdatCstream.forceChunkEnd();
		int clen = iIdatCstream.getLenLastChunk();
		final byte[] chunkid = iIdatCstream.getIdLastChunk();
		boolean endfound = false;
		boolean first = true;
		boolean skip = false;
		while (!endfound) {
			skip = false;
			if (!first) {
				clen = PngHelperInternal.readInt4(inputStream);
				offset += 4;
				if (clen < 0)
					throw new PngjInputException("bad chuck len " + clen);
				PngHelperInternal.readBytes(inputStream, chunkid, 0, 4);
				offset += 4;
			}
			first = false;
			if (Arrays.equals(chunkid, ChunkHelper.b_IDAT)) {
				skip = true; // extra dummy (empty?) idat chunk, it can happen, ignore it
			} else if (Arrays.equals(chunkid, ChunkHelper.b_IEND)) {
				currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
				endfound = true;
			}
			readChunk(chunkid, clen, skip);
		}
		if (!endfound)
			throw new PngjInputException("end chunk not found - offset=" + offset);
		// PngHelper.logdebug("end chunk found ok offset=" + offset);
	}

	/**
	 * Reads chunkd from input stream, adds to ChunksList, and returns it. If
	 * it's skipped, a PngChunkSkipped object is created
	 */
	private PngChunk readChunk(final byte[] chunkid, final int clen, final boolean skipforced) {
		if (clen < 0)
			throw new PngjInputException("invalid chunk lenght: " + clen);
		// skipChunksByIdSet is created lazyly, if fist IHDR has already been read
		if (skipChunkIdsSet == null && currentChunkGroup > ChunksList.CHUNK_GROUP_0_IDHR)
			skipChunkIdsSet = new HashSet<String>(Arrays.asList(skipChunkIds));
		final String chunkidstr = ChunkHelper.toString(chunkid);
		final boolean critical = ChunkHelper.isCritical(chunkidstr);
		PngChunk pngChunk = null;
		boolean skip = skipforced;
		if (maxTotalBytesRead > 0 && clen + offset > maxTotalBytesRead)
			throw new PngjInputException("Maximum total bytes to read exceeeded: " + maxTotalBytesRead + " offset:"
					+ offset + " clen=" + clen);
		// an ancillary chunks can be skipped because of several reasons:
		if (currentChunkGroup > ChunksList.CHUNK_GROUP_0_IDHR && !critical)
			skip = skip || (skipChunkMaxSize > 0 && clen >= skipChunkMaxSize) || skipChunkIdsSet.contains(chunkidstr)
					|| (maxBytesMetadata > 0 && clen > maxBytesMetadata - bytesChunksLoaded)
					|| !ChunkHelper.shouldLoad(chunkidstr, chunkLoadBehaviour);
		if (skip) {
			PngHelperInternal.skipBytes(inputStream, clen);
			PngHelperInternal.readInt4(inputStream); // skip - we dont call PngHelperInternal.skipBytes(inputStream,
			// clen + 4) for risk of overflow
			pngChunk = new PngChunkSkipped(chunkidstr, imgInfo, clen);
		} else {
			final ChunkRaw chunk = new ChunkRaw(clen, chunkid, true);
			chunk.readChunkData(inputStream, crcEnabled || critical);
			pngChunk = PngChunk.factory(chunk, imgInfo);
			if (!pngChunk.crit)
				bytesChunksLoaded += chunk.len;
		}
		pngChunk.setOffset(offset - 8L);
		chunksList.appendReadChunk(pngChunk, currentChunkGroup);
		offset += clen + 4L;
		return pngChunk;
	}

	/**
	 * Logs/prints a warning.
	 * <p>
	 * The default behaviour is print to stderr, but it can be overriden.
	 * <p>
	 * This happens rarely - most errors are fatal.
	 */
	protected void logWarn(final String warn) {
		System.err.println(warn);
	}

	/**
	 * @see #setChunkLoadBehaviour(ChunkLoadBehaviour)
	 */
	public ChunkLoadBehaviour getChunkLoadBehaviour() {
		return chunkLoadBehaviour;
	}

	/**
	 * Determines which ancillary chunks (metada) are to be loaded
	 *
	 * @param chunkLoadBehaviour
	 *            {@link ChunkLoadBehaviour}
	 */
	public void setChunkLoadBehaviour(final ChunkLoadBehaviour chunkLoadBehaviour) {
		this.chunkLoadBehaviour = chunkLoadBehaviour;
	}

	/**
	 * All loaded chunks (metada). If we have not yet end reading the image,
	 * this will include only the chunks before the pixels data (IDAT)
	 * <p>
	 * Critical chunks are included, except that all IDAT chunks appearance are
	 * replaced by a single dummy-marker IDAT chunk. These might be copied to
	 * the PngWriter
	 * <p>
	 *
	 * @see #getMetadata()
	 */
	public ChunksList getChunksList() {
		if (firstChunksNotYetRead())
			readFirstChunks();
		return chunksList;
	}

	int getCurrentChunkGroup() {
		return currentChunkGroup;
	}

	/**
	 * High level wrapper over chunksList
	 *
	 * @see #getChunksList()
	 */
	public PngMetadata getMetadata() {
		if (firstChunksNotYetRead())
			readFirstChunks();
		return metadata;
	}

	/**
	 * If called for first time, calls readRowInt. Elsewhere, it calls the
	 * appropiate readRowInt/readRowByte
	 * <p>
	 * In general, specifying the concrete readRowInt/readRowByte is preferrable
	 *
	 * @see #readRowInt(int) {@link #readRowByte(int)}
	 */
	public ImageLine readRow(final int nrow) {
		if (imgLine == null)
			imgLine = new ImageLine(imgInfo, SampleType.INT, unpackedMode);
		return imgLine.sampleType != SampleType.BYTE ? readRowInt(nrow) : readRowByte(nrow);
	}

	/**
	 * Reads the row as INT, storing it in the {@link #imgLine} property and
	 * returning it.
	 *
	 * The row must be greater or equal than the last read row.
	 *
	 * @param nrow
	 *            Row number, from 0 to rows-1. Increasing order.
	 * @return ImageLine object, also available as field. Data is in
	 *         {@link ImageLine#scanline} (int) field.
	 */
	public ImageLine readRowInt(final int nrow) {
		if (imgLine == null)
			imgLine = new ImageLine(imgInfo, SampleType.INT, unpackedMode);
		if (imgLine.getRown() == nrow) // already read
			return imgLine;
		readRowInt(imgLine.scanline, nrow);
		imgLine.setFilterUsed(FilterType.getByVal(rowbfilter[0]));
		imgLine.setRown(nrow);
		return imgLine;
	}

	/**
	 * Reads the row as BYTES, storing it in the {@link #imgLine} property and
	 * returning it.
	 *
	 * The row must be greater or equal than the last read row. This method
	 * allows to pass the same row that was last read.
	 *
	 * @param nrow
	 *            Row number, from 0 to rows-1. Increasing order.
	 * @return ImageLine object, also available as field. Data is in
	 *         {@link ImageLine#scanlineb} (byte) field.
	 */
	public ImageLine readRowByte(final int nrow) {
		if (imgLine == null)
			imgLine = new ImageLine(imgInfo, SampleType.BYTE, unpackedMode);
		if (imgLine.getRown() == nrow) // already read
			return imgLine;
		readRowByte(imgLine.scanlineb, nrow);
		imgLine.setFilterUsed(FilterType.getByVal(rowbfilter[0]));
		imgLine.setRown(nrow);
		return imgLine;
	}

	/**
	 * @see #readRowInt(int[], int)
	 */
	public final int[] readRow(final int[] buffer, final int nrow) {
		return readRowInt(buffer, nrow);
	}

	/**
	 * Reads a line and returns it as a int[] array.
	 * <p>
	 * You can pass (optionally) a prealocatted buffer.
	 * <p>
	 * If the bitdepth is less than 8, the bytes are packed - unless
	 * {@link #unpackedMode} is true.
	 *
	 * @param buffer
	 *            Prealocated buffer, or null.
	 * @param nrow
	 *            Row number (0 is top). Most be strictly greater than the last
	 *            read row.
	 *
	 * @return The scanline in the same passwd buffer if it was allocated, a
	 *         newly allocated one otherwise
	 */
	public final int[] readRowInt(int[] buffer, final int nrow) {
		if (buffer == null)
			buffer = new int[unpackedMode ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked];
		if (!interlaced) {
			if (nrow <= rowNum)
				throw new PngjInputException("rows must be read in increasing order: " + nrow);
			int bytesread = 0;
			while (rowNum < nrow)
				bytesread = readRowRaw(rowNum + 1); // read rows, perhaps skipping if necessary
			decodeLastReadRowToInt(buffer, bytesread);
		} else { // interlaced
			if (deinterlacer.getImageInt() == null)
				deinterlacer.setImageInt(readRowsInt().scanlines); // read all image and store it in deinterlacer
			System.arraycopy(deinterlacer.getImageInt()[nrow], 0, buffer, 0, unpackedMode ? imgInfo.samplesPerRow
					: imgInfo.samplesPerRowPacked);
		}
		return buffer;
	}

	/**
	 * Reads a line and returns it as a byte[] array.
	 * <p>
	 * You can pass (optionally) a prealocatted buffer.
	 * <p>
	 * If the bitdepth is less than 8, the bytes are packed - unless
	 * {@link #unpackedMode} is true. <br>
	 * If the bitdepth is 16, the least significant byte is lost.
	 * <p>
	 *
	 * @param buffer
	 *            Prealocated buffer, or null.
	 * @param nrow
	 *            Row number (0 is top). Most be strictly greater than the last
	 *            read row.
	 *
	 * @return The scanline in the same passwd buffer if it was allocated, a
	 *         newly allocated one otherwise
	 */
	public final byte[] readRowByte(byte[] buffer, final int nrow) {
		if (buffer == null)
			buffer = new byte[unpackedMode ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked];
		if (!interlaced) {
			if (nrow <= rowNum)
				throw new PngjInputException("rows must be read in increasing order: " + nrow);
			int bytesread = 0;
			while (rowNum < nrow)
				bytesread = readRowRaw(rowNum + 1); // read rows, perhaps skipping if necessary
			decodeLastReadRowToByte(buffer, bytesread);
		} else { // interlaced
			if (deinterlacer.getImageByte() == null)
				deinterlacer.setImageByte(readRowsByte().scanlinesb); // read all image and store it in deinterlacer
			System.arraycopy(deinterlacer.getImageByte()[nrow], 0, buffer, 0, unpackedMode ? imgInfo.samplesPerRow
					: imgInfo.samplesPerRowPacked);
		}
		return buffer;
	}

	/**
	 * @param nrow
	 * @deprecated Now {@link #readRow(int)} implements the same funcion. This
	 *             method will be removed in future releases
	 */
	public ImageLine getRow(final int nrow) {
		return readRow(nrow);
	}

	private void decodeLastReadRowToInt(final int[] buffer, final int bytesRead) {
		if (imgInfo.bitDepth <= 8)
			for (int i = 0, j = 1; i < bytesRead; i++)
				buffer[i] = (rowb[j++] & 0xFF); // http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		else
			for (int i = 0, j = 1; j <= bytesRead; i++)
				buffer[i] = ((rowb[j++] & 0xFF) << 8) + (rowb[j++] & 0xFF); // 16 bitspc
		if (imgInfo.packed && unpackedMode)
			ImageLine.unpackInplaceInt(imgInfo, buffer, buffer, false);
	}

	private void decodeLastReadRowToByte(final byte[] buffer, final int bytesRead) {
		if (imgInfo.bitDepth <= 8)
			System.arraycopy(rowb, 1, buffer, 0, bytesRead);
		else
			for (int i = 0, j = 1; j < bytesRead; i++, j += 2)
				buffer[i] = rowb[j];// 16 bits in 1 byte: this discards the LSB!!!
		if (imgInfo.packed && unpackedMode)
			ImageLine.unpackInplaceByte(imgInfo, buffer, buffer, false);
	}

	/**
	 * Reads a set of lines and returns it as a ImageLines object, which wraps
	 * matrix. Internally it reads all lines, but decodes and stores only the
	 * wanted ones. This starts and ends the reading, and cannot be combined
	 * with other reading methods.
	 * <p>
	 * This it's more efficient (speed an memory) that doing calling
	 * readRowInt() for each desired line only if the image is interlaced.
	 * <p>
	 * Notice that the columns in the matrix is not the pixel width of the
	 * image, but rather pixels x channels
	 *
	 * @see #readRowInt(int) to read about the format of each row
	 *
	 * @param rowOffset
	 *            Number of rows to be skipped
	 * @param nRows
	 *            Total number of rows to be read. -1: read all available
	 * @param rowStep
	 *            Row increment. If 1, we read consecutive lines; if 2, we read
	 *            even/odd lines, etc
	 * @return Set of lines as a ImageLines, which wraps a matrix
	 */
	public ImageLines readRowsInt(final int rowOffset, int nRows, final int rowStep) {
		if (nRows < 0)
			nRows = (imgInfo.rows - rowOffset) / rowStep;
		if (rowStep < 1 || rowOffset < 0 || nRows * rowStep + rowOffset > imgInfo.rows)
			throw new PngjInputException("bad args");
		final ImageLines imlines = new ImageLines(imgInfo, SampleType.INT, unpackedMode, rowOffset, nRows, rowStep);
		if (!interlaced) {
			for (int j = 0; j < imgInfo.rows; j++) {
				final int bytesread = readRowRaw(j); // read and perhaps discards
				final int mrow = imlines.imageRowToMatrixRowStrict(j);
				if (mrow >= 0)
					decodeLastReadRowToInt(imlines.scanlines[mrow], bytesread);
			}
		} else { // and now, for something completely different (interlaced)
			final int[] buf = new int[unpackedMode ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked];
			for (int p = 1; p <= 7; p++) {
				deinterlacer.setPass(p);
				for (int i = 0; i < deinterlacer.getRows(); i++) {
					final int bytesread = readRowRaw(i);
					final int j = deinterlacer.getCurrRowReal();
					final int mrow = imlines.imageRowToMatrixRowStrict(j);
					if (mrow >= 0) {
						decodeLastReadRowToInt(buf, bytesread);
						deinterlacer.deinterlaceInt(buf, imlines.scanlines[mrow], !unpackedMode);
					}
				}
			}
		}
		end();
		return imlines;
	}

	/**
	 * Same as readRowsInt(0, imgInfo.rows, 1)
	 *
	 * @see #readRowsInt(int, int, int)
	 */
	public ImageLines readRowsInt() {
		return readRowsInt(0, imgInfo.rows, 1);
	}

	/**
	 * Reads a set of lines and returns it as a ImageLines object, which wrapas
	 * a byte[][] matrix. Internally it reads all lines, but decodes and stores
	 * only the wanted ones. This starts and ends the reading, and cannot be
	 * combined with other reading methods.
	 * <p>
	 * This it's more efficient (speed an memory) that doing calling
	 * readRowByte() for each desired line only if the image is interlaced.
	 * <p>
	 * Notice that the columns in the matrix is not the pixel width of the
	 * image, but rather pixels x channels
	 *
	 * @see #readRowByte(int) to read about the format of each row. Notice that
	 *      if the bitdepth is 16 this will lose information
	 *
	 * @param rowOffset
	 *            Number of rows to be skipped
	 * @param nRows
	 *            Total number of rows to be read. -1: read all available
	 * @param rowStep
	 *            Row increment. If 1, we read consecutive lines; if 2, we read
	 *            even/odd lines, etc
	 * @return Set of lines as a matrix
	 */
	public ImageLines readRowsByte(final int rowOffset, int nRows, final int rowStep) {
		if (nRows < 0)
			nRows = (imgInfo.rows - rowOffset) / rowStep;
		if (rowStep < 1 || rowOffset < 0 || nRows * rowStep + rowOffset > imgInfo.rows)
			throw new PngjInputException("bad args");
		final ImageLines imlines = new ImageLines(imgInfo, SampleType.BYTE, unpackedMode, rowOffset, nRows, rowStep);
		if (!interlaced) {
			for (int j = 0; j < imgInfo.rows; j++) {
				final int bytesread = readRowRaw(j); // read and perhaps discards
				final int mrow = imlines.imageRowToMatrixRowStrict(j);
				if (mrow >= 0)
					decodeLastReadRowToByte(imlines.scanlinesb[mrow], bytesread);
			}
		} else { // and now, for something completely different (interlaced)
			final byte[] buf = new byte[unpackedMode ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked];
			for (int p = 1; p <= 7; p++) {
				deinterlacer.setPass(p);
				for (int i = 0; i < deinterlacer.getRows(); i++) {
					final int bytesread = readRowRaw(i);
					final int j = deinterlacer.getCurrRowReal();
					final int mrow = imlines.imageRowToMatrixRowStrict(j);
					if (mrow >= 0) {
						decodeLastReadRowToByte(buf, bytesread);
						deinterlacer.deinterlaceByte(buf, imlines.scanlinesb[mrow], !unpackedMode);
					}
				}
			}
		}
		end();
		return imlines;
	}

	/**
	 * Same as readRowsByte(0, imgInfo.rows, 1)
	 *
	 * @see #readRowsByte(int, int, int)
	 */
	public ImageLines readRowsByte() {
		return readRowsByte(0, imgInfo.rows, 1);
	}

	/*
	 * For the interlaced case, nrow indicates the subsampled image - the pass must be set already.
	 *
	 * This must be called in strict order, both for interlaced or no interlaced.
	 *
	 * Updates rowNum.
	 *
	 * Leaves raw result in rowb
	 *
	 * Returns bytes actually read (not including the filter byte)
	 */
	private int readRowRaw(final int nrow) {
		if (nrow == 0) {
			if (firstChunksNotYetRead())
				readFirstChunks();
			allocateBuffers();
			if (interlaced)
				Arrays.fill(rowb, (byte) 0); // new subimage: reset filters: this is enough, see the swap that happens lines
		}
		// below
		int bytesRead = imgInfo.bytesPerRow; // NOT including the filter byte
		if (interlaced) {
			if (nrow < 0 || nrow > deinterlacer.getRows() || (nrow != 0 && nrow != deinterlacer.getCurrRowSubimg() + 1))
				throw new PngjInputException("invalid row in interlaced mode: " + nrow);
			deinterlacer.setRow(nrow);
			bytesRead = (imgInfo.bitspPixel * deinterlacer.getPixelsToRead() + 7) / 8;
			if (bytesRead < 1)
				throw new PngjExceptionInternal("wtf??");
		} else { // check for non interlaced
			if (nrow < 0 || nrow >= imgInfo.rows || nrow != rowNum + 1)
				throw new PngjInputException("invalid row: " + nrow);
		}
		rowNum = nrow;
		// swap buffers
		final byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
		// loads in rowbfilter "raw" bytes, with filter
		PngHelperInternal.readBytes(idatIstream, rowbfilter, 0, bytesRead + 1);
		offset = iIdatCstream.getOffset();
		if (offset < 0)
			throw new PngjExceptionInternal("bad offset ??" + offset);
		if (maxTotalBytesRead > 0 && offset >= maxTotalBytesRead)
			throw new PngjInputException("Reading IDAT: Maximum total bytes to read exceeeded: " + maxTotalBytesRead
					+ " offset:" + offset);
		rowb[0] = 0;
		unfilterRow(bytesRead);
		rowb[0] = rowbfilter[0];
		if ((rowNum == imgInfo.rows - 1 && !interlaced) || (interlaced && deinterlacer.isAtLastRow()))
			readLastAndClose();
		return bytesRead;
	}

	/**
	 * Reads all the (remaining) file, skipping the pixels data. This is much
	 * more efficient that calling readRow(), specially for big files (about 10
	 * times faster!), because it doesn't even decompress the IDAT stream and
	 * disables CRC check Use this if you are not interested in reading
	 * pixels,only metadata.
	 */
	public void readSkippingAllRows() {
		if (firstChunksNotYetRead())
			readFirstChunks();
		// we read directly from the compressed stream, we dont decompress nor chec CRC
		iIdatCstream.disableCrcCheck();
		allocateBuffers();
		try {
			int r;
			do {
				r = iIdatCstream.read(rowbfilter, 0, buffersLen);
			} while (r >= 0);
		} catch (final IOException e) {
			throw new PngjInputException("error in raw read of IDAT", e);
		}
		offset = iIdatCstream.getOffset();
		if (offset < 0)
			throw new PngjExceptionInternal("bad offset ??" + offset);
		if (maxTotalBytesRead > 0 && offset >= maxTotalBytesRead)
			throw new PngjInputException("Reading IDAT: Maximum total bytes to read exceeeded: " + maxTotalBytesRead
					+ " offset:" + offset);
		readLastAndClose();
	}

	/**
	 * Set total maximum bytes to read (0: unlimited; default: 200MB). <br>
	 * These are the bytes read (not loaded) in the input stream. If exceeded,
	 * an exception will be thrown.
	 */
	public void setMaxTotalBytesRead(final long maxTotalBytesToRead) {
		this.maxTotalBytesRead = maxTotalBytesToRead;
	}

	/**
	 * @return Total maximum bytes to read.
	 */
	public long getMaxTotalBytesRead() {
		return maxTotalBytesRead;
	}

	/**
	 * Set total maximum bytes to load from ancillary chunks (0: unlimited;
	 * default: 5Mb).<br>
	 * If exceeded, some chunks will be skipped
	 */
	public void setMaxBytesMetadata(final int maxBytesChunksToLoad) {
		this.maxBytesMetadata = maxBytesChunksToLoad;
	}

	/**
	 * @return Total maximum bytes to load from ancillary ckunks.
	 */
	public int getMaxBytesMetadata() {
		return maxBytesMetadata;
	}

	/**
	 * Set maximum size in bytes for individual ancillary chunks (0: unlimited;
	 * default: 2MB). <br>
	 * Chunks exceeding this length will be skipped (the CRC will not be
	 * checked) and the chunk will be saved as a PngChunkSkipped object. See
	 * also setSkipChunkIds
	 */
	public void setSkipChunkMaxSize(final int skipChunksBySize) {
		this.skipChunkMaxSize = skipChunksBySize;
	}

	/**
	 * @return maximum size in bytes for individual ancillary chunks.
	 */
	public int getSkipChunkMaxSize() {
		return skipChunkMaxSize;
	}

	/**
	 * Chunks ids to be skipped. <br>
	 * These chunks will be skipped (the CRC will not be checked) and the chunk
	 * will be saved as a PngChunkSkipped object. See also setSkipChunkMaxSize
	 */
	public void setSkipChunkIds(final String[] skipChunksById) {
		this.skipChunkIds = skipChunksById == null ? new String[] {} : skipChunksById;
	}

	/**
	 * @return Chunk-IDs to be skipped.
	 */
	public String[] getSkipChunkIds() {
		return skipChunkIds;
	}

	/**
	 * if true, input stream will be closed after ending read
	 * <p>
	 * default=true
	 */
	public void setShouldCloseStream(final boolean shouldCloseStream) {
		this.shouldCloseStream = shouldCloseStream;
	}

	/**
	 * Normally this does nothing, but it can be used to force a premature
	 * closing. Its recommended practice to call it after reading the image
	 * pixels.
	 */
	public void end() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END)
			close();
	}

	/**
	 * Interlaced PNG is accepted -though not welcomed- now...
	 */
	public boolean isInterlaced() {
		return interlaced;
	}

	/**
	 * set/unset "unpackedMode"<br>
	 * If false (default) packed types (bitdepth=1,2 or 4) will keep several
	 * samples packed in one element (byte or int) <br>
	 * If true, samples will be unpacked on reading, and each element in the
	 * scanline will be sample. This implies more processing and memory, but
	 * it's the most efficient option if you intend to read individual pixels. <br>
	 * This option should only be set before start reading.
	 *
	 * @param unPackedMode
	 */
	public void setUnpackedMode(final boolean unPackedMode) {
		this.unpackedMode = unPackedMode;
	}

	/**
	 * @see PngReader#setUnpackedMode(boolean)
	 */
	public boolean isUnpackedMode() {
		return unpackedMode;
	}

	/**
	 * Tries to reuse the allocated buffers from other already used PngReader
	 * object. This will have no effect if the buffers are smaller than necessary.
	 * It also reuses the inflater.
	 *
	 * @param other A PngReader that has already finished reading pixels. Can be null.
	 */
	public void reuseBuffersFrom(final PngReader other) {
		if(other==null) return;
		if (other.currentChunkGroup < ChunksList.CHUNK_GROUP_5_AFTERIDAT)
			throw new PngjInputException("PngReader to be reused have not yet ended reading pixels");
		if (other.rowbfilter != null && other.rowbfilter.length >= buffersLen) {
			rowbfilter = other.rowbfilter;
			rowb = other.rowb;
			rowbprev = other.rowbprev;
		}
		inflater = other.inflater;
	}

	/**
	 * Disables the CRC integrity check in IDAT chunks and ancillary chunks,
	 * this gives a slight increase in reading speed for big files
	 */
	public void setCrcCheckDisabled() {
		crcEnabled = false;
	}

	/**
	 * Just for testing. TO be called after ending reading, only if
	 * initCrctest() was called before start
	 *
	 * @return CRC of the raw pixels values
	 */
	long getCrctestVal() {
		return crctest.getValue();
	}

	/**
	 * Inits CRC object and enables CRC calculation
	 */
	void initCrctest() {
		this.crctest = new CRC32();
	}

	/**
	 * Basic info, for debugging.
	 */
	@Override
	public String toString() { // basic info
		return "filename=" + filename + " " + imgInfo.toString();
	}
}
