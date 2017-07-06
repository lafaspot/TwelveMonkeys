package com.twelvemonkeys.imageio.metadata.exif;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import org.junit.Test;

import com.twelvemonkeys.imageio.metadata.CompoundDirectory;
import com.twelvemonkeys.imageio.metadata.Directory;
import com.twelvemonkeys.imageio.metadata.MetadataReaderAbstractTest;
import com.twelvemonkeys.imageio.metadata.tiff.TIFF;
import com.twelvemonkeys.imageio.stream.SubImageInputStream;

/**
 * EXIFReaderTest for reading only the first directory.
 *
 * @author <a href="mailto:harald.kuhr@gmail.com">Harald Kuhr</a>
 * @author last modified by $Author: haraldk$
 * @version $Id: EXIFReaderTest.java,v 1.0 23.12.11 13:50 haraldk Exp$
 */
@SuppressWarnings("deprecation")
public class EXIFReaderOneDirectoryTest extends MetadataReaderAbstractTest {
    @Override
    protected InputStream getData() throws IOException {
        return getResource("/exif/exif-jpeg-segment.bin").openStream();
    }

    @Override
    protected EXIFReader createReader() {
        return new EXIFReader();
    }

    @Test
    public void testIsCompoundDirectory() throws IOException {
        Directory exif = createReader().read(getDataAsIIS());
        assertThat(exif, instanceOf(CompoundDirectory.class));
    }

    @Test
    public void testDirectory() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        assertEquals(1, exif.directoryCount());
        assertNotNull(exif.getDirectory(0));
        assertEquals(exif.size(), exif.getDirectory(0).size());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testDirectoryOutOfBounds() throws IOException {
        InputStream data = getData();

        CompoundDirectory exif = (CompoundDirectory) createReader().read(ImageIO.createImageInputStream(data));

        assertEquals(1, exif.directoryCount());
        assertNotNull(exif.getDirectory(exif.directoryCount()));
    }

    @Test
    public void testEntries() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        // From IFD0
        assertNotNull(exif.getEntryById(TIFF.TAG_SOFTWARE));
        assertEquals("Adobe Photoshop CS2 Macintosh", exif.getEntryById(TIFF.TAG_SOFTWARE).getValue());
        assertEquals(exif.getEntryById(TIFF.TAG_SOFTWARE), exif.getEntryByFieldName("Software"));
    }

    @Test
    public void testIFD0() throws IOException {
        CompoundDirectory exif = (CompoundDirectory) createReader().read(getDataAsIIS());

        Directory ifd0 = exif.getDirectory(0);
        assertNotNull(ifd0);

        assertNotNull(ifd0.getEntryById(TIFF.TAG_IMAGE_WIDTH));
        assertEquals(3601, ifd0.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());

        assertNotNull(ifd0.getEntryById(TIFF.TAG_IMAGE_HEIGHT));
        assertEquals(4176, ifd0.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue());

        // Assert 'uncompressed' (there's no TIFF image here, really)
        assertNotNull(ifd0.getEntryById(TIFF.TAG_COMPRESSION));
        assertEquals(1, ifd0.getEntryById(TIFF.TAG_COMPRESSION).getValue());
    }

    @Test
    public void testReadBadDataZeroCount() throws IOException {
        // This image seems to contain bad Exif. But as other tools are able to read, so should we..
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg"));
        stream.seek(12);
        Directory directory = createReader().read(new SubImageInputStream(stream, 21674));

        assertEquals(10, directory.size());

        // Special case: Ascii string with count == 0, not ok according to spec (?), but we'll let it pass
        assertEquals("", directory.getEntryById(TIFF.TAG_IMAGE_DESCRIPTION).getValue());
    }

    @Test
    public void testReadBadDirectoryCount() throws IOException {
        // This image seems to contain bad Exif. But as other tools are able to read, so should we..
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-bad-directory-entry-count.jpg"));
        stream.seek(4424 + 10);

        Directory directory = createReader().read(new SubImageInputStream(stream, 214 - 6));
        assertEquals(7, directory.size()); // TIFF structure says 8, but the last entry isn't there
    }

    @Test
    public void testTIFFWithBadExifIFD() throws IOException {
        // This image seems to contain bad TIFF data. But as other tools are able to read, so should we..
        // It seems that the EXIF data (at offset 494196 or 0x78a74) overlaps with a custom
        // Microsoft 'OLE Property Set' entry at 0x78a70 (UNDEFINED, count 5632)...
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/tiff/chifley_logo.tif"));
        Directory directory = createReader().read(stream);
        assertEquals(22, directory.size());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirR98() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-R98.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 1360));
        assertEquals(11, directory.size());
        assertEquals(1, directory.directoryCount());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirEmpty() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-empty.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 1360));
        assertEquals(11, directory.size());
        assertEquals(1, directory.directoryCount());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirEOF() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-eof.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 236));
        assertEquals(8, directory.size());
        assertEquals(1, directory.directoryCount());
    }

    @Test
    public void testReadExifJPEGWithInteropSubDirBad() throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(getResource("/jpeg/exif-with-interop-subdir-bad.jpg"));
        stream.seek(30);

        CompoundDirectory directory = (CompoundDirectory) createReader().read(new SubImageInputStream(stream, 12185));
        assertEquals(9, directory.size());
        assertEquals(1, directory.directoryCount());
    }

    @Test
    public void testReadExifWithMissingEOFMarker() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/noeof.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(15, directory.size());
            assertEquals(1, directory.directoryCount());
        }
    }

    @Test
    public void testReadExifWithEmptyTag() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/emptyexiftag.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(1, directory.directoryCount());
        }
    }

    @Test
    public void testReadValueBeyondEOF() throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(getResource("/exif/value-beyond-eof.tif"))) {
            CompoundDirectory directory = (CompoundDirectory) createReader().read(stream);
            assertEquals(1, directory.directoryCount());
            assertEquals(5, directory.size());

            assertEquals(1, directory.getEntryById(TIFF.TAG_PHOTOMETRIC_INTERPRETATION).getValue());
            assertEquals(10, directory.getEntryById(TIFF.TAG_IMAGE_WIDTH).getValue());
            assertEquals(10, directory.getEntryById(TIFF.TAG_IMAGE_HEIGHT).getValue());
            assertEquals(42L, directory.getEntryById(32935).getValue());
            // NOTE: Assumes current implementation, could possibly change in the future.
            assertTrue(directory.getEntryById(32934).getValue() instanceof EOFException);
        }
    }
}
