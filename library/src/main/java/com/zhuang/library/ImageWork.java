package com.zhuang.library;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

/**
 * Created by zhuang on 2015/8/21.
 * 图片加载类
 */
public class ImageWork {

    private Bitmap mLoadingBitmap;//图片正在加载时显示的图片
    protected Resources mResources;
    private ImageCache imageCache;//缓存

    protected int mImageWidth;//图片宽度
    protected int mImageHeight;//图片高度

    private final Object mPauseWorkLock = new Object();//锁
    protected boolean mPauseWork = false;//是否暂停加载图片的任务

    private static final int FADE_IN_TIME = 200;

    public ImageWork(Context context,ImageCache imageCache) {
        mResources = context.getResources();
        this.imageCache = imageCache;
    }

    public ImageWork(Context context) {
        mResources = context.getResources();
    }

    //获取图片并压缩
    public  Bitmap decodeBitmapFromResource(String path, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        //初始压缩比例
        options.inSampleSize = calculateBitmapSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bmp = BitmapFactory.decodeFile(path, options);
        // 如果运行在Honeycomb或者更高版本,使用inBitmap.
        if (Utils.hasHoneycomb()) {
            addInBitmapOptions(options);
        }
        return bmp;
    }

    private  void addInBitmapOptions(BitmapFactory.Options options) {
        // inBitmap only works with mutable bitmaps, so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;
        Bitmap inBitmap = null;
        // Try to find a bitmap to use for inBitmap.
        if(imageCache!=null){
            inBitmap = imageCache.getBitmapFromReusableSet(options);
        }

        if (inBitmap != null) {
            // If a suitable bitmap has been found, set it as the value of
            // inBitmap.
            options.inBitmap = inBitmap;
        }
    }

    //计算压缩比例
    public static int calculateBitmapSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // BEGIN_INCLUDE (calculate_sample_size)
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

           /* final int halfHeight = height / 2;
            final int halfWidth = width / 2;*/

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((height / inSampleSize) > reqHeight
                    && (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
        // END_INCLUDE (calculate_sample_size)
    }

    //根据路径获取本地图片并压缩
    //使用弱引用，不阻止垃圾回收机制回收imageview，因为有可能task还未完成时，imageview已经不存在我们屏幕中，
    //如果task持有imageview的强引用，那么会导致垃圾回收机制不回收该imageview，
    class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {
        private String mPath;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(String path, ImageView imageView) {
            mPath = path;
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        // Decode image in background.
        @Override
        protected BitmapDrawable doInBackground(String... params) {

            Bitmap bitmap = null;
            BitmapDrawable drawable = null;

            // Wait here if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {}
                }
            }

            //如果task未取消，并且task对应的imagewview所对应的task和当前task是一样的
            //则继续执行任务，获取图片
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null) {
                 bitmap = decodeBitmapFromResource(mPath, mImageWidth, mImageHeight);
            }

           //Bitmap转换成BitmapDrawable，并加入缓存
            if (bitmap != null) {
                //根据不同的版本设置不同的BitmapDrawable，因为不同版本的缓存处理策略不一样
                //可根据不同版本优化缓存
                if (Utils.hasHoneycomb()) {
                    // 如果是Honeycomb及更高版本，使用BitmapDrawable
                    drawable = new BitmapDrawable(mResources, bitmap);
                } else {
                    // 如果是Gingerbread及更低版本,使用RecyclingBitmapDrawable
                    drawable = new RecyclingBitmapDrawable(mResources, bitmap);
                }
                if(imageCache!=null){
                    imageCache.addBitmapToMemCache(mPath, drawable);
                }
            }
            return drawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable value) {
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled()) {
                value = null;
            }

            //再次半点task和imageview的对应关系是否存在
            final ImageView imageView = getAttachedImageView();
            if (value != null && imageView != null) {
                setImageDrawable(imageView, value);
            }
        }

        @Override
        protected void onCancelled(BitmapDrawable value) {
            super.onCancelled(value);
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
        }

        /**
         * 获取imageview所对应的task，判断该task是否和当前task一致，如果一直证明imageview和task有对应关系
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }

    }

    /**
     * Called when the processing is complete and the final drawable should be
     * set on the ImageView.
     *
     * @param imageView
     * @param drawable
     */
    private void setImageDrawable(ImageView imageView, Drawable drawable) {
        final TransitionDrawable td =
                new TransitionDrawable(new Drawable[] {
                        new ColorDrawable(mResources.getColor(android.R.color.transparent)),
                        drawable
                });
        // Set background to loading bitmap
        imageView.setBackgroundDrawable(
                new BitmapDrawable(mResources, mLoadingBitmap));

        imageView.setImageDrawable(td);
        td.startTransition(FADE_IN_TIME);
    }


    //加载图片，缓存有则从缓存加载，没有则开启任务后台加载
    public void loadImage(String path, ImageView imageView) {
        if (path == null || path.equals("")) {
            return;
        }
        BitmapDrawable bitmap = null;
        if(imageCache!=null){
            bitmap = imageCache.getBitmapFromMemCache(path);
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
            imageView.setImageDrawable(bitmap);
        } else if (cancelPotentialWork(path,imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(path,imageView);
            final AsyncDrawable asyncDrawable = new AsyncDrawable(mResources, mLoadingBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR);
        }
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    public static boolean cancelPotentialWork(String path, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.mPath;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null|| !bitmapData.equals(path)) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    /**
     * Cancels any pending work attached to the provided ImageView.
     * @param imageView
     */
    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
        }
    }

    /**
     * Set the target image width and height.
     *
     * @param width
     * @param height
     */
    public void setImageSize(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
    }

    /**
     * Set the target image size (width and height will be the same).
     *
     * @param size
     */
    public void setImageSize(int size) {
        setImageSize(size, size);
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param bitmap
     */
    public void setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     *
     * @param resId
     */
    public void setLoadingImage(int resId) {
        mLoadingBitmap = BitmapFactory.decodeResource(mResources, resId);
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    public void setImageCache(ImageCache imageCache) {
        this.imageCache = imageCache;
    }


}
