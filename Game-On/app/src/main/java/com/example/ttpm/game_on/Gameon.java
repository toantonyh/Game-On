package com.example.ttpm.game_on;

import com.example.ttpm.game_on.models.GameOnSession;
import com.parse.Parse;
import com.parse.ParseObject;

public class Gameon extends android.app.Application {


    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(GameOnSession.class);

        Parse.initialize(this, "duemHXnG4aocoONNNIEQLevZ7MyLAvqWSSFlBnpW", "Conlzrgvh0WbBVQgV7c0VIjqlEIcxUNSi4iwmzyW");
    }
}
