package com.wangyicaipiao.app.view.scrollview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.HashMap;

public class ExcelView extends View {

  public static class Cell {
    String text;
    Integer bgColor = null;
    Integer textColor = Color.BLACK;
    Integer ballColor = null;
    private boolean fixedX, fixedY;
    boolean fixedTextX, fixedTextY;
    int col, row;// cell坐标
    int width = 1, height = 1;// 宽度 高度均以单元格为单位
    int textSize = -1;
    Drawable rtDrawable = null;
    Align aligin = Align.CENTER;
  }

  final public static int SCROLL_START = 1;
  final public static int SCROLLING = 2;
  final public static int SCROLL_END = 3;

  public interface OnScrollChangedListener {
    void OnHorizontalScrollChanged(View view, int state, int dX);

    void OnVerticalScrollChanged(View view, int state, int dY);
  }

  OnScrollChangedListener onScrollChangedListener;
  protected int colWidth[];// 各列宽度
  protected int rowHeight[];// 各行宽度
  protected int cellX[];// 各cell的起始位置
  protected int cellY[];// 各cell的起始位置

  int offX, offY;
  int worldWidth, worldHeight;
  int minimumVelocity, maximumVelocity;
  int touchSlop;

  Integer horizontalDivideLineColor = Color.BLACK;
  Integer verticalDivideLineColor = Color.BLACK;

  int textSize = 16;
  int firstVisibleRow, lastVisibleRow;
  int firstVisibleCol, lastVisibleCol;
  float dp = 1.0f;

  @SuppressLint("NewApi")
  public ExcelView(Context context) {
    super(context);
    scroller = new Scroller(context);
    final ViewConfiguration configuration = ViewConfiguration.get(getContext());
    minimumVelocity = configuration.getScaledMinimumFlingVelocity();
    maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    touchSlop = configuration.getScaledTouchSlop();
    if (Integer.valueOf(Build.VERSION.SDK) >= 11)
      this.setLayerType(View.LAYER_TYPE_SOFTWARE, painter);
    dp = context.getResources().getDisplayMetrics().density;
  }

  Paint painter = new Paint(Paint.ANTI_ALIAS_FLAG);
  // Cell [][]data=null;
  private ArrayList<Cell> cells = new ArrayList<Cell>();// 全部cell
  /*
   * private ArrayList<Cell> normalCells=new ArrayList<Cell>();//正常单元 private
   * ArrayList<Cell> fixedCells1=new ArrayList<Cell>();//固定行或列的cell private
   * ArrayList<Cell> fixedCells2=new ArrayList<Cell>();//固定位置的cell
   */private ArrayList<Cell> mergedCells = new ArrayList<Cell>();// 合并单元格的cell
  private HashMap<String, Cell> pos2cell = new HashMap<String, Cell>();
  private ArrayList<Cell> linkedCells = new ArrayList<Cell>();
  private boolean showLinkedLine;
  Scroller scroller;
  int fixedRow = -1, fixedCol = -1;

  public void setTextSize(int size) {
    this.textSize = size;
  }

  public void setOnScrollChangedListener(
      OnScrollChangedListener onScrollChangedListener) {
    this.onScrollChangedListener = onScrollChangedListener;
  }

  public void setShowLinkedLine(boolean linkedLine) {
    showLinkedLine = linkedLine;
  }

  public void setLinkedCell(ArrayList<Cell> linkedCells) {
    this.linkedCells.clear();
    if (linkedCells != null)
      this.linkedCells.addAll(linkedCells);
  }

  /**
   * 返回一个cell在world中的位置
   * 
   * @param row
   * @param col
   * @return
   */
  public Rect getCellRectInWorld(int row, int col) {
    Cell cell = this.getCell(row, col);
    if (cell == null) {
      cell = new Cell();
      cell.row = row;
      cell.col = col;
    }

    return this.getCellRectInWorld(cell);
  }

  public enum DIRECTION {
    LEFT, UP, RIGHT, DOWN
  }

  /**
   * 返回当前列表是否可向某一方向滑动
   * 
   * @param direction
   * @return
   */
  public boolean canMoveTo(DIRECTION direction) {

    if (direction == DIRECTION.LEFT) {
      if (this.getWidth() > this.worldWidth
          || offX <= this.getWidth() - this.worldWidth)
        return false;
      else
        return true;
    } else if (direction == DIRECTION.RIGHT) {
      if (this.getWidth() > this.worldWidth || offX >= 0)
        return false;
      else
        return true;
    } else if (direction == DIRECTION.UP) {
      if (this.getHeight() > this.worldHeight
          || offY <= getHeight() - this.worldHeight)
        return false;
      else
        return true;
    } else if (direction == DIRECTION.DOWN) {
      if (this.getHeight() > this.worldHeight || offY <= 0)
        return false;
      else
        return true;
    }
    return true;
  }

