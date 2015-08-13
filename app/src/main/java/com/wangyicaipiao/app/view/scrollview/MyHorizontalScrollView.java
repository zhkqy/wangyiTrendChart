package com.wangyicaipiao.app.view.scrollview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

public class MyHorizontalScrollView extends HorizontalScrollView {
    public OnHorizontalScrollListener onScrollListener;

    /**
     * 主要是用在用户手指离开MyScrollView，MyScrollView还在继续滑动，我们用来保存Y的距离，然后做比较
     */

    public MyHorizontalScrollView(Context context) {
        this(context, null);

    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 设置滚动接口
     *
     * @param onScrollListener
     */
    public void setOnHorizontalScrollListener(OnHorizontalScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }


    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        if (onScrollListener != null) {
            onScrollListener.onScroll(scrollX, scrollY);
        }
    }

    /**
     * 滚动的回调接口
     */
    public interface OnHorizontalScrollListener {
        void onScroll(int scrollX, int scrollY);
    }
}