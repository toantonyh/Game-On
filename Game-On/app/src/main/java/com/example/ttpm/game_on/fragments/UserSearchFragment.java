package com.example.ttpm.game_on.fragments;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ttpm.game_on.R;
import com.example.ttpm.game_on.activities.SplashActivity;
import com.example.ttpm.game_on.activities.UserGameActivity;
import com.example.ttpm.game_on.models.BoardGame;
import com.example.ttpm.game_on.models.BoardGameCollection;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

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
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_log_out:
                ParseUser currentUser1 = ParseUser.getCurrentUser();
                String currentuses = currentUser1.getUsername();
                Toast.makeText(getActivity(), currentuses + " has logged out.", Toast.LENGTH_LONG).show();
                ParseUser.logOut();
                ParseUser currentUser = ParseUser.getCurrentUser();// this will now be null
                if (currentUser != null) {
                    Toast.makeText(getActivity(), "Error logging out!", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(getActivity(), SplashActivity.class);
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void queryForAllOpenUniqueBoardGames() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("GameOnSession");
        query.orderByAscending("gameTitle");
        query.whereNotEqualTo("host", ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
            if (e == null) {
                for (ParseObject boardGameName : objects) {
                    boolean boardGameExist = false;
                    BoardGame b = new BoardGame();
                    b.setBoardName(boardGameName.getString("gameTitle"));
                    for(BoardGame boardGame : mBoardGames) {
                        if(boardGame.getBoardName().toLowerCase()
                                .equals(b.getBoardName().toLowerCase())){
                            boardGameExist = true;
                        }
                    }
                    if(!boardGameExist) {
                        mBoardGames.add(b);
                        mSearchAdapter.addNewGame(b);
                    }
                }
            }
            }
        });
    }

    private class UserSearchViewHolder extends RecyclerView.ViewHolder {

        public TextView mTitleTextView;
        private TextView mSessionsTextView;
        private TextView mJoinButton;

        private BoardGame mBoardGame;

        public UserSearchViewHolder(View itemView) {
            super(itemView);

            mTitleTextView =
                    (TextView) itemView.findViewById(R.id.list_item_user_games_game_pic);
            mSessionsTextView =
                    (TextView) itemView.findViewById(R.id.list_item_user_games_game_open);
            mJoinButton =
                    (TextView) itemView.findViewById(R.id.list_item_user_games_button);
            mJoinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = UserGameActivity
                            .newIntent(getActivity(), mTitleTextView.getText().toString());
                    startActivity(intent);
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
    }
}
