package com.kostasdrakonakis.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.kostasdrakonakis.loopingviewpager.LoopingViewPager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final StringPagerAdapter adapter = new StringPagerAdapter(this, getResources().getStringArray(R.array.colors), true);
        LoopingViewPager viewPager = findViewById(R.id.viewpager);
        viewPager.setAdapter(adapter);
    }
}
