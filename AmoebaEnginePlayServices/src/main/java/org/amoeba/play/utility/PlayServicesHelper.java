package org.ameoba.play;

/**
 * Helper class for the initialization and connection of Play Services clients. Responsible for maintaining
 * and ending connections.
 */
public class PlayServicesHelper implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, OnSignOutCompleteListener
{
    private static final String TAG = "AmoebaEngine.GameHelper";

    //Request code when invoking other Activities to complete the sign in
    //process.
    private static final int RC_RESOLVE = 9001;

    //Request code when invoking Activities for which the result does not matter.
    private static final int RC_UNUSED = 9002;

    private Activity activity;
    private PlayServicesEventHandler eventHandler;

    private PlayServicesClientRequest clientRequest;
    //private List scopes;

    //private String signingInMessage;
    //private String signingOutMessage;
    //private String unknownErrorMessage;

    private GamesClient gamesClient;
    private PlusClient plusClient;
    private AppStateClient appStateClient;

    private boolean expectingConnectionResolution;
    private boolean autoSignInEnabled;

    //Bitmask representation of connected clients.
    private int connectedClients;
    private int currentlyConnectingClient;
    private String invitation;
    private boolean currentResolvingConnectionIssue;

    /**
     * Constructor.
     * @param  creatingActivity activity that is creating this helper.
     */
    public PlayServicesHelper(final Activity creatingActivity)
    {
        activity = creatingActivity;
        eventHandler = null;
        clientRequest = null;

        expectingConnectionResolution = false;
        autoSignInEnabled = false;

        connectedClients = CLIENT_NONE;
        currentlyConnectingClient = CLIENT_NONE;
        invitation = null;
        currentResolvingConnectionIssue = false;

        //scopes = new ArrayList<String>();

        //signingInMessage = "Signing in...";
        //signingOutMessage = "Signing out...";
        //unknownErrorMessage = "Unknown error occurred."
    }

    /**
     * Sets the event handler of this helper, to be called back on certain connection events.
     * @param handler handler to be called back
     */
    public void setEventHandler(final PlayServicesEventHandler handler)
    {
        eventHandler = handler;
    }

