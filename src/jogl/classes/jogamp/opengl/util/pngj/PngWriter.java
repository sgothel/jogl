package jogamp.opengl.util.pngj;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import jogamp.opengl.util.pngj.ImageLine.SampleType;
import jogamp.opengl.util.pngj.chunks.ChunkCopyBehaviour;
import jogamp.opengl.util.pngj.chunks.ChunkHelper;
import jogamp.opengl.util.pngj.chunks.ChunksList;
import jogamp.opengl.util.pngj.chunks.ChunksListForWrite;
import jogamp.opengl.util.pngj.chunks.PngChunk;
import jogamp.opengl.util.pngj.chunks.PngChunkIEND;
import jogamp.opengl.util.pngj.chunks.PngChunkIHDR;
import jogamp.opengl.util.pngj.chunks.PngChunkSkipped;
import jogamp.opengl.util.pngj.chunks.PngChunkTextVar;
import jogamp.opengl.util.pngj.chunks.PngMetadata;

/**
 * Writes a PNG image
 */
public class PngWriter {

	public final ImageInfo imgInfo;

	private final String filename; // optional, can be a description

	/**
	 * last read row number, starting from 0
	 */
	protected int rowNum = -1;

	private final ChunksListForWrite chunksList;

	private final PngMetadata metadata; // high level wrapper over chunkList

	/**
	 * Current chunk grounp, (0-6) already read or reading
	 * <p>
	 * see {@link ChunksList}
	 */
	protected int currentChunkGroup = -1;

	/**
	 * PNG filter strategy
	 */
	protected FilterWriteStrategy filterStrat;

	/**
	 * zip compression level 0 - 9
	 */
	private int compLevel = 6;
	private boolean shouldCloseStream = true; // true: closes stream after ending write

	private PngIDatChunkOutputStream datStream;

	private DeflaterOutputStream datStreamDeflated;

	/**
	 * Deflate algortithm compression strategy
	 */
	private int deflaterStrategy = Deflater.FILTERED;

	private final int[] histox = new int[256]; // auxiliar buffer, only used by reportResultsForFilter

	private int idatMaxSize = 0; // 0=use default (PngIDatChunkOutputStream 32768)

	private final OutputStream os;

	protected byte[] rowb = null; // element 0 is filter type!
	protected byte[] rowbfilter = null; // current line with filter

	protected byte[] rowbprev = null; // rowb prev

	// this only influences the 1-2-4 bitdepth format - and if we pass a ImageLine to writeRow, this is ignored
	private boolean unpackedMode = false;

	public PngWriter(final OutputStream outputStream, final ImageInfo imgInfo) {
		this(outputStream, imgInfo, "[NO FILENAME AVAILABLE]");
	}

	/**
	 * Constructs a new PngWriter from a output stream. After construction
	 * nothing is writen yet. You still can set some parameters (compression,
	 * filters) and queue chunks before start writing the pixels.
	 * <p>
	 * See also <code>FileHelper.createPngWriter()</code> if available.
	 *
	 * @param outputStream
	 *            Opened stream for binary writing
	 * @param imgInfo
	 *            Basic image parameters
	 * @param filenameOrDescription
	 *            Optional, just for error/debug messages
	 */
	public PngWriter(final OutputStream outputStream, final ImageInfo imgInfo, final String filenameOrDescription) {
		this.filename = filenameOrDescription == null ? "" : filenameOrDescription;
		this.os = outputStream;
		this.imgInfo = imgInfo;
		// prealloc
		rowb = new byte[imgInfo.bytesPerRow + 1];
		rowbprev = new byte[rowb.length];
		rowbfilter = new byte[rowb.length];
		chunksList = new ChunksListForWrite(imgInfo);
		metadata = new PngMetadata(chunksList);
		filterStrat = new FilterWriteStrategy(imgInfo, FilterType.FILTER_DEFAULT); // can be changed
	}

	private void init() {
		datStream = new PngIDatChunkOutputStream(this.os, idatMaxSize);
		final Deflater def = new Deflater(compLevel);
		def.setStrategy(deflaterStrategy);
		datStreamDeflated = new DeflaterOutputStream(datStream, def);
		writeSignatureAndIHDR();
		writeFirstChunks();
	}

