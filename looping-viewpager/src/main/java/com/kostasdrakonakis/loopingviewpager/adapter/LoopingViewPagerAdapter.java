package com.kostasdrakonakis.loopingviewpager.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class LoopingViewPagerAdapter<T> extends PagerAdapter {

    private Context context;
    private final List<T> items = new ArrayList<>();
    private final SparseArray<View> viewCache = new SparseArray<>();

    private boolean isEndless;
    private boolean isInfinite = true;

    private boolean dataSetChangeLock = false;

    public LoopingViewPagerAdapter(Context context) {
        this.context = context;
    }

    public LoopingViewPagerAdapter(Context context, T[] items, boolean isEndless) {
        this(context, Arrays.asList(items), isEndless);
    }

    public LoopingViewPagerAdapter(Context context, T[] items) {
        this(context, Arrays.asList(items), false);
    }

    public LoopingViewPagerAdapter(Context context, List<T> items) {
        this(context, items, false);
    }

    public LoopingViewPagerAdapter(Context context, List<T> items, boolean isEndless) {
        this.context = context;
        this.isEndless = isEndless;
        setItems(items);
    }

    public void setItems(T[] items) {
        setItems(Arrays.asList(items));
    }

    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        isInfinite = items.size() > 1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        int itemPosition = (isEndless && isInfinite) ? getItemPosition(position) : position;

        int viewType = getItemViewType(itemPosition);

        View convertView;
        if (viewCache.get(viewType, null) == null) {
            convertView = inflateView(context, viewType, container, itemPosition);
        } else {
            convertView = viewCache.get(viewType);
            viewCache.remove(viewType);
        }

        bindView(convertView, itemPosition, viewType);

        container.addView(convertView);

        return convertView;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        int itemPosition = (isEndless && isInfinite) ? getItemPosition(position) : position;

        container.removeView((View) object);
        if (!dataSetChangeLock) viewCache.put(getItemViewType(itemPosition), (View) object);
    }

    @Override
    public void notifyDataSetChanged() {
        dataSetChangeLock = true;
        super.notifyDataSetChanged();
        dataSetChangeLock = false;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public int getCount() {
        int count = items.size();
        return (isEndless && isInfinite) ? count + 2 : count;
    }

    public T getItem(int position) {
        return position >= 0 && position < items.size() ? items.get(position) : null;
    }

    public boolean isEndless() {
        return this.isEndless;
    }

    protected abstract View inflateView(Context context, int viewType, ViewGroup container, int position);

    protected abstract void bindView(View convertView, int position, int viewType);

    protected int getItemViewType(int position) {
        return 0;
    }

    protected Context getContext() {
        return context;
    }

    protected boolean isInfinite() {
        return isInfinite;
    }

    protected List<T> getItems() {
        return items;
    }

    protected SparseArray<View> getViewCache() {
        return viewCache;
    }

    public int getListCount() {
        return items.size();
    }

    public int getLastItemPosition() {
        return isEndless ? items.size() : items.size() - 1;
    }

    private int getItemPosition(int position) {
        if (!(isEndless && isInfinite)) return position;
        int itemsPosition;
        if (position == 0) {
            itemsPosition = getCount() - 1 - 2;
        } else if (position > getCount() - 2) {
            itemsPosition = 0;
        } else {
            itemsPosition = position - 1;
        }
        return itemsPosition;
    }
}