  public int getOffX() {
    return offX;
  }

  public int getOffY() {
    return offY;
  }

  public int getWorldX() {
    return this.worldWidth;
  }

  public int getWorldHeight() {
    return this.worldHeight;
  }

  public void scrollTo(int offX, int offY, boolean animation) {
    scroller.abortAnimation();
    if (this.getWidth() > worldWidth)
      offX = 0;
    else if (offX > 0)
      offX = 0;
    else if (offX < this.getWidth() - worldWidth)
      offX = this.getWidth() - worldWidth;
    if (this.getHeight() > worldHeight)
      offY = 0;
    else if (offY > 0)
      offY = 0;
    else if (offY < this.getHeight() - worldHeight)
      offY = this.getHeight() - worldHeight;
    if (!animation) {

      this.offX = offX;

      this.offY = offY;
    } else {
      scroller.startScroll(this.offX, this.offY, offX - this.offX, offY
          - this.offY, 200);
    }
    this.invalidate();
  }

  public void setCellData(ArrayList<Cell> data, int fixedRow, int fixedCol) {
    cells.clear();

    mergedCells.clear();
    pos2cell.clear();
    cells.addAll(data);
    this.fixedRow = fixedRow;
    this.fixedCol = fixedCol;

    for (Cell t : data) {
      if (t.row < fixedRow)
        t.fixedY = true;
      if (t.col < fixedCol)
        t.fixedX = true;
      if (t.width > 1 || t.height > 1)
        mergedCells.add(t);

      pos2cell.put(t.row + "_" + t.col, t);

    }

  }

