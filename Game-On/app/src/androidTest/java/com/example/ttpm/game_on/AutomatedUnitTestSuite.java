package com.example.ttpm.game_on;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;

import com.example.ttpm.game_on.activities.SplashActivity;
import com.robotium.solo.Solo;
import java.util.*;

@SuppressWarnings("rawtypes")
public class AutomatedUnitTestSuite extends ActivityInstrumentationTestCase2<SplashActivity> {
    private Solo solo;


    public AutomatedUnitTestSuite() throws ClassNotFoundException {
        super(SplashActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation());
        getActivity();
    }

    @Override
    public void tearDown() throws Exception {
        solo.finishOpenedActivities();
        super.tearDown();
    }

    public void testRun() {
        solo.scrollViewToSide(solo.getView(R.id.activity_home_pager_view_pager), solo.RIGHT);
        solo.clickOnView(solo.getView(R.id.list_item_user_games_list_button));
        //      solo.clickOnView(solo.getView(R.id.list_item_join_button));
        //  solo.waitForView(solo.getView(com.example.ttpm.game_on.R.id.menu_action_log_out));
        //solo.clickOnButton("JOIN");
        //   solo.clickOnButton("Log Out");
        //    solo.clickOnView(solo.getView(com.example.ttpm.game_on.R.id.menu_action_log_out));

        /*
        try {
            solo.wait(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */
    }

    //Host Session: Host a game
    public void testHostSession() {
        solo.scrollViewToSide(solo.getView(R.id.activity_home_pager_view_pager), solo.RIGHT);
        solo.scrollViewToSide(solo.getView(R.id.activity_home_pager_view_pager), solo.RIGHT);
        assertTrue(solo.searchText("Caverna: The Cave Farmers"));
       // solo.clickOnImageButton(0);
        int count = 1;
        ArrayList<Button> buttoni = solo.getCurrentViews(Button.class);
        for(Button button: buttoni){
            if(button.getId()== (R.id.list_item_host_games_list_button)){
                if(count==1){
                    solo.clickOnView(button);
                    break;
                }
                count++;

            }
        }
       // solo.clickOnImageButton(1);
       // solo.clickInList(1);
    }
}