package com.example.ttpm.game_on.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.ttpm.game_on.QueryPreferences;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.HomePagerActivity;
import com.example.ttpm.game_on.activities.SessionActivity;
import com.example.ttpm.game_on.adapters.PlayerAdapter;
import com.example.ttpm.game_on.models.GameOnSession;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tri Han on 2/10/2016.
 */
public class SessionFragment extends VisibleFragment {

    private static final String ARG_CURRENT_LOCATION = "com.example.ttpm.game_on.current_location";

    private static final String TAG = "ERROR";

    ParseUser sessionHostName = new ParseUser();
    private boolean mUserIsHost;

    private GameOnSession mCurrentGameOnSession;
    private GridView mPlayerGrid;
    private TextView mTimerTextView;
    private TextView mNoPlayersFoundTextView;
    private Button mHostStartButton;
    private Button mLeaveButton;
    private TextView mBoardGameTextView;
    private ImageView mBoardGameImageView;

    private GoogleMap mMap;
    private MapView mapView;
    private Location mCurrentLocation;
    CameraUpdate cu;

    LatLngBounds.Builder builder;

    private CountDownTimer mCountDownTimer;

    public static SessionFragment newInstance(Location currentLocation) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CURRENT_LOCATION, currentLocation);

        SessionFragment fragment = new SessionFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void performActionBasedOnSessionUpdated() {
        // Refreshing the session page. NOTE (2/13/2016): NOT SURE IF THIS WORKS YET!
        Fragment newFragment = new SessionFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        transaction.replace(R.id.fragment_container, newFragment);

        transaction.commit();
    }

    @Override
    protected void performActionBasedOnSessionCancelled() {
        // 1. display cancellation message
        // 2. Clear session id in shared preferences
        // 3. Redirect user back to HomeActivity.
        // NOTE (2/13/2016): NOT SURE IF THIS WORKS YET!

        ((SessionActivity)getActivity()).showDialog();
        // The line above is the replacement for the Toast line below. However, not sure if it works yet.
//        Toast.makeText(getActivity(), "Session has been cancelled", Toast.LENGTH_LONG).show();
        QueryPreferences.setStoredSessionId(getActivity(), null);
        Intent intent = HomePagerActivity.newIntent(getActivity());
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentLocation = (Location) getArguments().getParcelable(ARG_CURRENT_LOCATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_session, container, false);

        newMapView(view, savedInstanceState);

        ParseQuery<GameOnSession> query = GameOnSession.getQuery();
        query.whereEqualTo("objectId", QueryPreferences.getStoredSessionId(getActivity()));
        query.findInBackground(new FindCallback<GameOnSession>() {
            @Override
            public void done(List<GameOnSession> list, ParseException e) {
                if (e == null) {
                    mCurrentGameOnSession = list.get(0);
                    try {
                        sessionHostName = mCurrentGameOnSession.getParseUser("host").fetchIfNeeded();
                    } catch (ParseException ex) {
                        Log.e("GAMEONSESSION", "session fetchifneeded: " + ex.toString());
                    }

                    mUserIsHost = mCurrentGameOnSession.getHost().getObjectId()
                            .equals(ParseUser.getCurrentUser().getObjectId());

                    // Display grid of players in session, if players, display
                    // else, show no players message
                    // Todo: does noPlayerFoundTextView display after refreshing and finding no players?
                    mNoPlayersFoundTextView = (TextView)
                            view.findViewById(R.id.session_no_players_found);
                    if(mCurrentGameOnSession.getAllPlayers().length() != 0) {
                        mNoPlayersFoundTextView.setVisibility(View.GONE);
                        mPlayerGrid = (GridView) view.findViewById(R.id.session_participant_container);
                        mPlayerGrid.setAdapter(new PlayerAdapter(getContext(), mCurrentGameOnSession));
                        mPlayerGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                Toast.makeText(getContext(), mCurrentGameOnSession.getPlayer(position), Toast.LENGTH_SHORT).show();
                                // Todo: grab parseuser with objectid and display user info onclick
                            }
                        });
                    } else {
                        mNoPlayersFoundTextView.setVisibility(View.VISIBLE);
                    }

                    // Display board game name
                    mBoardGameTextView = (TextView) view.findViewById(R.id.session_game_game_name);
                    mBoardGameTextView.setText(mCurrentGameOnSession.getGameTitle());

                    // Display host name
                    TextView hostNameTextView = (TextView) view.findViewById(R.id.session_game_host_name);
                    Resources res = getResources();
                    String hostSessionTag = res.getString(R.string.session_host_tag)
                            + " "
                            + sessionHostName.getUsername();
                    hostNameTextView.setText(hostSessionTag);

                    // Display timer countdown
                    mTimerTextView = (TextView) view.findViewById(R.id.timerTextView);
                    if (!mUserIsHost) {
                        mTimerTextView.setText(R.string.time_left);
                    } else {
                        startTimer();
                    }

                    // Display host start button
                    mHostStartButton = (Button) view.findViewById(R.id.session_host_start_button);
                    mHostStartButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mCountDownTimer.cancel();
                            // user has confirmed to start session. do appropriate actions here.
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.session_confirmed_message)
                                    .setTitle(R.string.session_confirmed_title)
                                    .setPositiveButton(R.string.dialog_confirmed_button_text, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            mCurrentGameOnSession.setOpenStatus(false);
                                            mCurrentGameOnSession.saveInBackground();
                                            QueryPreferences.setStoredSessionId(getActivity(), null);
                                            sendUserBackToHomePagerActivity();
                                        }
                                    });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });

                    // Hide host start button if user isn't host
                    if (!mUserIsHost) {
                        mHostStartButton.setVisibility(View.GONE);
                    }

                    // Display leave button
                    mLeaveButton = (Button) view.findViewById(R.id.leaveButton);
                    mLeaveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            leaveSession();
                        }
                    });

                    mBoardGameImageView = (ImageView) view.findViewById(R.id.session_game_image_view);
                    loadBoardImage();
                } else {
                    Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        if(mCurrentGameOnSession != null) {
            updateUserMarker();
        }

        return view;
    }

    // Todo: Need to perform loading board images asynchronously
    private void loadBoardImage() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("BoardGames");
        query.whereEqualTo("boardName", mBoardGameTextView.getText().toString());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    ParseFile picture = objects.get(0).getParseFile("gameLogo");
                    if(picture != null) {
                        // If board game has image, load image
                        final String pictureName = picture.getName();

                        picture.getDataInBackground(new GetDataCallback() {
                            @Override
                            public void done(byte[] data, ParseException e) {
                                if (e == null) {
                                    if (data.length == 0) {
                                        Log.d("GAMEON", "Data found, but nothing to extract");
                                        return;
                                    }
                                    Log.d("GAMEON", "Host board image found!");

                                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), pictureName);
                                    try {
                                        OutputStream os = new FileOutputStream(file);
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                                        os.flush();
                                        os.close();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }

                                    Glide.with(getContext())
                                            .load(file)
                                            .into(mBoardGameImageView);
                                } else {
                                    Log.d("GAMEON", "Parsefile contains no data");
                                }
                            }
                        });
                    } else {
                        // If board game has no image, load a placeholder image
                        Log.d("GAMEON", "no pic");
                        Glide.clear(mBoardGameImageView);
                    }
                } else {
                    Log.d("GAMEON", "loadBoardImage ParseException");
                }
            }
        });
    }

    private void newMapView(View view, Bundle savedInstanceState) {
        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) view.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        mMap = mapView.getMap();
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setMyLocationEnabled(true);

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        MapsInitializer.initialize(getActivity());

        // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(37.3353, -121.8813), 15);
        mMap.animateCamera(cameraUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void startTimer() {

        long thirtyMinutes = 1000 * 60 * 30; // (1000 milliseconds/sec * 60 sec/min * 30 min)
        long minute = 1000 * 60;
        long tickLength = 1000; // 1 second

        mCountDownTimer = new CountDownTimer(thirtyMinutes, tickLength) {
            public void onTick(long millisUntilFinished) {
                mTimerTextView.setText("" + String.format("%d:%d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }

            public void onFinish() {
                mTimerTextView.setText(R.string.timed_out_text);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.session_timed_out_message)
                        .setTitle(R.string.session_timed_out_title)
                        .setPositiveButton(R.string.dialog_timed_out_button_text, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                leaveSession();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        mCountDownTimer.start();
    }

    private void leaveSession() {
        removeCurrentUserFromSession();
        sendUserBackToHomePagerActivity();
    }

    private void removeCurrentUserFromSession() {
        if (mUserIsHost) {
            mCurrentGameOnSession.deleteInBackground();
        } else {
            try {
                mCurrentGameOnSession.removePlayer(ParseUser.getCurrentUser().getObjectId());
                mCurrentGameOnSession.saveInBackground();
            } catch (JSONException e) {
                Log.d(TAG, "JSONException occurred: " + e);
            }
        }
        QueryPreferences.setStoredSessionId(getActivity(), null);
    }

    private void sendUserBackToHomePagerActivity() {
        Intent intent = new Intent(getActivity(), HomePagerActivity.class);
        startActivity(intent);
    }

    private void updateHostMarker()
    {
        /*ParseGeoPoint point = mCurrentGameOnSession.getLocation();


            Marker marker = mMap.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(point.getLatitude(), point.getLongitude()))
                            .title("Host")
            ); */
    }

    private void updateUserMarker()
    {
        Marker marker1 = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                        .title("You")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );

        ParseGeoPoint point = mCurrentGameOnSession.getLocation();

        Marker marker = mMap.addMarker(
                new MarkerOptions()
                        .position(new LatLng(point.getLatitude(), point.getLongitude()))
                        .title("Host")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        );

        List<Marker> markersList = new ArrayList<Marker>();
        markersList.add(marker1);
        markersList.add(marker);

        builder = new LatLngBounds.Builder();
        for (Marker m : markersList) {
            builder.include(m.getPosition());
        }

        int padding = 50;
        /**create the bounds from latlngBuilder to set into map camera*/
        LatLngBounds bounds = builder.build();
        /**create the camera with bounds and padding to set into map*/
        cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        /**call the map call back to know map is loaded or not*/
        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                /**set animated zoom camera into map*/
                mMap.animateCamera(cu);

            }
        });
    }
}