	private void reportResultsForFilter(final int rown, final FilterType type, final boolean tentative) {
		Arrays.fill(histox, 0);
		int s = 0, v;
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			v = rowbfilter[i];
			if (v < 0)
				s -= v;
			else
				s += v;
			histox[v & 0xFF]++;
		}
		filterStrat.fillResultsForFilter(rown, type, s, histox, tentative);
	}

	private void writeEndChunk() {
		final PngChunkIEND c = new PngChunkIEND(imgInfo);
		c.createRawChunk().writeChunk(os);
	}

	private void writeFirstChunks() {
		int nw = 0;
		currentChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		nw = chunksList.writeChunks(os, currentChunkGroup);
		currentChunkGroup = ChunksList.CHUNK_GROUP_2_PLTE;
		nw = chunksList.writeChunks(os, currentChunkGroup);
		if (nw > 0 && imgInfo.greyscale)
			throw new PngjOutputException("cannot write palette for this format");
		if (nw == 0 && imgInfo.indexed)
			throw new PngjOutputException("missing palette");
		currentChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
		nw = chunksList.writeChunks(os, currentChunkGroup);
		currentChunkGroup = ChunksList.CHUNK_GROUP_4_IDAT;
	}

	private void writeLastChunks() { // not including end
		currentChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
		chunksList.writeChunks(os, currentChunkGroup);
		// should not be unwriten chunks
		final List<PngChunk> pending = chunksList.getQueuedChunks();
		if (!pending.isEmpty())
			throw new PngjOutputException(pending.size() + " chunks were not written! Eg: " + pending.get(0).toString());
		currentChunkGroup = ChunksList.CHUNK_GROUP_6_END;
	}

	/**
	 * Write id signature and also "IHDR" chunk
	 */
	private void writeSignatureAndIHDR() {
		currentChunkGroup = ChunksList.CHUNK_GROUP_0_IDHR;

		PngHelperInternal.writeBytes(os, PngHelperInternal.getPngIdSignature()); // signature
		final PngChunkIHDR ihdr = new PngChunkIHDR(imgInfo);
		// http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
		ihdr.setCols(imgInfo.cols);
		ihdr.setRows(imgInfo.rows);
		ihdr.setBitspc(imgInfo.bitDepth);
		int colormodel = 0;
		if (imgInfo.alpha)
			colormodel += 0x04;
		if (imgInfo.indexed)
			colormodel += 0x01;
		if (!imgInfo.greyscale)
			colormodel += 0x02;
		ihdr.setColormodel(colormodel);
		ihdr.setCompmeth(0); // compression method 0=deflate
		ihdr.setFilmeth(0); // filter method (0)
		ihdr.setInterlaced(0); // we never interlace
		ihdr.createRawChunk().writeChunk(os);

	}

	protected void encodeRowFromByte(final byte[] row) {
		if (row.length == imgInfo.samplesPerRowPacked) {
			// some duplication of code - because this case is typical and it works faster this way
			int j = 1;
			if (imgInfo.bitDepth <= 8) {
				for (final byte x : row) { // optimized
					rowb[j++] = x;
				}
			} else { // 16 bitspc
				for (final byte x : row) { // optimized
					rowb[j] = x;
					j += 2;
				}
			}
		} else {
			// perhaps we need to pack?
			if (row.length >= imgInfo.samplesPerRow && unpackedMode)
				ImageLine.packInplaceByte(imgInfo, row, row, false); // row is packed in place!
			if (imgInfo.bitDepth <= 8) {
				for (int i = 0, j = 1; i < imgInfo.samplesPerRowPacked; i++) {
					rowb[j++] = row[i];
				}
			} else { // 16 bitspc
				for (int i = 0, j = 1; i < imgInfo.samplesPerRowPacked; i++) {
					rowb[j++] = row[i];
					rowb[j++] = 0;
				}
			}

		}
	}

	protected void encodeRowFromInt(final int[] row) {
		// http://www.libpng.org/pub/png/spec/1.2/PNG-DataRep.html
		if (row.length == imgInfo.samplesPerRowPacked) {
			// some duplication of code - because this case is typical and it works faster this way
			int j = 1;
			if (imgInfo.bitDepth <= 8) {
				for (final int x : row) { // optimized
					rowb[j++] = (byte) x;
				}
			} else { // 16 bitspc
				for (final int x : row) { // optimized
					rowb[j++] = (byte) (x >> 8);
					rowb[j++] = (byte) (x);
				}
			}
		} else {
			// perhaps we need to pack?
			if (row.length >= imgInfo.samplesPerRow && unpackedMode)
				ImageLine.packInplaceInt(imgInfo, row, row, false); // row is packed in place!
			if (imgInfo.bitDepth <= 8) {
				for (int i = 0, j = 1; i < imgInfo.samplesPerRowPacked; i++) {
					rowb[j++] = (byte) (row[i]);
				}
			} else { // 16 bitspc
				for (int i = 0, j = 1; i < imgInfo.samplesPerRowPacked; i++) {
					rowb[j++] = (byte) (row[i] >> 8);
					rowb[j++] = (byte) (row[i]);
				}
			}
		}
	}

	private void filterRow(final int rown) {
		// warning: filters operation rely on: "previos row" (rowbprev) is
		// initialized to 0 the first time
		if (filterStrat.shouldTestAll(rown)) {
			filterRowNone();
			reportResultsForFilter(rown, FilterType.FILTER_NONE, true);
			filterRowSub();
			reportResultsForFilter(rown, FilterType.FILTER_SUB, true);
			filterRowUp();
			reportResultsForFilter(rown, FilterType.FILTER_UP, true);
			filterRowAverage();
			reportResultsForFilter(rown, FilterType.FILTER_AVERAGE, true);
			filterRowPaeth();
			reportResultsForFilter(rown, FilterType.FILTER_PAETH, true);
		}
		final FilterType filterType = filterStrat.gimmeFilterType(rown, true);
		rowbfilter[0] = (byte) filterType.val;
		switch (filterType) {
		case FILTER_NONE:
			filterRowNone();
			break;
		case FILTER_SUB:
			filterRowSub();
			break;
		case FILTER_UP:
			filterRowUp();
			break;
		case FILTER_AVERAGE:
			filterRowAverage();
			break;
		case FILTER_PAETH:
			filterRowPaeth();
			break;
		default:
			throw new PngjUnsupportedException("Filter type " + filterType + " not implemented");
		}
		reportResultsForFilter(rown, filterType, false);
	}

	private void prepareEncodeRow(final int rown) {
		if (datStream == null)
			init();
		rowNum++;
		if (rown >= 0 && rowNum != rown)
			throw new PngjOutputException("rows must be written in order: expected:" + rowNum + " passed:" + rown);
		// swap
		final byte[] tmp = rowb;
		rowb = rowbprev;
		rowbprev = tmp;
	}

	private void filterAndSend(final int rown) {
		filterRow(rown);
		try {
			datStreamDeflated.write(rowbfilter, 0, imgInfo.bytesPerRow + 1);
		} catch (final IOException e) {
			throw new PngjOutputException(e);
		}
	}

	protected void filterRowAverage() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			rowbfilter[i] = (byte) (rowb[i] - ((rowbprev[i] & 0xFF) + (j > 0 ? (rowb[j] & 0xFF) : 0)) / 2);
		}
	}

	protected void filterRowNone() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			rowbfilter[i] = rowb[i];
		}
	}

	protected void filterRowPaeth() {
		int i, j, imax;
		imax = imgInfo.bytesPerRow;
		for (j = 1 - imgInfo.bytesPixel, i = 1; i <= imax; i++, j++) {
			// rowbfilter[i] = (byte) (rowb[i] - PngHelperInternal.filterPaethPredictor(j > 0 ? (rowb[j] & 0xFF) : 0,
			// rowbprev[i] & 0xFF, j > 0 ? (rowbprev[j] & 0xFF) : 0));
			rowbfilter[i] = (byte) PngHelperInternal.filterRowPaeth(rowb[i], j > 0 ? (rowb[j] & 0xFF) : 0,
					rowbprev[i] & 0xFF, j > 0 ? (rowbprev[j] & 0xFF) : 0);
		}
	}

	protected void filterRowSub() {
		int i, j;
		for (i = 1; i <= imgInfo.bytesPixel; i++)
			rowbfilter[i] = rowb[i];
		for (j = 1, i = imgInfo.bytesPixel + 1; i <= imgInfo.bytesPerRow; i++, j++) {
			// !!! rowbfilter[i] = (byte) (rowb[i] - rowb[j]);
			rowbfilter[i] = (byte) PngHelperInternal.filterRowSub(rowb[i], rowb[j]);
		}
	}

	protected void filterRowUp() {
		for (int i = 1; i <= imgInfo.bytesPerRow; i++) {
			// rowbfilter[i] = (byte) (rowb[i] - rowbprev[i]); !!!
			rowbfilter[i] = (byte) PngHelperInternal.filterRowUp(rowb[i], rowbprev[i]);
		}
	}

	protected int sumRowbfilter() { // sums absolute value
		int s = 0;
		for (int i = 1; i <= imgInfo.bytesPerRow; i++)
			if (rowbfilter[i] < 0)
				s -= rowbfilter[i];
			else
				s += rowbfilter[i];
		return s;
	}

	/**
	 * copy chunks from reader - copy_mask : see ChunksToWrite.COPY_XXX
	 * <p>
	 * If we are after idat, only considers those chunks after IDAT in PngReader
	 * <p>
	 * TODO: this should be more customizable
	 */
	private void copyChunks(final PngReader reader, final int copy_mask, final boolean onlyAfterIdat) {
		final boolean idatDone = currentChunkGroup >= ChunksList.CHUNK_GROUP_4_IDAT;
		if (onlyAfterIdat && reader.getCurrentChunkGroup() < ChunksList.CHUNK_GROUP_6_END)
			throw new PngjExceptionInternal("tried to copy last chunks but reader has not ended");
		for (final PngChunk chunk : reader.getChunksList().getChunks()) {
			final int group = chunk.getChunkGroup();
			if (group < ChunksList.CHUNK_GROUP_4_IDAT && idatDone)
				continue;
			boolean copy = false;
			if (chunk.crit) {
				if (chunk.id.equals(ChunkHelper.PLTE)) {
					if (imgInfo.indexed && ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_PALETTE))
						copy = true;
					if (!imgInfo.greyscale && ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_ALL))
						copy = true;
				}
			} else { // ancillary
				final boolean text = (chunk instanceof PngChunkTextVar);
				final boolean safe = chunk.safe;
				// notice that these if are not exclusive
				if (ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_ALL))
					copy = true;
				if (safe && ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_ALL_SAFE))
					copy = true;
				if (chunk.id.equals(ChunkHelper.tRNS)
						&& ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_TRANSPARENCY))
					copy = true;
				if (chunk.id.equals(ChunkHelper.pHYs) && ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_PHYS))
					copy = true;
				if (text && ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_TEXTUAL))
					copy = true;
				if (ChunkHelper.maskMatch(copy_mask, ChunkCopyBehaviour.COPY_ALMOSTALL)
						&& !(ChunkHelper.isUnknown(chunk) || text || chunk.id.equals(ChunkHelper.hIST) || chunk.id
								.equals(ChunkHelper.tIME)))
					copy = true;
				if (chunk instanceof PngChunkSkipped)
					copy = false;
			}
			if (copy) {
				chunksList.queue(PngChunk.cloneChunk(chunk, imgInfo));
			}
		}
	}

	/**
	 * Copies first (pre IDAT) ancillary chunks from a PngReader.
	 * <p>
	 * Should be called when creating an image from another, before starting
	 * writing lines, to copy relevant chunks.
	 * <p>
	 *
	 * @param reader
	 *            : PngReader object, already opened.
	 * @param copy_mask
	 *            : Mask bit (OR), see <code>ChunksToWrite.COPY_XXX</code>
	 *            constants
	 */
	public void copyChunksFirst(final PngReader reader, final int copy_mask) {
		copyChunks(reader, copy_mask, false);
	}

	/**
	 * Copies last (post IDAT) ancillary chunks from a PngReader.
	 * <p>
	 * Should be called when creating an image from another, after writing all
	 * lines, before closing the writer, to copy additional chunks.
	 * <p>
	 *
	 * @param reader
	 *            : PngReader object, already opened and fully read.
	 * @param copy_mask
	 *            : Mask bit (OR), see <code>ChunksToWrite.COPY_XXX</code>
	 *            constants
	 */
	public void copyChunksLast(final PngReader reader, final int copy_mask) {
		copyChunks(reader, copy_mask, true);
	}

	/**
	 * Computes compressed size/raw size, approximate.
	 * <p>
	 * Actually: compressed size = total size of IDAT data , raw size =
	 * uncompressed pixel bytes = rows * (bytesPerRow + 1).
	 *
	 * This must be called after pngw.end()
	 */
	public double computeCompressionRatio() {
		if (currentChunkGroup < ChunksList.CHUNK_GROUP_6_END)
			throw new PngjOutputException("must be called after end()");
		final double compressed = datStream.getCountFlushed();
		final double raw = (imgInfo.bytesPerRow + 1) * imgInfo.rows;
		return compressed / raw;
	}

	/**
	 * Finalizes the image creation and closes the stream. This MUST be called
	 * after writing the lines.
	 */
	public void end() {
		if (rowNum != imgInfo.rows - 1)
			throw new PngjOutputException("all rows have not been written");
		try {
			datStreamDeflated.finish();
			datStream.flush();
			writeLastChunks();
			writeEndChunk();
			if (shouldCloseStream)
				os.close();
		} catch (final IOException e) {
			throw new PngjOutputException(e);
		}
	}

	/**
	 * returns the chunks list (queued and writen chunks)
	 */
	public ChunksListForWrite getChunksList() {
		return chunksList;
	}

	/**
	 * Filename or description, from the optional constructor argument.
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * High level wrapper over chunksList for metadata handling
	 */
	public PngMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Sets compression level of ZIP algorithm.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setFilterType()
	 *
	 * @param compLevel
	 *            between 0 and 9 (default:6 , recommended: 6 or more)
	 */
	public void setCompLevel(final int compLevel) {
		if (compLevel < 0 || compLevel > 9)
			throw new PngjOutputException("Compression level invalid (" + compLevel + ") Must be 0..9");
		this.compLevel = compLevel;
	}

	/**
	 * Sets internal prediction filter type, or strategy to choose it.
	 * <p>
	 * This must be called just after constructor, before starting writing.
	 * <p>
	 * See also setCompLevel()
	 *
	 * @param filterType
	 *            One of the five prediction types or strategy to choose it (see
	 *            <code>PngFilterType</code>) Recommended values: DEFAULT
	 *            (default) or AGGRESIVE
	 */
	public void setFilterType(final FilterType filterType) {
		filterStrat = new FilterWriteStrategy(imgInfo, filterType);
	}

	/**
	 * Sets maximum size of IDAT fragments. This has little effect on
	 * performance you should rarely call this
	 * <p>
	 *
	 * @param idatMaxSize
	 *            default=0 : use defaultSize (32K)
	 */
	public void setIdatMaxSize(final int idatMaxSize) {
		this.idatMaxSize = idatMaxSize;
	}

	/**
	 * if true, input stream will be closed after ending write
	 * <p>
	 * default=true
	 */
	public void setShouldCloseStream(final boolean shouldCloseStream) {
		this.shouldCloseStream = shouldCloseStream;
	}

	/**
	 * Deflater strategy: one of Deflater.FILTERED Deflater.HUFFMAN_ONLY
	 * Deflater.DEFAULT_STRATEGY
	 * <p>
	 * Default: Deflater.FILTERED . This should be changed very rarely.
	 */
	public void setDeflaterStrategy(final int deflaterStrategy) {
		this.deflaterStrategy = deflaterStrategy;
	}

	/**
	 * Writes line, checks that the row number is consistent with that of the
	 * ImageLine See writeRow(int[] newrow, int rown)
	 *
	 * @deprecated Better use writeRow(ImageLine imgline, int rownumber)
	 */
	public void writeRow(final ImageLine imgline) {
		writeRow(imgline.scanline, imgline.getRown());
	}

	/**
	 * Writes line. See writeRow(int[] newrow, int rown)
	 *
	 * The <tt>packed</tt> flag of the imageline is honoured!
	 *
	 * @see #writeRowInt(int[], int)
	 */
	public void writeRow(final ImageLine imgline, final int rownumber) {
		unpackedMode = imgline.samplesUnpacked;
		if (imgline.sampleType == SampleType.INT)
			writeRowInt(imgline.scanline, rownumber);
		else
			writeRowByte(imgline.scanlineb, rownumber);
	}

	/**
	 * Same as writeRow(int[] newrow, int rown), but does not check row number
	 *
	 * @param newrow
	 */
	public void writeRow(final int[] newrow) {
		writeRow(newrow, -1);
	}

	/**
	 * Alias to writeRowInt
	 *
	 * @see #writeRowInt(int[], int)
	 */
	public void writeRow(final int[] newrow, final int rown) {
		writeRowInt(newrow, rown);
	}

	/**
	 * Writes a full image row.
	 * <p>
	 * This must be called sequentially from n=0 to n=rows-1 One integer per
	 * sample , in the natural order: R G B R G B ... (or R G B A R G B A... if
	 * has alpha) The values should be between 0 and 255 for 8 bitspc images,
	 * and between 0- 65535 form 16 bitspc images (this applies also to the
	 * alpha channel if present) The array can be reused.
	 * <p>
	 * Warning: the array might be modified in some cases (unpacked row with low
	 * bitdepth)
	 * <p>
	 *
	 * @param newrow
	 *            Array of pixel values. Warning: the array size should be exact
	 *            (samplesPerRowP)
	 * @param rown
	 *            Row number, from 0 (top) to rows-1 (bottom). This is just used
	 *            as a check. Pass -1 if you want to autocompute it
	 */
	public void writeRowInt(final int[] newrow, final int rown) {
		prepareEncodeRow(rown);
		encodeRowFromInt(newrow);
		filterAndSend(rown);
	}

	/**
	 * Same semantics as writeRowInt but using bytes. Each byte is still a
	 * sample. If 16bitdepth, we are passing only the most significant byte (and
	 * hence losing some info)
	 *
	 * @see PngWriter#writeRowInt(int[], int)
	 */
	public void writeRowByte(final byte[] newrow, final int rown) {
		prepareEncodeRow(rown);
		encodeRowFromByte(newrow);
		filterAndSend(rown);
	}

	/**
	 * Writes all the pixels, calling writeRowInt() for each image row
	 */
	public void writeRowsInt(final int[][] image) {
		for (int i = 0; i < imgInfo.rows; i++)
			writeRowInt(image[i], i);
	}

	/**
	 * Writes all the pixels, calling writeRowByte() for each image row
	 */
	public void writeRowsByte(final byte[][] image) {
		for (int i = 0; i < imgInfo.rows; i++)
			writeRowByte(image[i], i);
	}

	public boolean isUnpackedMode() {
		return unpackedMode;
	}

	/**
	 * If false (default), and image has bitdepth 1-2-4, the scanlines passed
	 * are assumed to be already packed.
	 * <p>
	 * If true, each element is a sample, the writer will perform the packing if
	 * necessary.
	 * <p>
	 * Warning: when using {@link #writeRow(ImageLine, int)} (recommended) the
	 * <tt>packed</tt> flag of the ImageLine object overrides (and overwrites!)
	 * this field.
	 */
	public void setUseUnPackedMode(final boolean useUnpackedMode) {
		this.unpackedMode = useUnpackedMode;
	}

}
