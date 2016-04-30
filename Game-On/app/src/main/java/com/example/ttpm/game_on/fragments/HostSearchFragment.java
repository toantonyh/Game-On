package com.example.ttpm.game_on.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.ttpm.game_on.interfaces.SwipeFragmentUpdateInterface;
import com.example.ttpm.game_on.models.GameOnSession;
import com.example.ttpm.game_on.QueryPreferences;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.SessionActivity;
import com.example.ttpm.game_on.models.BoardGame;
import com.example.ttpm.game_on.models.BoardGameCollection;
import com.gc.materialdesign.views.ButtonRectangle;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 */
public class HostSearchFragment extends android.support.v4.app.Fragment
        implements SearchView.OnQueryTextListener, SwipeFragmentUpdateInterface {
    private static final String TAG = "HostSearchFragment";
    private static final String ARG_CURRENT_LOCATION = "com.example.ttpm.game_on.current_location";

    private RecyclerView mSearchRecyclerView;
    private HostSearchAdapter mSearchAdapter;
    private List<BoardGame> mBoardGames;

    private Location mCurrentLocation;
    private boolean isHost;

    public HostSearchFragment() {
    }

    public static HostSearchFragment newInstance(Location currentLocation)
    {
        Bundle args = new Bundle();
        args.putParcelable(ARG_CURRENT_LOCATION, currentLocation);

        HostSearchFragment fragment = new HostSearchFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mCurrentLocation = (Location) getArguments().getParcelable(ARG_CURRENT_LOCATION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host_search, container, false);
        mSearchRecyclerView = (RecyclerView) view.findViewById(R.id.host_search_recycler_view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(QueryPreferences.getStoredSessionId(getContext()) != null) {
            TextView sessionWarningTextView = (TextView) view.findViewById(R.id.host_search_session_warning);
            sessionWarningTextView.setVisibility(View.VISIBLE);
        }
        
        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        BoardGameCollection boardGameCollection = new BoardGameCollection();
        mBoardGames = boardGameCollection.getBoardGames();
        queryForBoardGames();

        mSearchAdapter = new HostSearchAdapter(getActivity(), mBoardGames);
        mSearchRecyclerView.setAdapter(mSearchAdapter);
    }

    public void onUpdateView() {
        Log.d("GAMEON", "hostSearch onUpdateView");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_host_search, menu);

        MenuItem item = menu.findItem(R.id.menu_item_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        List<BoardGame> filteredBoardGameList = filter(mBoardGames, newText);
        mSearchAdapter.animateTo(filteredBoardGameList);
        mSearchRecyclerView.scrollToPosition(0);
        return true;
    }

    private void queryForBoardGames() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("BoardGames");
        query.orderByAscending("boardName");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject boardGameName : objects) {
                        BoardGame b = new BoardGame();
                        b.setBoardName(boardGameName.getString("boardName"));
                        mBoardGames.add(b);
                        mSearchAdapter.addNewGame(b);
                    }
                }
            }
        });
    }

    private List<BoardGame> filter(List<BoardGame> boardGames, String query) {
        query = query.toLowerCase();

        List<BoardGame> filteredBoardGame = new ArrayList<>();
        for (BoardGame boardGame : boardGames) {
            String text = boardGame.getBoardName().toLowerCase();
            if (text.contains(query)) {
                filteredBoardGame.add(boardGame);
            }
        }

        return filteredBoardGame;
    }

    @Override
    public void fragmentBecameVisible() {
    }

    /**********************************************************************************************/
    /*                                Private Inner Classes                                       */
    /**********************************************************************************************/
    /**
     * HostSearchViewHolder is used by RecyclerView to display Views to the user.
     *
     * Created by Tony Nguyen on 2/7/2016.
     */
    private class HostSearchViewHolder extends RecyclerView.ViewHolder {

        private ImageView mBoardGameImageView;
        private TextView mBoardGameTextView;
        private ButtonRectangle mHostButton;

        private BoardGame mBoardGame;

        public HostSearchViewHolder(View itemView) {
            super(itemView);

            mBoardGameImageView =
                    (ImageView) itemView.findViewById(R.id.list_item_host_games_game_pic);

            mBoardGameTextView =
                    (TextView) itemView.findViewById(R.id.list_item_host_games_game_name);
            if(QueryPreferences.getStoredSessionId(getContext()) == null) {
                mBoardGameImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkHostAbility();
                    }
                });

                //Todo: make buttons visibility GONE, replaced by long/short holds on imageview
