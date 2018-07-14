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

    protected boolean isEndless = true;
    protected boolean isAutoScroll = false;
    protected boolean wrapContent = true;
    protected float aspectRatio;

    //AutoScroll
    private int interval = 5000;
    private int previousPosition = 0;
    private int currentPagePosition = 0;
    private Handler autoScrollHandler = new Handler();

    //For Indicator
    private IndicatorChangeListener indicatorListener;
    private int previousScrollState = SCROLL_STATE_IDLE;
    private int scrollState = SCROLL_STATE_IDLE;
    private boolean isToTheRight = true;
    /**
     * This boolean indicates whether LoopingViewPager needs to continuously tell the indicator about
     * the progress of the scroll, even after onIndicatorPageChange().
     * If indicator is smart, it should be able to finish the animation by itself after we told it that a position has been selected.
     * If indicator is not smart, then LoopingViewPager will continue to fire onIndicatorProgress() to update the indicator
     * transition position.
     */
    private boolean isIndicatorSmart = false;

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
            interval = a.getInt(R.styleable.LoopingViewPager_scrollInterval, 5000);
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
            //https://stackoverflow.com/a/24666987/7870874
            if (wrapContent) {
                int mode = MeasureSpec.getMode(heightMeasureSpec);
                // Unspecified means that the ViewPager is in a ScrollView WRAP_CONTENT.
                // At Most means that the ViewPager is not in a ScrollView WRAP_CONTENT.
                if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
                    // super has to be called in the beginning so the child views can be initialized.
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

    public void resumeAutoScroll() {
        autoScrollHandler.postDelayed(autoScrollRunnable, interval);
    }

    public void pauseAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable);
    }

    /**
     * A method that helps you integrate a ViewPager Indicator.
     * This method returns the expected position (Starting from 0) of indicators.
     * This method should be used after currentPagePosition is updated.
     */
    public int getIndicatorPosition() {
        if (!isEndless) {
            return currentPagePosition;
        } else {
            if (!(getAdapter() instanceof LoopingViewPagerAdapter)) return currentPagePosition;
            if (currentPagePosition == 0) { //Dummy last item is selected. Indicator should be at the last one
                return ((LoopingViewPagerAdapter) getAdapter()).getListCount() - 1;
            } else if (currentPagePosition == ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition() + 1) {
                //Dummy first item is selected. Indicator should be at the first one
                return 0;
            } else {
                return currentPagePosition - 1;
            }
        }
    }

    /**
     * A method that helps you integrate a ViewPager Indicator.
     * This method returns the expected position (Starting from 0) of indicators.
     * This method should be used before currentPagePosition is updated, when user is trying to
     * select a different page, i.e. onPageScrolled() is triggered.
     */
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
        if (currentPagePosition == 1 && !isToTheRight) { //Special case for first page to last page
            return ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition() - 1;
        } else if (currentPagePosition == ((LoopingViewPagerAdapter) getAdapter()).getLastItemPosition()
                && isToTheRight) { //Special case for last page to first page
            return 0;
        } else {
            return currentPagePosition + delta - 1;
        }
    }

    /**
     * A method that helps you integrate a ViewPager Indicator.
     * This method returns the expected count of indicators.
     */
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

    public void setIndicatorChangeListener(IndicatorChangeListener indicatorListener) {
        this.indicatorListener = indicatorListener;
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
                            setCurrentItem(itemCount - 2, false); //Real last item
                        } else if (index == itemCount - 1) {
                            setCurrentItem(1, false); //Real first item
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
