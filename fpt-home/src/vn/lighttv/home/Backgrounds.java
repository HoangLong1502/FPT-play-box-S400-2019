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
        if (!isImage(data, length)) {
            return false;
        }
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return false;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight);
        Bitmap decoded = BitmapFactory.decodeByteArray(data, 0, length, options);
        if (decoded == null) {
            return false;
        }
        Bitmap output = scaleDown(decoded);
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file(context));
            boolean wrote = output.compress(Bitmap.CompressFormat.JPEG, 86, stream);
            stream.flush();
            return wrote;
        } catch (Exception ignored) {
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

    private static boolean isImage(byte[] data, int length) {
        if (length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return true;
        }
        return length >= 8
                && data[0] == (byte) 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47;
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        while (width / sample > MAX_EDGE * 2 || height / sample > MAX_EDGE * 2) {
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
