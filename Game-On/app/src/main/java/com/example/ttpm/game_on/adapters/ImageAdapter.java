package com.example.ttpm.game_on.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.ttpm.game_on.BitmapWorkerTask;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.CameraActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Tony on 3/9/2016.
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private static int mImageWidth;
    private static int mImageHeight;

    private File[] mImageFiles;
    private Bitmap placeHolderBitmap;

    // Class that holds a reference to the BitmapWorkerTask object
    public static class AsyncDrawable extends BitmapDrawable {
        final WeakReference<BitmapWorkerTask> taskReference;

        public AsyncDrawable(Resources resources,
                             Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(resources, bitmap);
            taskReference = new WeakReference(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return taskReference.get();
        }
    }

    public ImageAdapter(File[] folderFiles, int imageWidth, int imageHeight) {
        mImageFiles = folderFiles;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        // programmatically create the xml layout file instead of inflating one
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mImageWidth, mImageHeight);
        imageView.setLayoutParams(params);

        return new ViewHolder(imageView, mImageFiles);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        File imageFile = mImageFiles[position];

        Glide.with(holder.getImageView().getContext())
                .load(imageFile)
                .centerCrop()
                .into(holder.getImageView());

        /*Bitmap bitmap = CameraActivity.getBitmapFromMemoryCache(imageFile.getName());
        if(bitmap != null) {
            holder.getImageView().setImageBitmap(bitmap);
        } else if(checkBitmapWorkerTask(imageFile, holder.getImageView())) {
            BitmapWorkerTask bitmapWorkerTask = new BitmapWorkerTask(holder.getImageView(),
                    mImageWidth, mImageHeight);
            AsyncDrawable asyncDrawable = new AsyncDrawable(holder.getImageView().getResources(),
                    placeHolderBitmap,
                    bitmapWorkerTask);
            holder.getImageView().setImageDrawable(asyncDrawable);
            bitmapWorkerTask.execute(imageFile);
        }*/
    }

    @Override
    public int getItemCount() {
        return mImageFiles.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private File[] files;

        public ViewHolder(View view, File[] imageFiles) {
            super(view);
            files = imageFiles;

            imageView = (ImageView) view;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("GAMEONSESSION", "pic: " + files[getAdapterPosition()]);
                }
            });
        }

        public ImageView getImageView() {
            return imageView;
        }
    }

    public static boolean checkBitmapWorkerTask(File imageFile, ImageView imageView) {
        BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if(bitmapWorkerTask != null) {
            final File workerFile = bitmapWorkerTask.getImageFile();
            if(workerFile != null) {
                if(workerFile != imageFile) {
                    bitmapWorkerTask.cancel(true);
                } else {
                    // bitmapworkertask file is the same as the imageview is expecting
                    // so do nothing
                    return false;
                }
            }
        }
        return true;
    }

    public static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        Drawable drawable = imageView.getDrawable();
        if(drawable instanceof AsyncDrawable) {
            AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
            return asyncDrawable.getBitmapWorkerTask();
        }
        return null;
    }
}
