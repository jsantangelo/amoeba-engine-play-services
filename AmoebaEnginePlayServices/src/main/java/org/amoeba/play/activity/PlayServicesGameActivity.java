package org.amoeba.play.activity;

import org.amoeba.activity.GameActivity;

import org.amoeba.play.utility.PlayServicesEventHandler;

/**
 * An extension of GameActivity, providing built-in support and handling of Google Play Services.
 */
public class PlayServicesGameActivity extends GameActivity implements PlayServicesEventHandler
{
    private static final String TAG = "AmoebaEngine.PlayServicesGameActivity";

    private PlayServicesHelper helper;
    private int requestedClients = CLIENT_GAMES;

    /**
     * Sets the clients that are requested for initialization and connection by end-user application space.
     * @param clients play services clients to be initialized for connection
     */
    protected void setRequestedClients(final int clients)
    {
        requestedClients = clients;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        helper = new PlayServicesHelper(this);
        helper.setEventHandler(this);
        helper.initializeClients(requestedClients);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        helper.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        helper.onStop();
    }

    /**
     * Begins the user-initiated sign in process for Play Services clients.
     */
    protected void initiatePlayServicesSignIn()
    {
        //This method is only to be used for the case when the sign in process
        //is initiated by the user, NOT when the user has already signed in and
        //you want to reconnect. In other words, this is for the non-auto sign in
        //process.
        helper.beginUserInitiatedSignIn();
    }

    /**
     * Gets the invitation ID associated with a sign in request.
     */
    protected void getInvitation()
    {
        //get invitation from helper
    }

    /**
     * Begins the signout process for all Play Services clients.
     */
    protected void signOut()
    {
        //sign out manually
    }

    /**
     * Forces a reconnect of all Play Services Clients.
     */
    protected void reconnect()
    {

    }
}
