package util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Minimal utility to write a single-page PDF containing a single image.
 * This avoids external dependencies like iText by constructing a tiny PDF
 * that embeds a JPEG image stream (DCTDecode). It supports typical
 * use-cases for screenshots/receipts used by the application.
 *
 * Limitations:
 *  - Only writes one image on a single page sized to the image dimensions (points).
 *  - Uses JPEG encoding; transparent pixels will be flattened against white.
 */
public final class PdfUtil {

    private PdfUtil() {}

    /**
     * Writes the provided BufferedImage to the given path as a one-page PDF.
     * The image is encoded as JPEG internally.
     */
    public static void writeImageAsPdf(BufferedImage image, File outFile) throws IOException {
        if (image == null) throw new IllegalArgumentException("image must not be null");
        if (outFile == null) throw new IllegalArgumentException("outFile must not be null");

        // Encode image to JPEG bytes
        ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
        // If image has alpha, draw onto white background to avoid black background in JPEG
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D g = rgb.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();

        if (!ImageIO.write(rgb, "jpg", imgBaos)) {
            throw new IOException("Failed to encode image as JPEG");
        }
        imgBaos.flush();
        byte[] imgBytes = imgBaos.toByteArray();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            writePdfWithJpeg(fos, imgBytes, image.getWidth(), image.getHeight());
        }
    }

    // Write a minimal PDF that embeds a JPEG image stream (DCTDecode)
    private static void writePdfWithJpeg(OutputStream out, byte[] jpegData, int pxWidth, int pxHeight) throws IOException {
        // PDF uses points (1/72 inch). We'll treat 1 pixel ~= 1 point to preserve layout size.
        // This is acceptable for simple receipts/screenshots.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, "ISO-8859-1"));

        // helper to track object positions
        long[] objOffsets = new long[5];

        // Header
        pw.println("%PDF-1.4");
        pw.flush();

        // obj 1: Catalog
        objOffsets[0] = baos.size();
        pw.println("1 0 obj");
        pw.println("<< /Type /Catalog /Pages 2 0 R >>");
        pw.println("endobj");
        pw.flush();

        // obj 2: Pages
        objOffsets[1] = baos.size();
        pw.println("2 0 obj");
        pw.println("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        pw.println("endobj");
        pw.flush();

        // obj 3: Page
        objOffsets[2] = baos.size();
        pw.println("3 0 obj");
        pw.println("<< /Type /Page /Parent 2 0 R /Resources << /XObject << /Im0 4 0 R >> /ProcSet [/PDF /ImageC] >> /MediaBox [0 0 " + pxWidth + " " + pxHeight + "] /Contents 5 0 R >>");
        pw.println("endobj");
        pw.flush();

        // obj 4: Image XObject (stream will be written as binary)
        objOffsets[3] = baos.size();
        pw.println("4 0 obj");
        pw.println("<< /Type /XObject /Subtype /Image /Width " + pxWidth + " /Height " + pxHeight + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + jpegData.length + " >>");
        pw.println("stream");
        pw.flush();
        baos.write(jpegData);
        baos.write('\n');
        pw.println("endstream");
        pw.println("endobj");
        pw.flush();

        // obj 5: Contents (draw the image to fill the page)
        String contents = "q\n" + pxWidth + " 0 0 " + pxHeight + " 0 0 cm\n/Im0 Do\nQ\n";
        byte[] contentsBytes = contents.getBytes("ISO-8859-1");
        objOffsets[4] = baos.size();
        pw.println("5 0 obj");
        pw.println("<< /Length " + contentsBytes.length + " >>");
        pw.println("stream");
        pw.flush();
        baos.write(contentsBytes);
        pw.println();
        pw.println("endstream");
        pw.println("endobj");
        pw.flush();

        // xref
        long xrefPos = baos.size();
        pw.println("xref");
        pw.println("0 6");
        pw.println("0000000000 65535 f ");
        for (int i = 0; i < objOffsets.length; i++) {
            pw.printf("%010d 00000 n \n", objOffsets[i]);
        }
        pw.flush();

        // trailer
        pw.println("trailer");
        pw.println("<< /Size 6 /Root 1 0 R >>");
        pw.println("startxref");
        pw.println(xrefPos);
        pw.println("%%EOF");
        pw.flush();

        // write to out
        baos.writeTo(out);
        out.flush();
    }
}