//                mHostButton =
//                        (ButtonRectangle) itemView.findViewById(R.id.list_item_host_games_list_button);
//                mHostButton.setVisibility(View.VISIBLE);
//                mHostButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        checkHostAbility();
//                    }
//                });
            }
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

                                        Glide.with(getContext()).load(file).into(mBoardGameImageView);
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

        public void bindGame(BoardGame boardGame) {
            mBoardGame = boardGame;
            mBoardGameTextView.setText(mBoardGame.getBoardName());
            loadBoardImage();
        }

        private void checkHostAbility() {
            ParseQuery<GameOnSession> query = GameOnSession.getQuery();
            query.whereEqualTo("host", ParseUser.getCurrentUser());
            query.whereEqualTo("open", true);
            query.findInBackground(new FindCallback<GameOnSession>() {
                @Override
                public void done(List<GameOnSession> objects, ParseException e) {
                    if(!objects.isEmpty()) {
                        Toast.makeText(getContext(), "You can only host one game", Toast.LENGTH_SHORT).show();
                    } else {
                        createSession(mBoardGame.getBoardName());
                    }
                }
            });
        }

        private void createSession(String gameTitle) {
            final GameOnSession session = new GameOnSession();
            session.setGameTitle(gameTitle);
            session.setHost(ParseUser.getCurrentUser());
            session.setPlayers(new JSONArray());
            session.setOpenStatus(true);
            session.setLocation(new ParseGeoPoint(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

            ParseACL acl  = new ParseACL();
            acl.setPublicReadAccess(true);
            acl.setPublicWriteAccess(true);
            session.setACL(acl);

            session.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        // Saved Successfully
                        QueryPreferences.setStoredSessionId(getActivity(), session.getObjectId());
                        Intent intent = SessionActivity.newIntent(getActivity(), mCurrentLocation);
                        startActivity(intent);
                    } else {
                        // The save failed
                        Log.d("ERROR", "Unable to save session in the background: " + e);
                    }
                }
            });
        }
    }

    /**
     * HostSearchAdapter works with RecyclerView to bind content to RecyclerView.ViewHolder and
     * display it to the user.
     *
     * Created by Tony Nguyen on 2/7/2016.
     */
    public class HostSearchAdapter extends RecyclerView.Adapter<HostSearchViewHolder> {

        private LayoutInflater mLayoutInflater;
        private List<BoardGame> mBoardGames;

        public HostSearchAdapter(Context context, List<BoardGame> boardGames) {
            mLayoutInflater = LayoutInflater.from(context);
            mBoardGames = new ArrayList<>(boardGames);
        }

        @Override
        public HostSearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.list_item_host_search, parent, false);
            return new HostSearchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(HostSearchViewHolder holder, int position) {
            BoardGame boardGame = mBoardGames.get(position);
            holder.bindGame(boardGame);
        }

        @Override
        public int getItemCount() {
            return mBoardGames.size();
        }

        public void addNewGame(BoardGame boardGame) {
            mBoardGames.add(boardGame);
            notifyDataSetChanged();
        }

        public BoardGame removeGame(int position) {
            BoardGame model = mBoardGames.remove(position);
            notifyItemRemoved(position);
            return model;
        }

        public void addGame(int position, BoardGame boardGame) {
            mBoardGames.add(position, boardGame);
            notifyItemInserted(position);
        }

        public void moveGame(int fromPosition, int toPosition) {
            BoardGame model = mBoardGames.remove(fromPosition);
            mBoardGames.add(toPosition, model);
            notifyItemMoved(fromPosition, toPosition);
        }

        public void animateTo(List<BoardGame> boardGames) {
            // remove all items that do not exist in filtered List anymore
            applyAndAnimateRemovals(boardGames);
            // add all items that did not exist in og list but do in filtered list
            applyAndAnimateAdditions(boardGames);
            // move all items which exist in both lists
            applyAndAnimatedMovedItems(boardGames);
        }

        // iterate backwards thru internal list of Adapter
        // check if ea item is contained in new filtered list
        // if not, call removeGame
        private void applyAndAnimateRemovals(List<BoardGame> newBoardGames) {
            for (int i = mBoardGames.size() - 1; i >= 0; i--) {
                BoardGame boardGame = mBoardGames.get(i);
                if(!newBoardGames.contains(boardGame)) {
                    removeGame(i);
                }
            }
        }

        // iterate thru filtered list
        // check if ea item exists in internal list of adapter
        // if not, call addGame
        private void applyAndAnimateAdditions(List<BoardGame> newBoardGames) {
            for (int i = 0, count = newBoardGames.size(); i < count; i++) {
                BoardGame boardGame = newBoardGames.get(i);
                if(!mBoardGames.contains(boardGame)) {
                    addGame(i, boardGame);
                }
            }
        }

        // after removal and addition, both lists contain the same elements but in different order
        // iterate backwards thru filtered list
        // look up index of ea item in internal list
        // if index different between the two, call moveItem to sort internal list
        private void applyAndAnimatedMovedItems(List<BoardGame> newBoardGames) {
            for (int toPosition = newBoardGames.size() - 1; toPosition >= 0; toPosition--) {
                BoardGame boardGame = newBoardGames.get(toPosition);
                int fromPosition = mBoardGames.indexOf(boardGame);
                if (fromPosition >= 0 && fromPosition != toPosition) {
                    moveGame(fromPosition, toPosition);
                }
            }
        }
    }
}





