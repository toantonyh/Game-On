package com.example.ttpm.game_on.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttpm.game_on.models.GameOnSession;
import com.example.ttpm.game_on.QueryPreferences;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.HomePagerActivity;
import com.example.ttpm.game_on.activities.SessionActivity;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tri Han on 2/10/2016.
 */
public class SessionFragment extends VisibleFragment {

    private static final String ARG_SESSION_ID = "session_id";
    private static final String TAG = "ERROR";

    private GameOnSession mCurrentGameOnSession;
    ParseUser HostOfTheGame = new ParseUser();
    private boolean mCurrentUserIsHost;

    private LinearLayout mSessionInfoOutput;
    private TextView mTimerTextView;
    private Button mConfirmButton;
    private Button mLeaveButton;

    private CountDownTimer mCountDownTimer;

    public static SessionFragment newInstance(UUID sessionId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_SESSION_ID, sessionId);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_session, container, false);

        ParseQuery<GameOnSession> query = GameOnSession.getQuery();
        query.whereEqualTo("objectId", QueryPreferences.getStoredSessionId(getActivity()));
        query.include("host");
        query.findInBackground(new FindCallback<GameOnSession>() {
            @Override
            public void done(List<GameOnSession> list, ParseException e) {
                if (e == null) {

                    //Set the host of the game session to the pointer of the User of this session
                    HostOfTheGame = list.get(0).getParseUser("host");

                    mCurrentGameOnSession = list.get(0);
                    mCurrentUserIsHost = (mCurrentGameOnSession.getHost().getObjectId() == ParseUser.getCurrentUser().getObjectId());

                    mSessionInfoOutput = (LinearLayout) view.findViewById(R.id.sessionInfoOutputArea);

                    displayGameTitle(mSessionInfoOutput);
                    displaySessionHost(mSessionInfoOutput);
                    displaySessionParticipants(mSessionInfoOutput);

                    mTimerTextView = (TextView) view.findViewById(R.id.timerTextView);
                    if (!mCurrentUserIsHost) {
                        mTimerTextView.setText(R.string.message_for_participant);
                    } else {
                        startTimer();
                    }

                    mConfirmButton = (Button) view.findViewById(R.id.confirmButton);
                    mConfirmButton.setOnClickListener(new View.OnClickListener() {
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

                    if (!mCurrentUserIsHost) {
                        mConfirmButton.setVisibility(View.GONE);
                    }

                    mLeaveButton = (Button) view.findViewById(R.id.leaveButton);
                    mLeaveButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            performLeaveActions();
                        }
                    });
                } else {
                    // It could be either because the session no longer exists or
                    // there really was an error when trying to query Parse.
                    // for now, we can just assume that the session no longer exists.
                    // however, in the future, we will have to do some checking.

                    // another solution for now is to use call performActionBasedOnSessionCancelled here.
                    Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void displayGameTitle(LinearLayout displayArea) {
        TextView gameTitleTextView = new TextView(getActivity());
        gameTitleTextView.append("Game Title: " + mCurrentGameOnSession.getGameTitle() + "\n");
        displayArea.addView(gameTitleTextView);
    }

    private void displaySessionHost(LinearLayout displayArea) {
       TextView hostNameTextView = new TextView(getActivity());
        hostNameTextView.append("Host: " + HostOfTheGame.getUsername());
        displayArea.addView(hostNameTextView);

    }

    private void displaySessionParticipants(LinearLayout displayArea) {
        JSONArray participants = mCurrentGameOnSession.getParticipants();
        int length = participants.length();

        try {
            for (int i = 0; i < length; i++) {
                int participantNum = i + 1;
                TextView participantNameTextView = new TextView(getActivity());
                participantNameTextView.setText("Participant " + participantNum + ": " + participants.getString(i));
                displayArea.addView(participantNameTextView);
            }
        } catch (org.json.JSONException e) {
            e.getStackTrace();
        }
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
                                performLeaveActions();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        mCountDownTimer.start();
    }

    private void performLeaveActions() {
        removeCurrentUserFromSession();
        sendUserBackToHomePagerActivity();
    }

    private void removeCurrentUserFromSession() {
        if (mCurrentUserIsHost) {
            mCurrentGameOnSession.deleteInBackground();
        } else {
            try {
                mCurrentGameOnSession.removeParticipant(ParseUser.getCurrentUser().getObjectId());
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

}
