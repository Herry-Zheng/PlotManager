package com.example.plotmanager.plot;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PlotView extends View {

    // 节点尺寸（像素）
    private float nodeWidth, nodeHeight;
    private float gapX, gapY;
    private float minLogicalX = 0f;

    // 数据
    private List<PlotNode> nodes = new ArrayList<>();
    private Set<String> resultId = null;

    // 画笔
    private Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 变换参数
    private float scale = 1.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float initScale = 1.0f;
    private boolean isFirstLayout = true;
    private Matrix currentMatrix = new Matrix();

    // 手势
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // 节点点击监听（短按）
    public interface OnNodeClickListener {
        void onNodeClick(String nodeId);
    }
    private OnNodeClickListener clickListener;

    // 节点长按监听
    public interface OnNodeLongClickListener {
        void onNodeLongClick(String nodeId);
    }
    private OnNodeLongClickListener longClickListener;

    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnNodeLongClickListener(OnNodeLongClickListener listener) {
        this.longClickListener = listener;
    }

    public PlotView(Context context) {
        super(context);
        init(context);
    }

    public PlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setNodeVisibility(Set<String> ids){
        this.resultId = ids;
        invalidate();
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        nodeWidth = 180f * density;
        nodeHeight = 80f * density;
        gapX = nodeWidth;
        gapY = nodeHeight;

        nodePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        nodePaint.setStrokeWidth(3f * density);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);

        linePaint.setStrokeWidth(6f * density);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    // 加载数据
    public void loadStoryData(String storyId) {
        nodes.clear();
        try {
            nodes = PlotStorageHelper.loadNodesFromFile(getContext(), storyId);
        } catch (IllegalStateException e) {
            if (getContext() instanceof AppCompatActivity) {
                PlotStorageHelper.handleInvalidStoryFile((AppCompatActivity) getContext(), storyId);
            }
            nodes.clear();
            return;
        }

        invalidate();
    }

    private void resetTransform() {
        if (nodes.isEmpty()) return;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (PlotNode n : nodes) {
            float lx = (n.depth * (nodeWidth + gapX)) - minLogicalX;
            float ly = n.y * (nodeHeight + gapY) - nodeHeight / 2f;
            if (lx < minX) minX = lx;
            if (lx + nodeWidth > maxX) maxX = lx + nodeWidth;
            if (ly < minY) minY = ly;
            if (ly + nodeHeight > maxY) maxY = ly + nodeHeight;
        }
        float totalWidth = maxX - minX + nodeWidth;
        float totalHeight = maxY - minY + nodeHeight;

        float viewWidth = getWidth() - 40f;
        float viewHeight = getHeight() - 40f;
        float scaleX = viewWidth / totalWidth;
        float scaleY = viewHeight / totalHeight;
        initScale = Math.min(scaleX, scaleY);
        if (initScale < 0.1f) initScale = 0.1f;
        if (initScale > 10.0f) initScale = 10.0f;

        scale = initScale;
        offsetX = getWidth() / 2f - nodeWidth;
        offsetY = getHeight() / 2f;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (isFirstLayout && !nodes.isEmpty()) {
            resetTransform();
            isFirstLayout = false;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nodes.isEmpty()) return;

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);
        canvas.getMatrix(currentMatrix);

        // 绘制连线
        for (PlotNode node : nodes) {
            if (!node.type.equals("origin") && node.fatherId != null && !node.fatherId.isEmpty()) {
                PlotNode pred = findNodeById(node.fatherId);
                if (pred != null && !pred.type.equals("ghost")) {
                    float predCenterX = (pred.depth * (nodeWidth + gapX)) - minLogicalX + nodeWidth / 2f;
                    float predCenterY = - pred.y * (nodeHeight + gapY);
                    float nodeCenterX = (node.depth * (nodeWidth + gapX)) - minLogicalX + nodeWidth / 2f;
                    float nodeCenterY = - node.y * (nodeHeight + gapY);

                    float startX = predCenterX + nodeWidth / 2f;
                    float startY = predCenterY;
                    float endX = nodeCenterX - nodeWidth / 2f;
                    float endY = nodeCenterY;

                    linePaint.setColor(Color.parseColor(node.color));
                    canvas.drawLine(startX, startY, endX, endY, linePaint);
                }
            }
        }

        // 绘制节点
        for (PlotNode node : nodes) {
            if (!node.type.equals("ghost")) {
                float lx = (node.depth * (nodeWidth + gapX)) - minLogicalX;
                float ly = -node.y * (nodeHeight + gapY) - nodeHeight / 2f;

                if (resultId != null && !resultId.contains(node.id)) {
                    nodePaint.setAlpha(80);
                    textPaint.setAlpha(80);
                }
                nodePaint.setColor(Color.parseColor(node.color));
                RectF rect = new RectF(lx, ly, lx + nodeWidth, ly + nodeHeight);
                canvas.drawRoundRect(rect, 16f * scale, 16f * scale, nodePaint);

                String title = node.title;
                if (title.length() > 10) title = title.substring(0, 10);
                String line1 = title.length() > 5 ? title.substring(0, 5) : title;
                String line2 = title.length() > 5 ? title.substring(5) : "";

                float centerX = lx + nodeWidth / 2f;
                float centerY = ly + nodeHeight / 2f;
                canvas.drawText(line1, centerX, centerY - 10f * scale, textPaint);
                if (!line2.isEmpty()) {
                    canvas.drawText(line2, centerX, centerY + 30f * scale, textPaint);
                }
                nodePaint.setAlpha(255);
                textPaint.setAlpha(255);
            }
        }

        nodePaint.setAlpha(255);
        textPaint.setAlpha(255);
        canvas.restore();
    }

    private PlotNode findNodeById(String id) {
        for (PlotNode n : nodes) if (n.id.equals(id)) return n;
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float newScale = scale * detector.getScaleFactor();
            if (newScale < 0.1f) newScale = 0.1f;
            if (newScale > 10.0f) newScale = 10.0f;
            scale = newScale;
            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            offsetX -= distanceX;
            offsetY -= distanceY;
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            resetTransform();
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (longClickListener == null) return;
            String nodeId = findNodeAt(e.getX(), e.getY());
            if (nodeId != null) {
                longClickListener.onNodeLongClick(nodeId);
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (clickListener == null) return false;
            String nodeId = findNodeAt(e.getX(), e.getY());
            if (nodeId != null) {
                clickListener.onNodeClick(nodeId);
                return true;
            }
            return false;
        }

        private String findNodeAt(float screenX, float screenY) {
            float[] screenPoint = new float[]{screenX, screenY};
            Matrix inverse = new Matrix();
            if (!currentMatrix.invert(inverse)) return null;
            float[] logicalPoint = new float[2];
            inverse.mapPoints(logicalPoint, screenPoint);
            for (PlotNode node : nodes) {
                float lx = (node.depth * (nodeWidth + gapX)) - minLogicalX;
                float ly = -node.y * (nodeHeight + gapY) - nodeHeight / 2f;
                if (logicalPoint[0] >= lx && logicalPoint[0] <= lx + nodeWidth &&
                        logicalPoint[1] >= ly && logicalPoint[1] <= ly + nodeHeight) {
                    return node.id;
                }
            }
            return null;
        }
    }
}