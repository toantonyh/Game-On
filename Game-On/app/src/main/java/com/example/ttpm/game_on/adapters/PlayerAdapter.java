package com.example.ttpm.game_on.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.example.ttpm.game_on.models.GameOnSession;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

/**
 * Created by Tony on 4/13/2016.
 */
public class PlayerAdapter extends BaseAdapter {
    private Context mContext;
    private GameOnSession mSession;

    public PlayerAdapter(Context c, GameOnSession session) {
        mContext = c;
        mSession = session;
    }

    @Override
    public int getCount() {
        return mSession.getAllPlayers().length();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView playerTv;
        if(convertView == null) {
            playerTv = new TextView(mContext);
            playerTv.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            playerTv = (TextView) convertView;
        }

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("objectId", mSession.getPlayer(position));
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {
                String username = objects.get(0).getUsername();
                playerTv.setText("Player: " + username);
            }
        });

        return playerTv;
    }
}
