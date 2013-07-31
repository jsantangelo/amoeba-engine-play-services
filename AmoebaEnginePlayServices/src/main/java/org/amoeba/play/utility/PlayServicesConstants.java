package org.amoeba.play;

/**
 * Constants for the Play Services clients, as a convenience for the bit-mask operations.
 */
public final class PlayServicesConstants
{
    public static final int CLIENT_NONE = 0x00;
    public static final int CLIENT_GAMES = 0x01;
    public static final int CLIENT_PLUS = 0x02;
    public static final int CLIENT_APPSTATE = 0x04;
    public static final int CLIENT_ALL = CLIENT_GAMES | CLIENT_PLUS | CLIENT_APPSTATE;

    /**
     * Empty private constructor to prevent instantiation.
     */
    private PlayServicesConstants()
    {

    }
}
