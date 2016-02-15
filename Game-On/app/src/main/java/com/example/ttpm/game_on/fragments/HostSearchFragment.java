package com.example.ttpm.game_on.fragments;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttpm.game_on.GameOnSession;
import com.example.ttpm.game_on.QueryPreferences;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.HomeActivity;
import com.example.ttpm.game_on.activities.SessionActivity;
import com.example.ttpm.game_on.models.BoardGame;
import com.example.ttpm.game_on.models.BoardGameCollection;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * A simple {@link Fragment} subclass.
 */
public class HostSearchFragment extends android.support.v4.app.Fragment {

    private RecyclerView mSearchRecyclerView;
    private HostSearchAdapter mSearchAdapter;
    private List<BoardGame> mBoardGames;

    public HostSearchFragment() {
    }

    public static HostSearchFragment newInstance(String text)
    {
        HostSearchFragment f = new HostSearchFragment();
        Bundle b = new Bundle();
        b.putString("msg", text);
        f.setArguments(b);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host_search, container, false);

        mSearchRecyclerView = (RecyclerView) view
                .findViewById(R.id.host_search_recycler_view);

        SearchView mGameSearchView = (SearchView) view
                .findViewById(R.id.host_search_search_view);
        mGameSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        BoardGameCollection boardGameCollection = new BoardGameCollection();
        mBoardGames = boardGameCollection.getBoardGames();
        queryForBoardGames();

        mSearchAdapter = new HostSearchAdapter(getActivity(), mBoardGames);
        mSearchRecyclerView.setAdapter(mSearchAdapter);
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

    /**********************************************************************************************/
    /*                                Private Inner Classes                                       */
    /**********************************************************************************************/
    /**
     * HostSearchViewHolder is used by RecyclerView to display Views to the user.
     *
     * Created by Tony Nguyen on 2/7/2016.
     */
    private class HostSearchViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitleTextView;
        private TextView mSessionsTextView;
        private TextView mJoinButton;

        private BoardGame mBoardGame;

        public HostSearchViewHolder(View itemView) {
            super(itemView);

            mTitleTextView =
                    (TextView) itemView.findViewById(R.id.list_item_host_games_game_pic);
            mSessionsTextView =
                    (TextView) itemView.findViewById(R.id.list_item_host_games_game_open);
            mJoinButton =
                    (TextView) itemView.findViewById(R.id.list_item_host_games_button);
            mJoinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Toast.makeText(v.getContext(),
//                            "Wow, you QUICK JOIN this game!",
//                            Toast.LENGTH_SHORT).show();
                    createSession(mTitleTextView.getText().toString());
//                    Intent intent = SessionActivity.newIntent(getActivity());
//                    startActivity(intent);

                }
            });
        }

        public void bindGame(BoardGame boardGame) {
            mBoardGame = boardGame;
            mTitleTextView.setText(mBoardGame.getBoardName());
            mSessionsTextView.setText(Integer.toString(mBoardGame.getOpenSessions()));
        }
    }

    private void createSession(String gameTitle) {
        final GameOnSession session = new GameOnSession();
        session.setGameTitle(gameTitle);
        session.setHost(ParseUser.getCurrentUser());
        session.setParticipants(new JSONArray());

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
                    Intent intent = SessionActivity.newIntent(getActivity());
                    startActivity(intent);
                } else {
                    // The save failed
                    Log.d("ERROR", "Unable to save session in the background: " + e);
                }
            }
        });
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
            View view = mLayoutInflater.inflate(R.layout.list_item_host_games, parent, false);
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




