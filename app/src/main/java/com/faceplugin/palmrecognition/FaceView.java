package com.faceplugin.palmrecognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.View;

import androidx.annotation.Nullable;

import com.faceplugin.palm.PalmResult;
import java.util.List;

public class FaceView extends View {

    private Context context;
    private Paint realPaint;

    private Size frameSize;

    private List<PalmResult> faceBoxes;
    private String label;

    public FaceView(Context context) {
        this(context, null);

        this.context = context;
        init();
    }

    public FaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    public void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        realPaint = new Paint();
        realPaint.setStyle(Paint.Style.STROKE);
        realPaint.setStrokeWidth(3);
        realPaint.setColor(Color.GREEN);
        realPaint.setAntiAlias(true);
        realPaint.setTextSize(50);

        label = null;
    }

    public void setFrameSize(Size frameSize)
    {
        this.frameSize = frameSize;
    }

    public void setFaceBoxes(List<PalmResult> faceBoxes, String person_name)
    {
        this.faceBoxes = faceBoxes;
        label = person_name;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (frameSize != null &&  faceBoxes != null) {
            float x_scale = this.frameSize.getWidth() / (float)canvas.getWidth();
            float y_scale = this.frameSize.getHeight() / (float)canvas.getHeight();

            for (int i = 0; i < faceBoxes.size(); i++) {
                PalmResult faceBox = faceBoxes.get(i);

                if (label != null) {
                    realPaint.setStrokeWidth(3);
                    realPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    String showText = label;
                    canvas.drawText(showText, (faceBox.x1 / x_scale) + 10, (faceBox.y1 / y_scale) - 30, realPaint);
                }

                realPaint.setStyle(Paint.Style.STROKE);
                realPaint.setStrokeWidth(5);
                canvas.drawRect(new Rect(
                        (int)(faceBox.x1 / x_scale), (int)(faceBox.y1 / y_scale),
                        (int)(faceBox.x2 / x_scale), (int)(faceBox.y2 / y_scale)
                ), realPaint);
            }
        }
    }
}
