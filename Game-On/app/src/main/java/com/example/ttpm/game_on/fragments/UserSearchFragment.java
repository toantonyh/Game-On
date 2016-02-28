package com.example.ttpm.game_on.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.ttpm.game_on.models.GameOnSession;
import com.example.ttpm.game_on.QueryPreferences;
import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.SessionActivity;
import com.example.ttpm.game_on.activities.UserGameActivity;
import com.example.ttpm.game_on.models.BoardGame;
import com.example.ttpm.game_on.models.BoardGameCollection;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class UserSearchFragment extends android.support.v4.app.Fragment
        implements SearchView.OnQueryTextListener {

    private RecyclerView mSearchRecyclerView;
    private UserSearchAdapter mSearchAdapter;
    private List<BoardGame> mBoardGames;
    private GameOnSession mQuickJoinSession;

    public UserSearchFragment() {
    }

    public static UserSearchFragment newInstance()
    {
        return new UserSearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_search, container, false);

        mSearchRecyclerView = (RecyclerView) view
                .findViewById(R.id.user_search_recycler_view);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        BoardGameCollection boardGameCollection = new BoardGameCollection();
        mBoardGames = boardGameCollection.getBoardGames();

        queryForAllOpenUniqueBoardGames();

        mSearchAdapter = new UserSearchAdapter(getActivity(), mBoardGames);
        mSearchRecyclerView.setAdapter(mSearchAdapter);
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

    private void queryForAllOpenUniqueBoardGames() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GameOnSession");
        query.orderByAscending("gameTitle");
        query.whereNotEqualTo("host", ParseUser.getCurrentUser());
        query.whereEqualTo("Open", true);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    for (ParseObject boardGameName : objects) {
                        boolean boardGameExist = false;
                        BoardGame b = new BoardGame();
                        b.setBoardName(boardGameName.getString("gameTitle"));
                        for (BoardGame boardGame : mBoardGames) {
                            if (boardGame.getBoardName().toLowerCase()
                                    .equals(b.getBoardName().toLowerCase())) {
                                boardGameExist = true;
                            }
                        }
                        if (!boardGameExist) {
                            mBoardGames.add(b);
                            mSearchAdapter.addNewGame(b);
                        }
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

    private class UserSearchViewHolder extends RecyclerView.ViewHolder {

        public TextView mTitleTextView;
        private TextView mSessionsTextView;
        private Button mListGamesButton;
        private Button mQuickJoinButton;

        private BoardGame mBoardGame;

        public UserSearchViewHolder(View itemView) {
            super(itemView);

            mTitleTextView =
                    (TextView) itemView.findViewById(R.id.list_item_user_games_game_pic);
            mSessionsTextView =
                    (TextView) itemView.findViewById(R.id.list_item_user_games_game_open);
            mListGamesButton =
                    (Button) itemView.findViewById(R.id.list_item_user_games_list_button);
            mListGamesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = UserGameActivity
                            .newIntent(getActivity(), mTitleTextView.getText().toString());
                    startActivity(intent);
                }
            });
            mQuickJoinButton =
                    (Button) itemView.findViewById(R.id.list_item_user_games_quick_button);
            mQuickJoinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    quickJoinSession();
                }
            });
        }

        private void quickJoinSession() {
            ParseQuery<GameOnSession> query = GameOnSession.getQuery();
            query.whereEqualTo("gameTitle", mTitleTextView.getText());
            query.whereNotEqualTo("host", ParseUser.getCurrentUser());

            query.addDescendingOrder("createdAt");
            query.setLimit(1);
            query.findInBackground(new FindCallback<GameOnSession>() {
                @Override
                public void done(List<GameOnSession> objects, ParseException e) {
                    if (e == null) {
                        for (GameOnSession session : objects) {
                            mQuickJoinSession = session;
                            mQuickJoinSession.addParticipant
                                    (ParseUser.getCurrentUser().getObjectId());
                            mQuickJoinSession.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        // Saved Successfully
                                        QueryPreferences.setStoredSessionId(getActivity(),
                                                mQuickJoinSession.getObjectId());
                                        Intent intent = SessionActivity.newIntent(getActivity());
                                        startActivity(intent);
                                    } else {
                                        // The save failed
                                        Log.d("ERROR",
                                                "Unable to add current user to session: " + e);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        }

        public void bindGame(BoardGame boardGame) {
            mBoardGame = boardGame;
            mTitleTextView.setText(mBoardGame.getBoardName());
            mSessionsTextView.setText(Integer.toString(R.id.list_item_host_games_game_open));
        }
    }

    public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchViewHolder> {

        private LayoutInflater mLayoutInflater;
        private List<BoardGame> mBoardGames;

        public UserSearchAdapter(Context context, List<BoardGame> boardGames) {
            mLayoutInflater = LayoutInflater.from(context);
            mBoardGames = new ArrayList<>(boardGames);
        }

        @Override
        public UserSearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.list_item_user_search, parent, false);
            return new UserSearchViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserSearchViewHolder holder, int position) {
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
