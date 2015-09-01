package com.zhuang.library;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

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
    private ArrayList<ImageModel> imageList;//系统图片
    private ArrayList<ImageModel> selectImageList = new ArrayList<ImageModel>();//已选择的图片


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
        ArrayList<ImageModel> imageList = getImageList();
        setAdapter(new Adapter(context, imageList));
    }

    /**
     * 获取系统相册图片
     * @return
     */
    private ArrayList<ImageModel> getLocalImagesPath() {
        ArrayList<ImageModel> list = new ArrayList<ImageModel>();

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED, MediaStore.Images.Media.SIZE};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " desc";
        Cursor cursor = contentResolver.query(uri, projection, null, null, sortOrder);
        int iRemote = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast()) {
            String remote = cursor.getString(iRemote);
            ImageModel imageModel = new ImageModel(i,remote);
            list.add(imageModel);
            i++;
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    private class Adapter extends BaseAdapter {

        private ArrayList<ImageModel> images;
        private LayoutInflater layoutInflater;

        private ImageWork mImageWork;
        private int mImageThumbSize;

        public Adapter(Context context,ArrayList<ImageModel> images) {
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
            return images.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            final ImageModel imageModel = (ImageModel)getItem(i);
            String path = imageModel.getPath();
            boolean checked = imageModel.getChecked();
            if(view == null){
                viewHolder = new ViewHolder();
                view = layoutInflater.inflate(R.layout.imageview,null);
                viewHolder.imageView = (ImageView)view.findViewById(R.id.iv_imageView);
                viewHolder.checkBox = (CheckBox)view.findViewById(R.id.cb_imageview);
                view.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder)view.getTag();
            }
            viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
                    imageModel.setChecked(arg1);
                    if(!selectImageList.contains(imageModel)&&arg1){
                        addSelectImage(imageModel);
                    }
                }
            });
            viewHolder.checkBox.setChecked(checked);
            mImageWork.loadImage(path, viewHolder.imageView);
            return view;
        }

        class ViewHolder{
            ImageView imageView;
            CheckBox checkBox;
        }
    }

    public ArrayList<ImageModel> getImageList() {
        if(imageList == null){
            imageList = getLocalImagesPath();
        }
        return imageList;
    }

    public void setImageList(ArrayList<ImageModel> imageList) {
        this.imageList = imageList;
    }

    /**
     * 添加已选图片
     * @param imageModel
     */
    private void addSelectImage(ImageModel imageModel){
        selectImageList.add(imageModel);
    }

    /**
     * 获取已选择照片路径
     * @return
     */
    public ArrayList<ImageModel> getSelectImageList() {
        return selectImageList;
    }

    public Boolean getTakePhoto() {
        return takePhoto;
    }

    public void setTakePhoto(Boolean takePhoto) {
        this.takePhoto = takePhoto;
    }
}
