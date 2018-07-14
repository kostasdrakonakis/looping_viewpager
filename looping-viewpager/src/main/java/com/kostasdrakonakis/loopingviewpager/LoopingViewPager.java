package com.kostasdrakonakis.loopingviewpager;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.annotation.IntegerRes;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.kostasdrakonakis.loopingviewpager.adapter.LoopingViewPagerAdapter;

public class LoopingViewPager extends ViewPager {

    public interface IndicatorChangeListener {
        void onIndicatorProgress(int selectingPosition, float progress);

        void onIndicatorPageChange(int newIndicatorPosition);
    }

    private boolean isToTheRight = true;
    private boolean isIndicatorSmart = false;
    private int previousScrollState = SCROLL_STATE_IDLE;
    private int scrollState = SCROLL_STATE_IDLE;
    private int interval = 5000;
    private int previousPosition = 0;
    private int currentPagePosition = 0;

    protected boolean isEndless = true;
    protected boolean isAutoScroll = false;
    protected boolean wrapContent = true;
    protected float aspectRatio;

    private Handler autoScrollHandler = new Handler();
    private IndicatorChangeListener indicatorListener;

    public LoopingViewPager(Context context) {
        super(context);
        init();
    }

    public LoopingViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.LoopingViewPager, 0, 0);
        try {
            isEndless = a.getBoolean(R.styleable.LoopingViewPager_isEndless, false);
            isAutoScroll = a.getBoolean(R.styleable.LoopingViewPager_autoScroll, false);
            wrapContent = a.getBoolean(R.styleable.LoopingViewPager_wrap_content, true);
            interval = a.getInt(R.styleable.LoopingViewPager_scrollInterval, getInterval(R.integer.auto_scroll_interval));
            aspectRatio = a.getFloat(R.styleable.LoopingViewPager_viewPagerAspectRatio, 0f);
        } finally {
            a.recycle();
        }
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (aspectRatio > 0) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = Math.round((float) MeasureSpec.getSize(widthMeasureSpec) / aspectRatio);
            int finalWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int finalHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(finalWidthMeasureSpec, finalHeightMeasureSpec);
        } else {
            if (wrapContent) {
                int mode = MeasureSpec.getMode(heightMeasureSpec);
                if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    int height = 0;
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                        int h = child.getMeasuredHeight();
                        if (h > height) height = h;
                    }
                    heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        super.setAdapter(adapter);
        if (isEndless) setCurrentItem(1, false);
    }

    public int getIndicatorPosition() {
        if (!isEndless) {
            return currentPagePosition;
        } else {
            if (!(getAdapter() instanceof LoopingViewPagerAdapter)) return currentPagePosition;
            if (currentPagePosition == 0) {
                return ((LoopingViewPagerAdapter) getAdapter()).getListCount() - 1;
            } else if (currentPagePosition == ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition() + 1) {
                return 0;
            } else {
                return currentPagePosition - 1;
            }
        }
    }

    public int getSelectingIndicatorPosition(boolean isToTheRight) {
        if (scrollState == SCROLL_STATE_SETTLING
                || scrollState == SCROLL_STATE_IDLE
                || (previousScrollState == SCROLL_STATE_SETTLING
                && scrollState == SCROLL_STATE_DRAGGING)) {
            return getIndicatorPosition();
        }
        int delta = isToTheRight ? 1 : -1;

        if (!isEndless) return currentPagePosition + delta;

        if (!(getAdapter() instanceof LoopingViewPagerAdapter))
            return currentPagePosition + delta;
        if (currentPagePosition == 1 && !isToTheRight) {
            return ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition() - 1;
        } else if (currentPagePosition == ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition()
                && isToTheRight) {
            return 0;
        } else {
            return currentPagePosition + delta - 1;
        }
    }

    public int getIndicatorCount() {
        PagerAdapter adapter = getAdapter();
        if (adapter == null) return 0;

        if (adapter instanceof LoopingViewPagerAdapter) {
            return ((LoopingViewPagerAdapter) adapter).getListCount();
        } else {
            return adapter.getCount();
        }
    }

    public void reset() {
        if (isEndless) {
            setCurrentItem(1, false);
            currentPagePosition = 1;
        } else {
            setCurrentItem(0, false);
            currentPagePosition = 0;
        }
    }

    public void setIndicatorSmart(boolean isIndicatorSmart) {
        this.isIndicatorSmart = isIndicatorSmart;
    }

    public void setAutoScroll(boolean isAutoScroll) {
        this.isAutoScroll = isAutoScroll;
    }

    public void setIndicatorChangeListener(IndicatorChangeListener indicatorListener) {
        this.indicatorListener = indicatorListener;
    }

    public void resumeAutoScroll() {
        autoScrollHandler.postDelayed(autoScrollRunnable, interval);
    }

    public void pauseAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    protected void init() {
        addOnPageChangeListener(new OnPageChangeListener() {
            float currentPosition;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                isToTheRight = position + positionOffset >= currentPosition;
                if (positionOffset == 0) currentPosition = position;

                int realPosition = getSelectingIndicatorPosition(isToTheRight);

                float progress;
                if (scrollState == SCROLL_STATE_SETTLING && Math.abs(currentPagePosition - previousPosition) > 1) {
                    int pageDiff = Math.abs(currentPagePosition - previousPosition);
                    if (isToTheRight) {
                        progress = (((float) (position - previousPosition) / pageDiff)) + (positionOffset / pageDiff);
                    } else {
                        progress = ((float) (previousPosition - (position + 1)) / pageDiff) + ((1 - positionOffset) / pageDiff);
                    }
                } else {
                    progress = isToTheRight ? positionOffset : (1 - positionOffset);
                }

                if (progress == 0 || progress > 1) return;

                if (isIndicatorSmart) {
                    if (scrollState != SCROLL_STATE_DRAGGING) return;
                    if (indicatorListener != null) {
                        indicatorListener.onIndicatorProgress(realPosition, progress);
                    }
                } else {
                    if (scrollState == SCROLL_STATE_DRAGGING) {
                        if ((isToTheRight && Math.abs(realPosition - currentPagePosition) == 2) ||
                                !isToTheRight && realPosition == currentPagePosition) {
                            return;
                        }
                    }
                    if (indicatorListener != null) {
                        indicatorListener.onIndicatorProgress(realPosition, progress);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                previousPosition = currentPagePosition;
                currentPagePosition = position;
                if (indicatorListener != null) {
                    indicatorListener.onIndicatorPageChange(getIndicatorPosition());
                }
                autoScrollHandler.removeCallbacks(autoScrollRunnable);
                autoScrollHandler.postDelayed(autoScrollRunnable, getInterval(R.integer.auto_scroll_interval));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (!isIndicatorSmart) {
                    if (scrollState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_DRAGGING) {
                        if (indicatorListener != null) {
                            indicatorListener.onIndicatorProgress(
                                    getSelectingIndicatorPosition(isToTheRight), 1);
                        }
                    }
                }
                previousScrollState = scrollState;
                scrollState = state;

                if (state == SCROLL_STATE_IDLE) {
                    if (isEndless) {
                        PagerAdapter pagerAdapter = getAdapter();
                        if (pagerAdapter == null) return;

                        int itemCount = pagerAdapter.getCount();
                        if (itemCount < 2) {
                            return;
                        }

                        int index = getCurrentItem();
                        if (index == 0) {
                            setCurrentItem(itemCount - 2, false);
                        } else if (index == itemCount - 1) {
                            setCurrentItem(1, false);
                        }
                    }

                    if (indicatorListener != null) {
                        indicatorListener.onIndicatorProgress(getIndicatorPosition(), 1);
                    }
                }
            }
        });
        if (isEndless) setCurrentItem(1, false);
    }

    private int getInterval(@IntegerRes int intId) {
        return getContext().getResources().getInteger(intId);
    }

    private Runnable autoScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (getAdapter() == null || !isAutoScroll || getAdapter().getCount() < 2) return;
            if (!isEndless && getAdapter().getCount() - 1 == currentPagePosition) {
                currentPagePosition = 0;
            } else {
                currentPagePosition++;
            }
            setCurrentItem(currentPagePosition, true);
        }
    };
}
