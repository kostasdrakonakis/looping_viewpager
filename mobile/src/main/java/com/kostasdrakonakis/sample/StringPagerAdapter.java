package com.kostasdrakonakis.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kostasdrakonakis.loopingviewpager.adapter.LoopingViewPagerAdapter;

public class StringPagerAdapter extends LoopingViewPagerAdapter<String> {

    StringPagerAdapter(Context context, String[] items, boolean isEndless) {
        super(context, items, isEndless);
    }

    @Override
    protected View inflateView(Context context, int viewType, ViewGroup container, int position) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.item_string, container, false);
    }

    @Override
    protected void bindView(View convertView, int position, int viewType) {
        TextView textView = convertView.findViewById(R.id.string_item);
        String color = getItem(position);
        textView.setText(color);
    }
}
