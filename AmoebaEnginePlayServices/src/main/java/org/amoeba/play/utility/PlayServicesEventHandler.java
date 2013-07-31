package org.amoeba.play.utility;

/**
 * Event Handler for Play Services connection events via the PlayServiceGameHelper.
 */
public interface PlayServicesEventHandler
{
    /**
     * Callback for sign in failure.
     */
    public void onSignInFailure();

    /**
     * Callback for sign in success.
     */
    public void onSignInSuccess();

    /**
     * Callback for forceful disconnection of Play Services clients.
     */
    public void onDisconnection();
}
