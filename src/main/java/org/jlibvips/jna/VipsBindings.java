package org.jlibvips.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface VipsBindings extends Library {

    VipsBindings INSTANCE = Native.load("libvips.42.dylib", VipsBindings.class);

    int vips_init(String argv0);

    Pointer vips_image_new_from_file(String fileName, Object...args);

    int vips_pdfload(String fileName, Pointer[] pointer, Object...args);

    int vips_image_get_width(Pointer pointer);
    int vips_image_get_height(Pointer pointer);
    int vips_image_get_n_pages(Pointer pointer);

    int vips_image_get_bands(Pointer pointer);

    int vips_grid(Pointer in, Pointer[] out, int tile_height, int across, int down, Object...args);

    int vips_dzsave(Pointer pointer, String outDir, Object...args);

    int vips_thumbnail_image(Pointer in, Pointer[] out, int width, Object...args);

    int vips_extract_area(Pointer in, Pointer[] out, int left, int top, int width, int height, Object...args);

    Pointer vips_array_double_new(double[] array, int n);

    int vips_vipssave(Pointer in, String filename, Object...args);
    int vips_jpegsave(Pointer in, String filename, Object...args);
    int vips_pngsave(Pointer in, String filename, Object...args);
    int vips_webpsave(Pointer in, String filename, Object...args);

    int vips_insert(Pointer main, Pointer sub, Pointer[] out, int x, int y, Object...args);
    int vips_join(Pointer in1, Pointer in2, Pointer[] out, int direction, Object...args);

    int vips_draw_rect1(Pointer image, double ink, int left, int top, int width, int height, Object...args);
    int vips_draw_rect(Pointer image, Pointer ink, int n, int left, int top, int width, int height, Object...args);
}