    /**
     * Initializes the clients given a request.
     * @param request request containing clients to be initialized for connection
     */
    public void initializeClients(final PlayServicesClientRequest request)
    {
        clientRequest = request;

        if (clientRequest.isGamesClientRequested())
        {
            gamesClient = new GamesClient.Builder(activity, this, this)
                    .setGravityForPopups(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setScopes(clientRequest.getScopes())
                    .create();
        }

        if (clientRequest.isPlusClientRequested())
        {
            plusClient = new PlusClient.Builder(activity, this, this)
                    .setScopes(clientRequest.getScopes())
                    .create();
        }

        if (clientRequest.isAppStateClientRequested())
        {
            appStateClient = new AppStateClient.Builder(activity, this, this)
                    .setScopes(clientRequest.getScopes())
                    .create();
        }
    }

    /**
     * Signifies the start of the owning Activity, responsible for starting connections of clients
     * if in auto-signin mode.
     */
    public void onStart()
    {
        //If we are not here because we tried to resolve a connection issue
        //and are therefore expecting some resolution, and not here because
        //the user explicitly signed out (and will therefore manually invoke
        //the sign in process), then connect. In other words, we are not
        //waiting for connection resolution, and the user is auto signing in.
        if (!expectingConnectionResolution && autoSignInEnabled)
        {
            startConnections();
        }
    }

    /**
     * Starts the connection process for all clients, sequentially.
     */
    private void startConnections()
    {
        connectedClients = CLIENT_NONE;
        //invitationID = null;
        connectNextClient();
    }

    /**
     * Starts the connection process for the next client in line, as only one client
     * should attempt to connect at a time.
     */
    private void connectNextClient()
    {
        int pendingClients = clientRequest.getClients() & ~connectedClients;
        if (pendingClients == 0)
        {
            completeSignInProcess();
            notifyEventHandlerOfSignInSuccess();
            return;
        }

        //showProgressDialog(true);

        if (gamesClient != null && (0 != (pendingClients & CLIENT_GAMES)))
        {
            currentlyConnectingClient = CLIENT_GAMES;
        }
        else if (plusClient != null && (0 != (pendingClients & CLIENT_PLUS)))
        {
            currentlyConnectingClient = CLIENT_PLUS;
        }
        else if (appStateClient != null && (0 != (pendingClients & CLIENT_APPSTATE)))
        {
            currentlyConnectingClient = CLIENT_APPSTATE;
        }

        connectCurrentClient();
    }

    /**
     * Completes the signin process.
     */
    private void completeSignInProcess()
    {
        signedIn = true;
        signInError = false;
        autoSignInEnabled = true;
        userInitiatedSignIn = false;

        //dismissDialog();
    }

    /**
     * Notifies the event handler that the sign in process has completed.
     */
    private void notifyEventHandlerOfSignInSuccess()
    {
        if (eventHandler != null)
        {
            eventHandler.onSignInSuccess();
        }
    }

    /**
     * Attempts to connect the current client ready to be connected.
     */
    private void connectCurrentClient()
    {
        switch (currentlyConnectingClient)
        {
            case CLIENT_GAMES:
                gamesClient.connect();
                break;
            case CLIENT_PLUS:
                plusClient.connect();
                break;
            case CLIENT_APPSTATE:
                appStateClient.connect();
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnected(final Bundle connectionHint)
    {
        connectedClients |= currentlyConnectingClient;

        //If this was a connection for the games client, and it came with
        //an invitation, save it!
        if (currentlyConnectingClient == CLIENT_GAMES && connectionHint != null)
        {
            Invitation incomingInvitation = connectionHint.getParcelable(GamesClient.EXTRA_INVITATION);

            if (incomingInvitation != null && incomingInvitation.getInvitationId() != null)
            {
                invitation = incomingInvitation.getInvitationId();
            }
        }

        connectNextClient();
    }

    @Override
    public void onConnectionFailed(final ConnectionResult result)
    {
        //dismissDialog();

        //If this was not a user initiated sign in, then fail and wait for the user
        //to reinitiate if desired. Only try to resolve the failure if the user
        //initiated the sign in process.
        if (!userInitiatedSignIn)
        {
            if (eventHandler != null)
            {
                eventHandler.onSignInFailure();
            }
        }
        else
        {
            currentResolvingConnectionIssue = true;
            resolveConnectionResult(result);
        }
    }

    /**
     * Attempts to resolve a connection result with result resolution or user information.
     * @param result result of the failed connection
     */
    private void resolveConnectionResult(final ConnectionResult result)
    {
        if (result.hasResolution())
        {
            try
            {
                expectingConnectionResolution = true;
                result.startResolutionForResult(activity, RC_RESOLVE);
            }
            catch (SendIntentException e)
            {
                //Try connecting again.
                connectCurrentClient();
            }
        }
        else
        {
            //This is not a problem with a resolution, so stop trying.
            cancelSignInProcess(result);
        }
    }

    /**
     * Callback for a requested connection resolution result.
     * @param requestCode  Initial code that initiated the connection resolution activity
     * @param responseCode Response code to the resolution activity
     * @param intent       intent associated with returned activity
     */
    public void onActivityResult(final int requestCode, final int responseCode, final Intent intent)
    {
        //If we've received an activity result that matches our request code,
        //we're getting a response from our connection resolution activity.
        if (requestCode == RC_RESOLVE)
        {
            expectingConnectionResolution = false;
            if (responseCode == Activity.RESULT_OK)
            {
                connectCurrentClient();
            }
            else
            {
                cancelSignInProcess();
            }
        }
    }

    /**
     * Cancels the signin process (i.e., giving up).
     * @param result result that could not be resolved
     */
    private void cancelSignInProcess(final ConnectionResult result)
    {
        signInError = true;
        autoSignInEnabled = false;
        currentResolvingConnectionIssue = false;

        //dismissDialog();

        Dialog errorDialog = null;
        if (result != null)
        {
            errorDialog = getErrorDialog(result.getErrorCode());
            errorDialog.show();
            if (eventHandler != null)
            {
                eventHandler.onSignInFailure();
            }
        }
    }

    /**
     * Determines the error message and dialog associated with a given code.
     * @param  errorCode code for which to search for a message and dialog
     * @return           error dialog
     */
    private Dialog getErrorDialog(final int errorCode)
    {
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode, mActivity,
                RC_UNUSED, null);

        if (errorDialog != null)
        {
            return errorDialog;
        }

        return (new AlertDialog.Builder(activity)).setMessage("some message")
                .setNeutralButton(some.id, null).create();
    }

    /**
     * Begins the user-initiated manual signin process.
     */
    public void beginUserInitiatedSignIn()
    {
        if (!signedIn)
        {
            autoSignIn = true;

            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
            if (result != ConnectionResult.SUCCESS)
            {
                Dialog errorDialog = getErrorDialog(result);
                errorDialog.show();
                if (eventHandler != null)
                {
                    eventHandler.onSignInFailure();
                }
            }
            else
            {
                userInitiatedSignIn = true;
                //If we are still in the process of resolving a previous sign in
                //attempt (likely via the automatic sign in), try that first.
                if (currentResolvingConnectionIssue)
                {
                    //showProgressDialog(true);
                    resolveConnectionResult();
                }
                else
                {
                    startConnections();
                }
            }
        }
    }

    /**
     * Signifies the stop of the owning Activity, responsible for shutting down connections of all
     * connected clients.
     */
    public void onStop()
    {
        killConnections(CLIENT_ALL);

        signedIn = false;
        signInError = false;

        //dismissDialog();
        progressDialog = null;
    }

    /**
     * Kills the connections of the given clients.
     * @param clientsToDisconnect clients to disconnect
     */
    public void killConnections(final int clientsToDisconnect)
    {
        if ((clientsToDisconnect & CLIENT_GAMES) != 0 && gamesClient != null &&
                gamesClient.isConnected())
        {
            connectedClients &= ~CLIENT_GAMES;
            gamesClient.disconnect();
        }

        if ((clientsToDisconnect & CLIENT_PLUS) != 0 && plusClient != null &&
                plusClient.isConnected())
        {
            connectedClients &= ~CLIENT_PLUS;
            plusClient.disconnect();
        }

        if ((clientsToDisconnect & CLIENT_APPSTATE) != 0 && appStateClient != null &&
                appStateClient.isConnected())
        {
            connectedClients &= ~CLIENT_APPSTATE;
            appStateClient.disconnect();
        }
    }

    @Override
    public void onDisconnected()
    {
        //When we are forcefully disconnected from a client.
        //Need to revisit the logic here.

        autoSignIn = false;
        signedIn = false;
        signInError = false;
        invitation = null;
        connectedClients = CLIENT_NONE;
        currentResolvingConnectionIssue = false;

        if (eventHandler != null)
        {
            eventHandler.onDisconnected();
        }
    }

    /**
     * Initiates a graceful, manual signout of all connected clients.
     */
    public void signOut()
    {
        autoSignIn = false;
        signedIn = false;
        signInError = false;

        if (plusClient != null && plusClient.isConnected())
        {
            plusClient.clearDefaultAccount();
        }

        if (gamesClient != null && gamesClient.isConnected())
        {
            //showProgressDialog(false);
            gamesClient.signOut(this);
        }

        //GamesClient needs to remain connected until we get sign out complete.
        killConnections(CLIENT_ALL & ~CLIENT_GAMES);
    }

    @Override
    public void onSignOutComplete()
    {
        //dismissDialog();

        //Why?
        if (gamesClient.isConnected())
        {
            gamesClient.disconnect();
        }
    }

	/*public void setSigningInMessage(String message)
	{
		signingInMessage = message;
	}

	public void setSigningOutMessage(String message)
	{
		signingOutMessage = message;
	}

	public void setUnknownErrorMessage(String message)
	{
		unknownErrorMessage = message;
	}*/

}
