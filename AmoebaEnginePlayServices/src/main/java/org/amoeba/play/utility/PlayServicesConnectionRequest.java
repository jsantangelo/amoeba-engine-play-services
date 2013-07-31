package org.amoeba.play;

/**
 * Responsible for encapsulating and holding the request made by the end-user application space for connecting
 * Play Services clients.
 */
public class PlayServicesClientRequest
{
    private int requestedClients;
    private List<String> scopes;

    /**
     * Default constructor.
     */
    public PlayServicesClientRequest()
    {
        requestedClients = PlayServicesConstants.CLIENT_NONE;

        initializeScopes();
    }

    /**
     * Constructor given specific clients.
     * @param  clients bit-mask containing desired clients for initialization and connection
     */
    public PlayServicesClientRequest(final int clients)
    {
        requestedClients = clients;

        initializeScopes();
    }

    /**
     * Sets the desired clients for initialization and connection.
     * @param clients bit-mask containing desired clients for initialization and connection
     */
    public void setClients(final int clients)
    {
        requestedClients = clients;

        initializeScopes();
    }

    /**
     * Initializes scopes based on requested clients.
     */
    private void initializeScopes()
    {
        scopes = new ArrayList<String>();
        if (isGamesClientRequested())
        {
            scopes.add(Scopes.GAMES);
        }

        if (isPlusClientRequested())
        {
            scopes.add(Scopes.PLUS_LOGIN);
        }

        if (isAppStateClientRequested())
        {
            scopes.add(Scopes.APP_STATE);
        }
    }

    /**
     * Returns the clients requested for initialization and connection.
     * @return bit-mask containing clients
     */
    public int getClients()
    {
        return requestedClients;
    }

    /**
     * Returns the scopes associated with the clients requested.
     * @return  scropes associated with clients
     */
    public String[] getScopes()
    {
        return scopes.toArray();
    }

    /**
     * Determines whether or not the Games Client was among the request clients.
     * @return  whether or not Games Client was requested
     */
    public boolean isGamesClientRequested()
    {
        return (0 != (requestedClients & CLIENT_GAMES));
    }

    /**
     * Determines whether or not the Plus Client was among the request clients.
     * @return  whether or not Plus Client was requested
     */
    public boolean isPlusClientRequested()
    {
        return (0 != (requestedClients & CLIENT_PLUS));
    }

    /**
     * Determines whether or not the App State Client was among the request clients.
     * @return  whether or not App State Client was requested
     */
    public boolean isAppStateClientRequested()
    {
        return (0 != (requestedClients & CLIENT_APPSTATE));
    }
}
