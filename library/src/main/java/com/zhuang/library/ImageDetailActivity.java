package com.zhuang.library;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class ImageDetailActivity extends FragmentActivity implements View.OnClickListener{

    private ImagePagerAdapter mAdapter;
    private ViewPager mPager;
    private ArrayList<String> imageList;
    private ImageWork mImageWork;
    private LinearLayout lv_actionbar;
    private boolean isShowActionBar = true;
    private  Animation sIn;
    private  Animation sOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        lv_actionbar = (LinearLayout)findViewById(R.id.lv_actionbar);

        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // 屏幕宽度（像素）
        int height = metric.heightPixels;  // 屏幕高度（像素）
        mImageWork = new ImageWork(this);
        mImageWork.setImageSize(width, height);

        Intent intent = getIntent();
        imageList = intent.getStringArrayListExtra("imageList");
        int position = intent.getIntExtra("position", 0);
        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(),imageList.size());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(2);
        mPager.setCurrentItem(position);

        // Set up activity to go full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sIn = new ScaleAnimation(1f, 1f, 0f, 1f);
        sIn.setDuration(300);
        sIn.setFillAfter(true);

        sOut = new ScaleAnimation(1f, 1f, 1f, 0f);
        sOut.setDuration(300);
        sOut.setFillAfter(true);

    }

    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private final int mSize;

        public ImagePagerAdapter(FragmentManager fm, int size) {
            super(fm);
            mSize = size;
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Fragment getItem(int position) {
            String path = imageList.get(position);
            return ImageDetailFragment.newInstance(path);
        }
    }

    public ImageWork getmImageWork() {
        return mImageWork;
    }

    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onClick(View v) {
        if(isShowActionBar){
            lv_actionbar.startAnimation(sOut);
            isShowActionBar = false;
        }else{
            lv_actionbar.startAnimation(sIn);
            isShowActionBar = true;
        }

    }
}
