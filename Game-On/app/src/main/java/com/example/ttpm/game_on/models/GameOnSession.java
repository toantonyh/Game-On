package com.example.ttpm.game_on.models;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;

/**
 * Data model for a session
 */
@ParseClassName("GameOnSession")
public class GameOnSession extends ParseObject {

    public GameOnSession () {
        // default no-args constructor
    }

    public String getSessionId() {
        return getString("objectId");
    }

    public void setSessionId(String sessionId) {
        put("objectId", sessionId);
    }

    public  String getHostName(){return getString("Name");}

    public void  setHostName(String hostName){put("Name",hostName);}

    public String getHostEmail() { ParseUser currentUser = ParseUser.getCurrentUser();
        String currentuse = currentUser.getUsername();
        return currentuse;}

    public void setHostEmail(String hostEmail) {put("email", hostEmail);}

    public  void setNumberOfParticipants(String numberOfParticipants){put("participants",numberOfParticipants);}
    public  String getNumberOfParticipants(){
    int getParticipantsNumber = getParticipants().length();
    return String.valueOf(getParticipantsNumber);}



    public String getGameTitle() {
        return getString("gameTitle");
    }

    public void setGameTitle(String title) {
        put("gameTitle", title);
    }

    public ParseUser getHost() {
        return getParseUser("host");
    }

    public void setHost(ParseUser value) {
        put("host", value);
    }

    public JSONArray getParticipants() { return getJSONArray("participants"); }

    public void setParticipants(JSONArray participants) {
        put("participants", participants);
    }

    public void addParticipant(String userId) {
        JSONArray participants = this.getParticipants();
        participants.put(userId);
        this.put("partipants", participants);
    }

    public void removeParticipant(String userId) throws org.json.JSONException {
        JSONArray oldListOfParticipants = this.getParticipants();
        JSONArray newListOfParticipants = new JSONArray();

        int oldListLength = oldListOfParticipants.length();
        for (int i = 0; i < oldListLength; i++) {
            if (!oldListOfParticipants.getString(i).equals(userId)) {
                newListOfParticipants.put(oldListOfParticipants.getString(i));
            }
        }

        this.remove("participants");
        this.put("participants", newListOfParticipants);
    }

    public boolean isOpen() {
        return (this.getBoolean("Open") == true);
    }

    public void setOpenStatus(boolean status) {
        this.put("Open", status);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint("location");
    }

    public void setLocation(ParseGeoPoint value) {
        put("location", value);
    }

    public static ParseQuery<GameOnSession> getQuery() {
        return ParseQuery.getQuery(GameOnSession.class);
    }


    }