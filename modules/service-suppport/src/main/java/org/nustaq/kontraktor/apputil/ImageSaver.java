package org.nustaq.kontraktor.apputil;

import org.imgscalr.Scalr;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

public class ImageSaver {
    // 0..1
    static float DEFAULT_IMAGE_COMPRESSION_VALUE = 0.5f;

    String basePath;

    public ImageSaver(String basePath) {
        this.basePath = basePath;
    }

    public IPromise<String> handleImage(String base64Image, String imgType, String path, Dimension targetSize) {
        IPromise<String> prom = new Promise<>();
        Base64.Decoder decoder = Base64.getDecoder();
        int i = base64Image.indexOf(";base64");
        if (i < 0) {
            prom.reject("Not a Base64 Image");
            return prom;
        }
        String str = base64Image.substring(i + ";base64".length() + 1);
        byte[] decodedBytes;
        try {
            decodedBytes = decoder.decode(str);
            BufferedImage img = scalePreservingAspectRatioAsImage(decodedBytes, targetSize.width, targetSize.height);
            if (img == null) {
                prom.complete("Error scaling image...", null);
                return prom;
            }
            String fileEnding = validateType(imgType);
            String fileName = UUID.randomUUID().toString() + "." + fileEnding;
            String fullPath = basePath + "/" + fileName;
            boolean success = saveImage(img, fullPath);
            if (success) {
                String imageId = fileName;
                prom.complete(imageId, null);
                return prom;
            } else {
                prom.complete(null, "Image scaling incomplete ..");
                return prom;
            }
        } catch (Exception exc) {
            Log.Error(this, exc);
            prom.reject(exc.getMessage());
        }
        return prom;
    }

    protected String imageEndingByMimeType(String mimeType) {
        switch (mimeType) {
            case "image/cis-cod":
                return "cod"; //CIS-Cod-Dateien
            case "image/cmu-raster":
                return "ras"; //	CMU-Raster-Dateien
            case "image/fif":
                return "fif"; //	FIF-Dateien
            case "image/gif":
                return "gif"; //	GIF-Dateien
            case "image/ief":
                return "ief"; //	IEF-Dateien
            case "image/png":
                return "png"; //	PNG-Dateien
            case "image/tiff":
                return "tif"; //	TIFF-Dateien
            case "image/vasa":
                return "mcf"; //	Vasa-Dateien
            case "image/vnd.wap.wbmp":
                return "wbmp"; //	Bitmap-Dateien (WAP)
            case "image/jpeg":
            case "image/jpg":
                return "jpg"; //	JPEG-Dateien
            case "image/x-freehand":
                return "fhc"; //	Freehand-Dateien
            case "image/x-icon":
                return "ico"; //	Icon-Dateien (z.B. Favoriten-Icons)
            case "image/x-portable-anymap":
                return "pnm"; //	PBM Anymap Dateien
            case "image/x-portable-bitmap":
                return "pbm"; //	PBM Bitmap Dateien
            case "image/x-portable-graymap":
                return "pgm"; //	PBM Graymap Dateien
            case "image/x-portable-pixmap":
                return "ppm"; //	PBM Pixmap Dateien
            case "image/x-rgb":
                return "rgb"; //	RGB-Dateien
            case "image/x-windowdump":
                return "xwd"; //	X-Windows Dump
            case "image/x-xbitmap":
                return "xbm"; //	XBM-Dateien
            case "image/x-xpixmap":
                return "xpm"; //
            default:
                return "jpg";
        }
    }

    protected String validateType(String type) {
        if (type == null) return "jpg";
        type = type.toLowerCase();
        if (type.indexOf("/") >= 0) // its a mime type..
        {
            return imageEndingByMimeType(type);
        }
        return "jpg";
    }

    protected boolean saveImage(BufferedImage img, String fileName) {
        try {
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
            File sized = new File(fileName);
            sized.mkdirs();
            try {
                ImageWriter writer = ImageIO.getImageWritersByFormatName(ext).next();
                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    if (writeParam.canWriteProgressive()) {
                        writeParam.setProgressiveMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
                    }
                    // jdk bug forces change the color model from cymk to rgb..
                    img = convertCMYK2RGB(img);
                    // FIXME: Gif compression needs a compression type ..
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    if (!fileName.endsWith("gif")) {
                        writeParam.setCompressionQuality(DEFAULT_IMAGE_COMPRESSION_VALUE);
                    }
                }
            } catch (Exception ex) {
                Log.Error(this,ex);
            }
            ImageIO.write(img, ext, sized);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected BufferedImage convertCMYK2RGB(BufferedImage image) throws IOException {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        ColorConvertOp op = new ColorConvertOp(null);
        op.filter(image, rgbImage);
        return rgbImage;
    }

    protected BufferedImage bimgFromBytez(byte[] source) throws IOException {
        try (InputStream inputStream = new ByteArrayInputStream(source)) {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            if (bufferedImage != null) {
                return bufferedImage;
            } else {
                throw new IllegalArgumentException("No ImageReader could read the provided data");
            }
        }
    }

    public BufferedImage scalePreservingAspectRatioAsImage(byte[] source, int targetW, int targetH) throws IOException {
        BufferedImage bufferedImage = bimgFromBytez(source);
        return scalePreservingAspectRatioAsImage(bufferedImage, targetW, targetH);
    }

    public BufferedImage scalePreservingAspectRatioAsImage(BufferedImage bimg, int targetW, int targetH) {
        double width = bimg.getWidth(); // Double on purpose for division
        double height = bimg.getHeight(); // Double on purpose for division
        Scalr.Mode mode = (targetW / width >= targetH / height) ? Scalr.Mode.FIT_TO_WIDTH : Scalr.Mode.FIT_TO_HEIGHT;
        bimg = Scalr.resize(bimg, Scalr.Method.QUALITY, mode, targetW, targetH);
        int scaledImageW = bimg.getWidth();
        int scaledImageH = bimg.getHeight();
        if (scaledImageH != targetH || scaledImageW != targetW) {
            if (scaledImageH > scaledImageW) {
                int startingW = (scaledImageW - targetW) / 2;
                int startingH = 0;
                bimg = bimg.getSubimage(startingW, startingH, targetW, targetH);
            } else {
                // sonst center
                int startingW = (scaledImageW - targetW) / 2;
                int startingH = (scaledImageH - targetH) / 2;
                bimg = bimg.getSubimage(startingW, startingH, targetW, targetH);
            }
        }
        return bimg;
    }

}
