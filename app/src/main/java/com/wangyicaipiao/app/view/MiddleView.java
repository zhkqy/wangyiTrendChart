package com.wangyicaipiao.app.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by zhkqy on 15/8/11.
 */

public class MiddleView extends ViewGroup {

    int initX = 0;
    int initY = 0;
    int x = 0, y = 0;
    int tempInitX = 0;
    int tempInitY = 0;

    public static int cellWitch = 80;
    public static int cellHeight = 80;
    private int borderRight = 0;  //  右边距  包括没展示区域的总宽度
    private int borderBottom = 0;//   底边距  包括没展示区域的总高度
    private Context mContext;

    /**
     * 回调
     */
    public interface middleTouchEventListener {
        void middleOnTouchEvent(int gapX, int gapY);
    }

    private middleTouchEventListener eventListener;

    public void setMonTouchEventListener(middleTouchEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public MiddleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setFocusable(true);
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        removeAllViews();
        addTestData();
    }

    /**
     * 判断边界
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
                tempInitX = initX;
                tempInitY = initY;
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getX();
                int moveY = (int) event.getY();
                int gapX = moveX - x;
                int gapY = moveY - y;
                initX = tempInitX + gapX;
                initY = tempInitY + gapY;
                checkBorder();
                scrollTo(-initX, -initY);
                if (eventListener != null) {
                    eventListener.middleOnTouchEvent(initX, initY);
                }
                break;
        }
        return true;
    }

    /**
     * 添加测试数据
     */
    private void addTestData() {

        int left = 0;
        int right = cellWitch;
        int top = 0;
        int bottom = cellHeight;

        for (int i = 0; i < 33; i++) {
            for (int j = 0; j < 33; j++) {
                TextView t = new TextView(mContext);
                int random = (int) (1 + Math.random() * 33);
                t.setText(String.valueOf(random));
                t.layout(left, top, right, bottom);
                left += cellWitch;
                right += cellWitch;
                addView(t);
            }
            left = 0;
            right = cellWitch;
            top += cellHeight;
            bottom += cellHeight;
        }
        borderRight = 33 * cellWitch;
        borderBottom = 33 * cellHeight;
    }

    /**
     * 检查边界
     */
    private void checkBorder() {

        if (initX >= 0) {
            initX = 0;
        }
        if (initY >= 0) {
            initY = 0;
        }
        if (initX < -borderRight + getWidth()) {
            initX = -borderRight + getWidth();
        }
        if (initY < -borderBottom + getHeight()) {
            initY = -borderBottom + getHeight();
        }
    }

    /**
     * 外部调用滚动
     */
    public void setScrollXY(int x, int y) {
        initX = x;
        initY = y;
        scrollTo(-initX,-initY);
    }
}
