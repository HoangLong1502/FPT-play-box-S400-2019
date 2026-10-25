package vn.lighttv.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public final class StageView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Camera camera = new Camera();
    private final RectF rect = new RectF();
    private RadialGradient leftGlow;
    private RadialGradient rightGlow;
    private RadialGradient vignette;
    private Bitmap backdrop;
    private float bias;
    private float phase;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            phase += 0.012f;
            if (phase > 1f) {
                phase -= 1f;
            }
            invalidate();
            postDelayed(this, 50);
        }
    };

    public StageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setFocusBias(float bias) {
        this.bias = bias;
        invalidate();
    }

    public void setBackdrop(Bitmap bitmap) {
        Bitmap previous = backdrop;
        backdrop = bitmap;
        if (previous != null && previous != bitmap) {
            previous.recycle();
        }
        invalidate();
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        removeCallbacks(tick);
        post(tick);
    }

    public void stop() {
        running = false;
        removeCallbacks(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        leftGlow = new RadialGradient(w * 0.22f, h * 0.18f, w * 0.48f, 0x66466DFF, 0x00070910, Shader.TileMode.CLAMP);
        rightGlow = new RadialGradient(w * 0.82f, h * 0.72f, w * 0.42f, 0x5537D6C6, 0x00070910, Shader.TileMode.CLAMP);
        vignette = new RadialGradient(w * 0.5f, h * 0.45f, w * 0.62f, 0x00000000, 0xCC05060A, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        canvas.drawColor(0xFF070910);
        float drift = (float) Math.sin(phase * Math.PI * 2.0) * 36f;

        if (leftGlow != null) {
            canvas.save();
            canvas.translate(drift + bias * 28f, 0f);
            paint.setShader(leftGlow);
            canvas.drawRect(-80f, 0f, w + 80f, h, paint);
            canvas.restore();
        }
        if (rightGlow != null) {
            canvas.save();
            canvas.translate(-drift * 0.6f - bias * 20f, 0f);
            paint.setShader(rightGlow);
            canvas.drawRect(-80f, 0f, w + 80f, h, paint);
            canvas.restore();
        }
        paint.setShader(null);

        if (backdrop != null && !backdrop.isRecycled()) {
            drawPhoto(canvas, w, h);
        }
        drawFloor(canvas, w, h);
        if (vignette != null) {
            paint.setShader(vignette);
            canvas.drawRect(0f, 0f, w, h, paint);
            paint.setShader(null);
        }
    }

    private void drawPhoto(Canvas canvas, int w, int h) {
        float bw = backdrop.getWidth();
        float bh = backdrop.getHeight();
        float scale = Math.max(w / bw, h / bh) * 1.08f;
        float dw = bw * scale;
        float dh = bh * scale;
        rect.set((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f);

        canvas.save();
        canvas.translate(w / 2f, h / 2f);
        camera.save();
        camera.rotateY(bias * 7f);
        camera.rotateX(5f);
        camera.applyToCanvas(canvas);
        camera.restore();
        canvas.translate(-w / 2f, -h / 2f);
        paint.setShader(null);
        canvas.drawBitmap(backdrop, null, rect, paint);
        canvas.restore();
        canvas.drawColor(0xA0000000);
    }

    private void drawFloor(Canvas canvas, int w, int h) {
        canvas.save();
        canvas.translate(w / 2f + bias * 36f, h * 0.62f);
        camera.save();
        camera.rotateX(68f);
        camera.applyToCanvas(canvas);
        camera.restore();

        float span = w * 1.5f;
        float depth = h * 1.7f;
        float shift = phase * (span / 12f);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(backdrop == null ? 0x66D5E4FF : 0x33FFFFFF);
        for (int i = 0; i <= 12; i++) {
            float x = -span / 2f + span * i / 12f + shift;
            if (x > span / 2f) {
                x -= span;
            }
            canvas.drawLine(x, 0f, x, depth, paint);
        }
        for (int i = 1; i <= 8; i++) {
            float y = depth * i / 8f;
            canvas.drawLine(-span / 2f, y, span / 2f, y, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }
}