  @Override
  public void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    // 重新滚动列表
    if (this.worldWidth > w || this.worldHeight > h) {
      this.scrollTo(this.offX, this.offY, false);// 该方法会自动校正滚动超出最大范围的问题
    }

  }

  public void setCellSize(int[] width, int[] height) {
    colWidth = width;
    rowHeight = height;
    worldWidth = 0;
    worldHeight = 0;

    if (width != null) {
      cellX = new int[width.length + 1];
      for (int i = 0; i < width.length; i++) {
        cellX[i] = worldWidth;
        worldWidth += width[i];

      }
      cellX[width.length] = worldWidth;
    }
    if (height != null) {
      cellY = new int[height.length + 1];
      for (int i = 0; i < height.length; i++) {
        cellY[i] = worldHeight;
        worldHeight += height[i];
      }
      cellY[height.length] = worldHeight;
    }

  }

  public int getColNumer() {
    if (colWidth == null)
      return 0;
    return colWidth.length;
  }

  public int getRowNumer() {
    if (rowHeight == null)
      return 0;
    return rowHeight.length;
  }

  public Rect getCellRectInWorld(Cell cell) {
    Rect rect = new Rect();
    int x0 = cellX[cell.col];
    int y0 = cellY[cell.row];
    int x1 = cellX[cell.col + cell.width];
    int y1 = cellY[cell.row + cell.height];

    rect.left = x0;
    rect.top = y0;
    rect.right = x1;
    rect.bottom = y1;

    return rect;
  }

  public Cell getCell(int row, int col) {
    Cell cell = pos2cell.get(row + "_" + col);

    return cell;
  }

  public Rect getCellRectInView(Cell cell) {
    Rect rect = getCellRectInWorld(cell);

    int tx = offX, ty = offY;
    if (cell.fixedX)
      tx = 0;
    if (cell.fixedY)
      ty = 0;

    rect.left += tx;
    rect.top += ty;
    rect.right += tx;
    rect.bottom += ty;
    return rect;

  }

  public void setBorderColor(int hColor, int vColor) {

    this.horizontalDivideLineColor = hColor;
    this.verticalDivideLineColor = vColor;
  }

  public int[] getColWidth() {
    return this.colWidth;
  }

  public int[] getRowHeight() {
    return this.rowHeight;
  }

  private Rect getCellClipRect(Cell cell) {
    Rect cellRect = this.getCellRectInView(cell);
    if (cellRect == null)
      return null;
    Rect r1 = new Rect(0, 0, this.getWidth(), this.getHeight());
    if (cell.fixedX && cell.fixedY) {
      return cellRect;
    } else if (cell.fixedX) {
      r1 = new Rect(0, cellY[fixedRow], cellX[fixedCol], this.getHeight());

    } else if (cell.fixedY) {
      r1 = new Rect(cellX[fixedCol], 0, this.getWidth(), cellY[fixedRow]);

    } else {
      r1 = new Rect(cellX[fixedCol], cellY[fixedRow], this.getWidth(),
          this.getHeight());
    }
    if (r1.intersect(cellRect))
      return r1;
    else
      return null;
  }

  protected void onDrawExcelBackgroud(Canvas canvas) {

  }

  ArrayList<Rect> fixMerged = new ArrayList<Rect>(5);

  @Override
  public void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    onDrawExcelBackgroud(canvas);
    if (colWidth == null || rowHeight == null)
      return;
    if (showLinkedLine)
      onDrawLinkedLines(canvas);
    for (Cell cell : cells) {

      Rect rect = getCellClipRect(cell);
      if (rect != null) {
        canvas.save();
        canvas.clipRect(rect);
        onDrawCell(canvas, cell);
        canvas.restore();
      }

    }

    // 根据合并单元格设置裁剪区域
    canvas.save();
    canvas.clipRect(0, 0, this.getWidth(), this.getHeight());
    int fixMergedIndex = 0;
    for (Cell cell : mergedCells) {

      int tx = offX, ty = offY;
      if (cell.fixedX)
        tx = 0;
      if (cell.fixedY)
        ty = 0;
      int x0 = tx + cellX[cell.col];
      int y0 = ty + cellY[cell.row];
      int x1 = tx + cellX[cell.col + cell.width];
      int y1 = ty + cellY[cell.row + cell.height];

      if (x0 > this.getWidth() || y0 > this.getHeight())
        continue;
      if (x1 < 0 || y1 < 0)
        continue;
      canvas.clipRect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, Op.DIFFERENCE);
      if (cell.fixedX || cell.fixedY) {
        if (fixMergedIndex >= fixMerged.size())
          fixMerged.add(new Rect(0, 0, -1, -1));
        Rect r = fixMerged.get(fixMergedIndex);
        r.set(x0 + 1, y0 + 1, x1 - 1, y1 - 1);
        fixMergedIndex++;
      }
    }

    if (fixedRow > 0 && fixedRow < cellY.length) {
      if (Build.VERSION.SDK_INT > 11)// 2.3个别版本使用Op.UNION后会导致绘制到上层View
        canvas.clipRect(0, 0, this.getWidth(), cellY[fixedRow], Op.UNION);
    }
    if (fixedCol > 0 && fixedCol < cellX.length) {
      if (Build.VERSION.SDK_INT > 11)// 2.3个别版本使用Op.UNION后会导致绘制到上层View
        canvas.clipRect(0, 0, cellX[fixedCol], this.getHeight(), Op.UNION);
    }
    for (int i = 0; i < fixMergedIndex; i++) {
      Rect r = fixMerged.get(i);
      if (r.isEmpty() || r.left > this.getWidth() || r.top > this.getHeight())
        continue;
      if (r.right < 0 || r.bottom < 0)
        continue;
      canvas.clipRect(r.left, r.top, r.right, r.bottom, Op.DIFFERENCE);
    }

    painter.setStrokeWidth(1);
    int tempFirst = -1;
    int tempLast = rowHeight.length;
    if (this.horizontalDivideLineColor != null)
      painter.setColor(horizontalDivideLineColor);

    // 绘制分割线
    for (int i = 0; i < cellY.length; i++) {
      if (offY + cellY[i] >= 0 && tempFirst == -1)// 记录第一个可见行
        tempFirst = i;
      if (i <= this.fixedRow) {
        if (horizontalDivideLineColor != null
            && horizontalDivideLineColor != Color.TRANSPARENT)
          canvas.drawLine(offX, cellY[i], offX + worldWidth, cellY[i], painter);
      } else {
        int minY = 0;
        if (this.fixedRow > 0)
          minY = cellY[fixedRow];
        if (offY + cellY[i] < minY)
          continue;
        if (offY + cellY[i] > this.getHeight())
          break;
        tempLast = i;// 记录最后一个可见行
        if (horizontalDivideLineColor != null
            && horizontalDivideLineColor != Color.TRANSPARENT)
          canvas.drawLine(offX, offY + cellY[i], offX + worldWidth, offY
              + cellY[i], painter);
      }

    }
    this.firstVisibleRow = tempFirst;
    this.lastVisibleRow = tempLast;
    tempFirst = -1;
    tempLast = this.colWidth.length - 1;
    if (this.verticalDivideLineColor != null)
      painter.setColor(verticalDivideLineColor);
    for (int i = 0; i < cellX.length; i++) {
      if (offX + cellX[i] >= 0 && tempFirst == -1)
        tempFirst = i;
      if (i <= this.fixedCol) {
        if (this.verticalDivideLineColor != null
            && verticalDivideLineColor != Color.TRANSPARENT)
          canvas
              .drawLine(cellX[i], offY, cellX[i], offY + worldHeight, painter);
      } else {
        int minX = 0;
        if (this.fixedCol > 0)
          minX = cellX[fixedCol];
        if (offX + cellX[i] < minX)
          continue;
        if (offX + cellX[i] > this.getWidth())
          break;
        tempLast = i;
        if (verticalDivideLineColor != null
            && verticalDivideLineColor != Color.TRANSPARENT)
          canvas.drawLine(offX + cellX[i], offY, offX + cellX[i], offY
              + worldHeight, painter);
      }
    }
    this.firstVisibleCol = tempFirst;
    this.lastVisibleCol = tempLast;
    canvas.restore();

  }

  public void onDrawLinkedLines(Canvas canvas) {
    canvas.save();
    int startX = 0;
    int startY = 0;
    if (fixedCol > 0)
      startX = cellX[fixedCol];
    if (fixedRow > 0)
      startY = cellY[fixedRow];

    canvas.clipRect(startX, startY, this.getWidth(), this.getHeight());
    painter.setStrokeWidth(2 * this.getContext().getResources()
        .getDisplayMetrics().density);
    for (int i = 0; i < linkedCells.size() - 1; i++) {
      Cell c1 = linkedCells.get(i);
      Cell c2 = linkedCells.get(i + 1);
      if (c2.row - c1.row == 1) {
        if (c1.ballColor != null) {
          painter.setColor(c1.ballColor);
        } else if (c1.bgColor != null) {
          painter.setColor(c1.bgColor);
        }
        Rect r1 = this.getCellRectInView(c1);
        Rect r2 = this.getCellRectInView(c2);
        canvas.drawLine((r1.left + r1.right) / 2, (r1.top + r1.bottom) / 2,
            (r2.left + r2.right) / 2, (r2.top + r2.bottom) / 2, painter);
      }
    }
    canvas.restore();
  }

  protected void onDrawCell(Canvas canvas, Cell cell) {
    if (cell.text == null && cell.bgColor == null && cell.rtDrawable == null)
      return;// 如果无内容，且无背景色不进行绘制
    Rect rect = this.getCellRectInView(cell);
    if (cell.bgColor != null) {

      canvas.drawColor(cell.bgColor);
    }

    if (cell.ballColor != null) {
      painter.setColor(cell.ballColor);
      canvas.drawCircle(rect.centerX(), rect.centerY(), rect.height() / 2 - 1
          * dp, painter);
    }
    if (cell.textColor != null)
      painter.setColor(cell.textColor);
    if (cell.text != null) {
      if (cell.textSize > 0)
        painter.setTextSize(cell.textSize);
      else
        painter.setTextSize(this.textSize);
      painter.setTextAlign(cell.aligin);
      int cx = rect.centerX();
      if (cell.aligin == Align.LEFT)
        cx = rect.left;
      else if (cell.aligin == Align.RIGHT)
        cx = rect.right;
      int cy = rect.centerY() + (int) (painter.getTextSize() * 2 / 5 + 0.5f);
      if (cell.fixedTextX)
        cx -= offX;
      if (cell.fixedTextY)
        cy -= offY;

      canvas.drawText(cell.text, cx, cy, painter);
    }
    if (cell.rtDrawable != null) {
      int minWidth = cell.rtDrawable.getMinimumWidth();
      if (minWidth > rect.width() / 2)
        minWidth = rect.width() / 2;
      cell.rtDrawable.setBounds(new Rect(rect.right - minWidth, rect.top,
          rect.right, rect.top + minWidth));
      cell.rtDrawable.draw(canvas);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    if (widthMode == MeasureSpec.UNSPECIFIED
        || widthMode == MeasureSpec.AT_MOST) {

      if (widthMode == MeasureSpec.UNSPECIFIED || worldWidth < width)
        width = worldWidth;

    }

    if (heightMode == MeasureSpec.UNSPECIFIED
        || heightMode == MeasureSpec.AT_MOST) {

      if (heightMode == MeasureSpec.UNSPECIFIED || worldHeight < height)
        height = worldHeight;
    }

    setMeasuredDimension(width, height);
  }

  private final static float MOVEMENT_FACTOR = (float) 0.3;
  float lastX, lastY;
  boolean draggingX, draggingY;
  VelocityTracker velocityTracker = null;

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    super.onTouchEvent(event);
    if (velocityTracker == null)
      velocityTracker = VelocityTracker.obtain();
    velocityTracker.addMovement(event);
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      if (!scroller.isFinished())
        scroller.abortAnimation();
      lastX = event.getX();
      lastY = event.getY();

      draggingX = draggingY = false;
    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
      if (draggingX || Math.abs(event.getX() - lastX) > touchSlop) {
        this.offX += (event.getX() - lastX);
        if (this.getWidth() > worldWidth)
          offX = 0;
        else if (offX > 0)
          offX = 0;
        else if (offX < this.getWidth() - worldWidth)
          offX = this.getWidth() - worldWidth;

        if (!draggingX) {
          if (this.onScrollChangedListener != null)
            onScrollChangedListener.OnHorizontalScrollChanged(this,
                SCROLL_START, (int) (event.getX() - lastX));
        } else {
          if (this.onScrollChangedListener != null)
            onScrollChangedListener.OnHorizontalScrollChanged(this, SCROLLING,
                (int) (event.getX() - lastX));
        }
        lastX = event.getX();
        draggingX = true;
      }
      if (draggingY || Math.abs(event.getY() - lastY) > touchSlop) {
        this.offY += (event.getY() - lastY);
        if (this.getHeight() > worldHeight)
          offY = 0;
        else if (offY > 0)
          offY = 0;
        else if (offY < this.getHeight() - worldHeight)
          offY = this.getHeight() - worldHeight;

        if (!draggingY) {
          if (this.onScrollChangedListener != null)
            onScrollChangedListener.OnVerticalScrollChanged(this, SCROLL_START,
                (int) (event.getY() - lastY));
        } else {
          if (this.onScrollChangedListener != null)
            onScrollChangedListener.OnVerticalScrollChanged(this, SCROLLING,
                (int) (event.getY() - lastY));
        }
        lastY = event.getY();
        draggingY = true;
      }
      if (draggingX || draggingY)
         this.invalidate();
    } else if (event.getAction() == MotionEvent.ACTION_UP
        || event.getAction() == MotionEvent.ACTION_CANCEL) {
      velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
      float vx = velocityTracker.getXVelocity();
      float vy = velocityTracker.getYVelocity();
      int minX, minY;
      boolean flip = false;
      float minTan = 3.0f;
      // 当斜率大于minTan时忽略小方向的速度
      if (Math.abs(vx) < minimumVelocity || this.getWidth() > worldWidth
          || Math.abs(vy / vx) > minTan) {
        vx = 0;
        if (this.getWidth() > worldWidth)
          minX = 0;
        else
          minX = this.getWidth() - worldWidth;
      } else {
        minX = this.getWidth() - worldWidth;
        flip = true;
      }
      if (Math.abs(vy) < minimumVelocity || this.getHeight() > worldHeight
          || Math.abs(vx / vy) > minTan) {
        vy = 0;
        if (this.getHeight() > worldHeight)
          minY = 0;
        else
          minY = this.getHeight() - worldHeight;
      } else {
        minY = this.getHeight() - worldHeight;
        flip = true;
      }
      if (flip) {
        scrolling = true;
        scroller.fling(offX, offY, (int) vx, (int) vy, minX, 0, minY, 0);
        this.invalidate();
      } else {
        if (this.onScrollChangedListener != null) {
          if (draggingX) {
            onScrollChangedListener.OnHorizontalScrollChanged(this, SCROLL_END,
                0);
          }
          if (draggingY) {
            onScrollChangedListener
                .OnVerticalScrollChanged(this, SCROLL_END, 0);
          }
        }
      }
      draggingX = draggingY = false;
      velocityTracker.recycle();
      velocityTracker = null;
      this.invalidate();
    }
    return true;
  }

  boolean scrolling = false;

  @Override
  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      if (this.onScrollChangedListener != null) {

        onScrollChangedListener.OnHorizontalScrollChanged(this, SCROLLING,
            scroller.getCurrX() - offX);
        onScrollChangedListener.OnVerticalScrollChanged(this, SCROLLING,
            scroller.getCurrY() - offY);
      }
      offX = scroller.getCurrX();
      offY = scroller.getCurrY();
      invalidate();
    } else {
      if (scrolling) {
        if (this.onScrollChangedListener != null) {

          onScrollChangedListener
              .OnHorizontalScrollChanged(this, SCROLL_END, 0);
          onScrollChangedListener.OnVerticalScrollChanged(this, SCROLL_END, 0);
        }
        scrolling = false;
      }
    }
  }
}
