package com.example.asm_final.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.asm_final.R;
import java.util.ArrayList;
import java.util.List;

/** A theme-aware EXP ring. An empty ring represents zero, never invented subject data. */
public class SimplePieChartView extends View {
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF bounds = new RectF();
  private final List<Slice> slices = new ArrayList<>();
  private double total;

  public static class Slice {
    final float value;
    final int color;

    public Slice(float value, int color) {
      this.value = value;
      this.color = color;
    }
  }

  public SimplePieChartView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setData(List<Slice> values) {
    slices.clear();
    total = 0;
    if (values != null)
      for (Slice slice : values) {
        if (slice != null
            && slice.value > 0
            && !Float.isInfinite(slice.value)
            && !Float.isNaN(slice.value)) {
          slices.add(slice);
          total += slice.value;
        }
      }
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    float density = getResources().getDisplayMetrics().density;
    float width = getWidth() - getPaddingLeft() - getPaddingRight();
    float height = getHeight() - getPaddingTop() - getPaddingBottom();
    float stroke = Math.min(8 * density, Math.min(width, height) / 6f);
    float radius = (Math.min(width, height) - stroke) / 2f - density;
    if (radius <= 0 || stroke <= 0) return;
    float cx = getPaddingLeft() + width / 2f, cy = getPaddingTop() + height / 2f;
    bounds.set(cx - radius, cy - radius, cx + radius, cy + radius);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(stroke);
    paint.setStrokeCap(Paint.Cap.BUTT);
    paint.setColor(ContextCompat.getColor(getContext(), R.color.progressTrackColor));
    canvas.drawOval(bounds, paint);
    if (total <= 0) return;
    float start = -90;
    for (Slice slice : slices) {
      float sweep = (float) (slice.value / total * 360);
      float gap = slices.size() > 1 ? Math.min(3, sweep / 4) : 0;
      paint.setColor(slice.color);
      canvas.drawArc(bounds, start + gap / 2, sweep - gap, false, paint);
      start += sweep;
    }
  }
}
