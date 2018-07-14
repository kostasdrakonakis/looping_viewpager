package com.kostasdrakonakis.loopingviewpager.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

public abstract class LoopingViewPagerAdapter<T> extends PagerAdapter {

    protected Context context;
    protected List<T> items;
    protected final SparseArray<View> viewCache = new SparseArray<>();

    protected boolean isEndless;
    protected boolean canInfinite = true;

    private boolean dataSetChangeLock = false;

    public LoopingViewPagerAdapter(Context context, T[] items, boolean isEndless) {
        this(context, Arrays.asList(items), isEndless);
    }

    public LoopingViewPagerAdapter(Context context, List<T> items, boolean isEndless) {
        this.context = context;
        this.isEndless = isEndless;
        setItems(items);
    }

    public void setItems(List<T> items) {
        this.items = items;
        canInfinite = items.size() > 1;
        notifyDataSetChanged();
    }

    protected abstract View inflateView(int viewType, ViewGroup container, int position);

    protected abstract void bindView(View convertView, int position, int viewType);

    public T getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        } else {
            return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int itemPosition = (isEndless && canInfinite) ? getItemPosition(position) : position;

        int viewType = getItemViewType(itemPosition);

        View convertView;
        if (viewCache.get(viewType, null) == null) {
            convertView = inflateView(viewType, container, itemPosition);
        } else {
            convertView = viewCache.get(viewType);
            viewCache.remove(viewType);
        }

        bindView(convertView, itemPosition, viewType);

        container.addView(convertView);

        return convertView;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        int itemPosition = (isEndless && canInfinite) ? getItemPosition(position) : position;

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
        int count = 0;
        if (items != null) count = items.size();

        return (isEndless && canInfinite) ? count + 2 : count;
    }

    protected int getItemViewType(int position) {
        return 0;
    }

    public int getListCount() {
        return items == null ? 0 : items.size();
    }

    private int getItemPosition(int position) {
        if (!(isEndless && canInfinite)) return position;
        int itemsPosition;
        if (position == 0) {
            itemsPosition = getCount() - 1 - 2; //First item is a dummy of last item
        } else if (position > getCount() - 2) {
            itemsPosition = 0; //Last item is a dummy of first item
        } else {
            itemsPosition = position - 1;
        }
        return itemsPosition;
    }

    public int getLastItemPosition() {
        if (isEndless) {
            return items == null ? 0 : items.size();
        } else {
            return items == null ? 0 : items.size() - 1;
        }
    }

    public boolean isEndless() {
        return this.isEndless;
    }
}
