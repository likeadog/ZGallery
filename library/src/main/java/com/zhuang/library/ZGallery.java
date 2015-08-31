package com.zhuang.library;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhuang on 2015/8/25.
 */
public class ZGallery extends GridView {

    private Context mContext;
    private Boolean takePhoto = true;
    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ArrayList<String> imageList;
    private ArrayList<String> selectImagePathList = new ArrayList<String>();//已选择的图片路径


    public ZGallery(Context context) {
        super(context);
        init(context);
    }

    public ZGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZGallery(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void init(Context context){
        mContext = context;
        mImageThumbSize = context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
        setColumnWidth(mImageThumbSize);
        setNumColumns(AUTO_FIT);
        setStretchMode(STRETCH_COLUMN_WIDTH);
        setHorizontalSpacing(mImageThumbSpacing);
        setVerticalSpacing(mImageThumbSpacing);
        imageList = getImages();
        setAdapter(new Adapter(context, imageList));
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View v, int i, long l) {
                Intent intent = new Intent(mContext, ImageDetailActivity.class);
                intent.putStringArrayListExtra("imageList", imageList);
                intent.putExtra("position", i);
                if (Utils.hasJellyBean()) {
                    // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
                    // show plus the thumbnail image in GridView is cropped. so using
                    // makeScaleUpAnimation() instead.
                    ActivityOptions options =
                            ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
                    mContext.startActivity(intent, options.toBundle());
                } else {
                    mContext.startActivity(intent);
                }
            }
        });
    }

    private ArrayList<String> getImages() {
        ArrayList<String> list = new ArrayList<String>();

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " desc";
        Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder);
        int iRemote = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String remote = cursor.getString(iRemote);
            list.add(remote);
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    private class Adapter extends BaseAdapter {

        private List<String> images;
        private LayoutInflater layoutInflater;

        private ImageWork mImageWork;
        private int mImageThumbSize;

        public Adapter(Context context,List<String> images) {
            this.images = images;
            layoutInflater = LayoutInflater.from(context);
            mImageThumbSize = context.getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
            mImageWork = new ImageWork(context,new ImageCache(4));
            mImageWork.setImageSize(mImageThumbSize);
            mImageWork.setLoadingImage(R.drawable.empty_photo);
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int i) {
            String path = images.get(i);
            return path;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            String path = (String)getItem(i);
            if(view == null){
                viewHolder = new ViewHolder();
                view = layoutInflater.inflate(R.layout.imageview,null);
                viewHolder.imageView = (ImageView)view.findViewById(R.id.iv_imageView);
                view.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder)view.getTag();
            }

            mImageWork.loadImage(path, viewHolder.imageView);
            return view;
        }

        class ViewHolder{
            ImageView imageView;
        }
    }

    /**
     * 添加已选图片路径
     * @param path
     */
    public void addSelectImagePath(String path){
        selectImagePathList.add(path);
    }

    /**
     * 获取已选择照片路径
     * @return
     */
    public ArrayList<String> getSelectImagePathList() {
        return selectImagePathList;
    }

    public Boolean getTakePhoto() {
        return takePhoto;
    }

    public void setTakePhoto(Boolean takePhoto) {
        this.takePhoto = takePhoto;
    }
}
