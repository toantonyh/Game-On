package com.example.ttpm.game_on.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.ttpm.game_on.SingleImageBitmapWorkerTask;

import java.io.File;

/**
 * Created by Tony on 3/10/2016.
 */
public class SingleImageActivity extends Activity {

    private static final String IMAGE_FILE_LOCATION = "image_file_location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        // Creating a new RelativeLayout
        RelativeLayout relativeLayout = new RelativeLayout(this);
        // Defining RelativeLayout layout params, fill its parent
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);

        // Creating a new ImageView
        ImageView imageView = new ImageView(this);
        // Defining layout params of ImageView
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
        // Set params on the ImageView
        imageView.setLayoutParams(params);
        // Adding ImageView to the RelativeLayout as a child
        relativeLayout.addView(imageView);

        // Creating a new Button
        Button useAsProfilePicButton = new Button(this);
        // Setting text of button
        useAsProfilePicButton.setText("Use As Profile Picture");
        // Defining layout params of Button
        RelativeLayout.LayoutParams bp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        bp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        // Set params on the Button
        useAsProfilePicButton.setLayoutParams(bp);
        // Adding the Button to the RelativeLayout as a child
        relativeLayout.addView(useAsProfilePicButton);

        // Setting RelativeLayout as content view
        setContentView(relativeLayout, rlp);

        File imageFile = new File(getIntent().getStringExtra(IMAGE_FILE_LOCATION));

        SingleImageBitmapWorkerTask workerTask =
                new SingleImageBitmapWorkerTask(imageView, width, height);
        workerTask.execute(imageFile);
    }
}
