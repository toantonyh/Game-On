package com.example.ttpm.game_on.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.SplashActivity;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * A simple {@link Fragment} subclass.
 */
public class UserProfileFragment extends android.support.v4.app.Fragment {

    private static int PICK_IMAGE_REQUEST = 1;
    private static int REQUEST_CAMERA = 0;
    private static int SELECT_FILE = 1;

    protected TextView mWelcomeMessageTextView;
    protected ParseImageView mProfileImageView;
    protected ImageButton mPictureChangeButton;

    public UserProfileFragment() {
    }

    public static UserProfileFragment newInstance() {
        return new UserProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);

        mWelcomeMessageTextView = (TextView) view.findViewById(R.id.user_profile_welcome_message);
        mWelcomeMessageTextView.setText(ParseUser.getCurrentUser().getUsername());

        mProfileImageView = (ParseImageView)
                view.findViewById(R.id.user_profile_profile_parse_image_view);
        ParseFile currentObject = ParseUser.getCurrentUser().getParseFile("profilePicture");
        mProfileImageView.setParseFile(currentObject);
        mProfileImageView.loadInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] data, ParseException e) {
            }
        });

        mPictureChangeButton = (ImageButton)
                view.findViewById(R.id.user_profile_change_profile_picture_button);
        mPictureChangeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if(requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data);
            } else if (requestCode == SELECT_FILE) {
                onSelectFromGalleryResult(data);
            }
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Gallery", "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle("Upload Profile Picture");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (items[which].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[which].equals("Choose from Gallery")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"),
                            SELECT_FILE);
                } else if (items[which].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void onCaptureImageResult(Intent data) {
        // Finds image stored after camera picture taken
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        // Converts image to bytes
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // Compress image to lower quality scale 1 - 100
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        // Save and store image to phone directory
        File destination = new File(Environment.getExternalStorageDirectory(),
                System.currentTimeMillis() + ".jpg");
        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        uploadToParse(bytes);
        mProfileImageView.setImageBitmap(thumbnail);
    }

    private void onSelectFromGalleryResult(Intent data) {
        Uri selectedImageUri = data.getData();
        String[] projection = { MediaStore.MediaColumns.DATA };
        CursorLoader cursorLoader = new CursorLoader(this.getActivity(), selectedImageUri, projection,
                null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();

        String selectedImagePath = cursor.getString(column_index);

        Bitmap bm;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(selectedImagePath, options);
        final int REQUIRED_SIZE = 200;
        int scale = 1;
        while(options.outWidth / scale / 2 >= REQUIRED_SIZE &&
                options.outHeight / scale / 2 >= REQUIRED_SIZE) {
            scale *= 2;
        }
        options.inSampleSize = scale;
        options.inJustDecodeBounds = false;
        bm = BitmapFactory.decodeFile(selectedImagePath, options);

        mProfileImageView.setImageBitmap(bm);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        uploadToParse(bytes);
    }

    private void uploadToParse(ByteArrayOutputStream stream) {
        byte[] image = stream.toByteArray();

        // Create the ParseFile
        ParseFile file = new ParseFile(System.currentTimeMillis() + ".jpg", image);
        // Upload the image to Parse
        file.saveInBackground();

        // Retrieve current user
        ParseUser currentUser = ParseUser.getCurrentUser();
        // Finds profilePicture column in User table and inserts the image
        currentUser.put("profilePicture", file);
        // Save the user
        currentUser.saveInBackground();

        Toast.makeText(this.getActivity(), "Image uploaded!",
                Toast.LENGTH_SHORT).show();
    }
}

