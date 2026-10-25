package vn.lighttv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

final class Backgrounds {
    private static final String FILE_NAME = "background.jpg";
    private static final int MAX_EDGE = 1920;

    private Backgrounds() {
    }

    static File file(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    static Bitmap load(Context context) {
        File file = file(context);
        if (!file.exists() || file.length() < 32) {
            return null;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    static boolean save(Context context, byte[] data, int length) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return false;
        }
        Bitmap decoded;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
            decoded = BitmapFactory.decodeByteArray(data, 0, length, options);
        } catch (OutOfMemoryError ignored) {
            return false;
        }
        if (decoded == null) {
            return false;
        }
        Bitmap output = decoded;
        FileOutputStream stream = null;
        try {
            output = scaleDown(decoded);
            stream = new FileOutputStream(file(context));
            boolean wrote = output.compress(Bitmap.CompressFormat.JPEG, 86, stream);
            stream.flush();
            return wrote;
        } catch (Exception ignored) {
            return false;
        } catch (OutOfMemoryError ignored) {
            return false;
        } finally {
            if (output != decoded) {
                output.recycle();
            }
            decoded.recycle();
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {
                    // The file is already flushed or the write failed.
                }
            }
        }
    }

    static void clear(Context context) {
        File file = file(context);
        if (file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    static boolean isHeif(byte[] data, int length) {
        if (data == null || length < 12) {
            return false;
        }
        if (data[4] != 'f' || data[5] != 't' || data[6] != 'y' || data[7] != 'p') {
            return false;
        }
        String brand = new String(data, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return brand.startsWith("hei") || brand.startsWith("mif") || brand.startsWith("msf") || brand.startsWith("avi");
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        while (width / sample > MAX_EDGE || height / sample > MAX_EDGE) {
            sample *= 2;
        }
        return sample;
    }

    private static Bitmap scaleDown(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = Math.min(1f, Math.min(MAX_EDGE / (float) width, MAX_EDGE / (float) height));
        if (scale >= 1f) {
            return source;
        }
        return Bitmap.createScaledBitmap(source, Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)), true);
    }
}
