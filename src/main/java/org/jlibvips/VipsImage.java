package org.jlibvips;

import org.jlibvips.exceptions.CouldNotLoadPdfVipsException;
import org.jlibvips.jna.VipsBindings;
import org.jlibvips.operations.*;
import com.sun.jna.Pointer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * An image, residing in memory or disk, managed by libvips. Instance methods provide transformations and queries on
 * it.
 *
 * When loading simple images from the file system, call {@link VipsImage#fromFile(Path)}.
 *
 * For large vectorised PDFs we recommend {@link VipsImage#fromPdf(Path)}.
 *
 * @author amp
 */
public class VipsImage {

    /**
     * This is the maximum resolution to which libvips can convert vectorised PDFs.
     *
     * libvips builds on libpoppler which uses cairographics to draw the vectors on a canvas. cairos canvas is limited
     * by 32767x32767 rows and therefore PDF pages loaded as {@link VipsImage} cannot be larger than that.
     *
     * @see <a href="https://poppler.freedesktop.org/">libpoppler</a>
     * @see <a href="https://www.cairographics.org/">cairo</a>
     * @see <a href="https://gitlab.freedesktop.org/poppler/poppler/issues/511">Gitlab Issue</a>
     */
    public static final int POPPLER_CAIRO_LIMIT = 32767;

    /**
     * Loads a PDF document's page as {@link VipsImage} in the greatest possible resolution.
     *
     * @param p {@link Path} to the PDF document.
     * @param page page number (starting at 0)
     * @return the PDF page as {@link VipsImage}.
     */
    public static VipsImage fromPdf(Path p, int page) {
        // Due to excessive testing we set 6 to be the maximum scale parameter and decrease by 0.1 until we reach a
        // scale working with the limit.
        var scale = 6.0f;
        VipsImage image;
        do {
            Pointer[] ptr = new Pointer[1];
            var ret = VipsBindings.INSTANCE.vips_pdfload(p.toString(), ptr, "scale", scale, "page", page, null);
            if(ret != 0) {
                throw new CouldNotLoadPdfVipsException(ret);
            }
            image = new VipsImage(ptr[0]);
            scale -= 0.1f;
        } while(image.getWidth() > POPPLER_CAIRO_LIMIT || image.getHeight() > POPPLER_CAIRO_LIMIT);
        return image;
    }

    public static List<VipsImage> getPagesFromPdf(Path p, int MAX_PAGES){
        List<VipsImage> pages = new ArrayList<>();
        try {
            VipsImage first = fromPdf(p, 0);
            pages.add(first);

            for (int i = 1; i < Math.min(first.getPages(), MAX_PAGES); i++) {
                VipsImage page = fromPdf(p, i);
                pages.add(page);
            }
        }
        catch (Exception exception) {
            throw new CouldNotLoadPdfVipsException(-1);
        }
        return pages;
    }

    public static int init(){
        return VipsBindings.INSTANCE.vips_init("application");
    }

    /**
     * Loads a PDF document's page as {@link VipsImage} in the greatest possible resolution.
     *
     * @param p {@link Path} to the PDF document.
     * @return the PDF page as {@link VipsImage}.
     */
    public static VipsImage fromPdf(Path p) {
        return fromPdf(p, 1);
    }

    /**
     * Creates a new {@link VipsImage} from a {@link Path} to an image or PDF file.
     *
     * @param p {@link Path} to image file.
     * @return {@link VipsImage} representation of file.
     * @see <a href="http://libvips.github.io/libvips/API/current/VipsImage.html#vips-image-new-from-file">Native Function</a>
     */
    public static VipsImage fromFile(Path p) {
        var ptr = VipsBindings.INSTANCE.vips_image_new_from_file(p.toString());
        return new VipsImage(ptr);
    }

    private final Pointer ptr;

    /**
     * Initializes a new {@link VipsImage} with a pointer to a native <code>VipsImage</code> struct.
     *
     * @param ptr {@link Pointer} to <code>VipsImage</code> struct
     * @see <a href="http://libvips.github.io/libvips/API/current/VipsImage.html">VipsImage struct</a>
     */
    public VipsImage(final Pointer ptr) {
        this.ptr = ptr;
    }

    /**
     * Get the pointer to the <code>VipsImage</code> struct.
     *
     * @return {@link Pointer} to <code>VipsImage</code> struct
     * @see <a href="http://libvips.github.io/libvips/API/current/VipsImage.html">VipsImage struct</a>
     */
    public Pointer getPtr() {
        return ptr;
    }

    /**
     * Save an image as a set of tiles at various resolutions.
     *
     * <code>
     *     image.deepZoom(outDir)
     *      .layout(DeepZoomLayout.Google)
     *      .save()
     * </code>
     *
     * @param outDir {@link Path} to the output directory.
     * @return the {@link DeepZoomOperation}
     * @see <a href="http://libvips.github.io/libvips/API/current/VipsForeignSave.html#vips-dzsave">vips_dzsave()</a>
     */
    public DeepZoomOperation deepZoom(Path outDir) {
        return new DeepZoomOperation(this, outDir);
    }

    /**
     * Make a thumbnail of this {@link VipsImage}.
     *
     * <code>
     *     var thumbnail = image.thumbnail(100)
     *      .autoRotate()
     *      .create();
     * </code>
     *
     * @param width Fixed width in pixel of the new {@link VipsImage}
     * @return the {@link ThumbnailOperation}
     * @see <a href="http://libvips.github.io/libvips/API/current/libvips-resample.html#vips-thumbnail">vips_thumbnail()</a>
     * @see <a href="http://libvips.github.io/libvips/API/current/libvips-resample.html#vips-thumbnail-image">vips_thumbnail_image()</a>
     */
    public ThumbnailOperation thumbnail(int width) {
        return new ThumbnailOperation(this, width);
    }

    /**
     * Write an image to a file in Vips Image format.
     *
     * <code>
     *     java.nio.Path = image.v().save();
     * </code>
     *
     * <a href="http://libvips.github.io/libvips/API/current/VipsForeignSave.html#vips-vipssave">vips_vipssave</a>
     *
     * @return the {@link VipsSaveOperation}
     */
    public VipsSaveOperation v() {
        return new VipsSaveOperation(this);
    }

    /**
     * Write an image to a file in WebP format.
     *
     * <code>
     *     java.nio.Path path = image.webp().quality(100).save();
     * </code>
     *
     * <a href="http://libvips.github.io/libvips/API/current/VipsForeignSave.html#vips-webpsave">vips_webpsave()</a>
     *
     * @return the {@link WebpSaveOperation}
     */
    public WebpSaveOperation webp() {
        return new WebpSaveOperation(this);
    }

    /**
     * Write an image to a file in JPEG format.
     *
     * <code>
     *     java.nio.Path path = image.jpeg().quality(100).save();
     * </code>
     *
     * <a href="http://libvips.github.io/libvips/API/current/VipsForeignSave.html#vips-jpegsave">vips_jpegsave()</a>
     *
     * @return the {@link JpegSaveOperation}
     */
    public JpegSaveOperation jpeg() {
        return new JpegSaveOperation(this);
    }

    /**
     * Get the width of this image.
     *
     * @return width in pixel
     * @see <a href="http://libvips.github.io/libvips/API/current/libvips-header.html#vips-image-get-width">vips_image_get_width()</a>
     */
    public int getWidth() {
        return VipsBindings.INSTANCE.vips_image_get_width(ptr);
    }

    public int getPages() {
        return VipsBindings.INSTANCE.vips_image_get_n_pages(ptr);
    }

    /**
     * Get the height of this image.
     *
     * @return height in pixel
     * @see <a href="http://libvips.github.io/libvips/API/current/libvips-header.html#vips-image-get-height">vips_image_get_height()</a>
     */
    public int getHeight() {
        return VipsBindings.INSTANCE.vips_image_get_height(ptr);
    }

    /**
     * Get the bands of this image.
     *
     * @return number of bands
     * @see <a href="http://libvips.github.io/libvips/API/current/libvips-header.html#vips-image-get-bands">http://libvips.github.io/libvips/API/current/libvips-header.html#vips-image-get-bands()</a>
     */
    public int getBands() {
        return VipsBindings.INSTANCE.vips_image_get_bands(ptr);
    }

    public VipsImage getGrids() {
        var out = new Pointer[getPages()];
        VipsBindings.INSTANCE.vips_grid(this.ptr, out, getHeight()/getPages(), getPages(), 1, null);
        return new VipsImage(out[0]);
    }
    /**
     * Insert sub into main at position.
     *
     * <a href="https://jcupitt.github.io/libvips/API/current/libvips-conversion.html#vips-insert">https://jcupitt.github.io/libvips/API/current/libvips-conversion.html#vips-insert</a>
     *
     * @param sub small image
     * @return the {@link VipsInsertOperation}
     */
    public VipsInsertOperation insert(VipsImage sub) {
        return new VipsInsertOperation(this, sub);
    }

    public VipsJoinOperation join(VipsImage other) {
        return new VipsJoinOperation(this, other);
    }

    /**
     * Paint pixels within left , top , width , height in image with ink . If fill is zero, just paint a 1-pixel-wide
     * outline.
     *
     * <a href="https://jcupitt.github.io/libvips/API/current/libvips-draw.html#vips-draw-rect">vips_draw_rect()</a>
     *
     * @return the {@link DrawRectOperation}
     */
    public DrawRectOperation rect() {
        return new DrawRectOperation(this);
    }
}
