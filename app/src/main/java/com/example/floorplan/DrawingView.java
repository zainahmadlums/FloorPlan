package com.example.floorplan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    public enum Mode { DRAW, PAN_ZOOM }

    private Mode currentMode = Mode.DRAW;
    private Bitmap baseBitmap;
    private final Matrix matrix = new Matrix();
    private final Matrix inverseMatrix = new Matrix();
    private final Paint paint = new Paint();

    private int currentColor = Color.BLACK;
    private float startX, startY, currentX, currentY;
    private boolean isDrawing = false;
    private boolean initialSetupDone = false;

    // Zoom and Pan variables
    private ScaleGestureDetector scaleDetector;
    private float lastTouchX, lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    private static class DrawAction {
        RectF rect;
        int color;
        DrawAction(RectF rect, int color) {
            this.rect = rect;
            this.color = color;
        }
    }

    private final List<DrawAction> actions = new ArrayList<>();
    private final List<DrawAction> redoActions = new ArrayList<>();

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.FILL);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                matrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
                matrix.invert(inverseMatrix);
                invalidate();
                return true;
            }
        });
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
    }

    public Mode getMode() {
        return currentMode;
    }

    public void setBitmap(Bitmap bitmap) {
        this.baseBitmap = bitmap;
        initialSetupDone = false;
        calculateInitialMatrix();
        invalidate();
    }

    public void setCurrentColor(int color) {
        this.currentColor = color;
    }

    public void undo() {
        if (!actions.isEmpty()) {
            redoActions.add(actions.remove(actions.size() - 1));
            invalidate();
        }
    }

    public void redo() {
        if (!redoActions.isEmpty()) {
            actions.add(redoActions.remove(redoActions.size() - 1));
            invalidate();
        }
    }

    public Bitmap getResultBitmap() {
        if (baseBitmap == null) return null;
        Bitmap result = baseBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        for (DrawAction action : actions) {
            p.setColor(action.color);
            canvas.drawRect(action.rect, p);
        }
        return result;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateInitialMatrix();
    }

    private void calculateInitialMatrix() {
        if (!initialSetupDone && baseBitmap != null && getWidth() > 0 && getHeight() > 0) {
            float scale = Math.min((float) getWidth() / baseBitmap.getWidth(), (float) getHeight() / baseBitmap.getHeight());
            float dx = (getWidth() - baseBitmap.getWidth() * scale) / 2f;
            float dy = (getHeight() - baseBitmap.getHeight() * scale) / 2f;
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx, dy);
            matrix.invert(inverseMatrix);
            initialSetupDone = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (baseBitmap == null) return false;

        if (currentMode == Mode.PAN_ZOOM) {
            scaleDetector.onTouchEvent(event);

            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    final int pointerIndex = event.getActionIndex();
                    lastTouchX = event.getX(pointerIndex);
                    lastTouchY = event.getY(pointerIndex);
                    activePointerId = event.getPointerId(0);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);
                        if (!scaleDetector.isInProgress()) {
                            final float dx = x - lastTouchX;
                            final float dy = y - lastTouchY;
                            matrix.postTranslate(dx, dy);
                            matrix.invert(inverseMatrix);
                            invalidate();
                        }
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                    break;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_POINTER_UP: {
                    final int pointerIndex = event.getActionIndex();
                    final int pointerId = event.getPointerId(pointerIndex);
                    if (pointerId == activePointerId) {
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        lastTouchX = event.getX(newPointerIndex);
                        lastTouchY = event.getY(newPointerIndex);
                        activePointerId = event.getPointerId(newPointerIndex);
                    }
                    break;
                }
            }
            return true;
        } else {
            // DRAW MODE
            float[] pts = {event.getX(), event.getY()};
            inverseMatrix.mapPoints(pts);

            float x = Math.max(0, Math.min(baseBitmap.getWidth(), pts[0]));
            float y = Math.max(0, Math.min(baseBitmap.getHeight(), pts[1]));

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = x;
                    startY = y;
                    currentX = x;
                    currentY = y;
                    isDrawing = true;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    currentX = x;
                    currentY = y;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    currentX = x;
                    currentY = y;
                    isDrawing = false;
                    RectF rect = new RectF(
                            Math.min(startX, currentX),
                            Math.min(startY, currentY),
                            Math.max(startX, currentX),
                            Math.max(startY, currentY)
                    );
                    if (rect.width() > 0 && rect.height() > 0) {
                        actions.add(new DrawAction(rect, currentColor));
                        redoActions.clear();
                    }
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(event);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (baseBitmap == null) return;

        canvas.save();
        canvas.concat(matrix);

        canvas.drawBitmap(baseBitmap, 0, 0, null);

        for (DrawAction action : actions) {
            paint.setColor(action.color);
            canvas.drawRect(action.rect, paint);
        }

        if (isDrawing && currentMode == Mode.DRAW) {
            paint.setColor(currentColor);
            RectF previewRect = new RectF(
                    Math.min(startX, currentX),
                    Math.min(startY, currentY),
                    Math.max(startX, currentX),
                    Math.max(startY, currentY)
            );
            canvas.drawRect(previewRect, paint);
        }
        canvas.restore();
    }
}