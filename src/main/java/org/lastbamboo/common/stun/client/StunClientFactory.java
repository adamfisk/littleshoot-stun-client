package org.lastbamboo.common.stun.client;

import java.net.InetAddress;

/**
 * Interface for factories for creating STUN clients.
 */
public interface StunClientFactory
    {

    /**
     * Creates a UDP STUN client using the default server address.
     * 
     * @return a UDP STUN client.
     */
    StunClient createUdpClient();

    /**
     * Creates a new UDP STUN client using the specified STUN server address.
     * 
     * @param stunServerAddress The address of the STUN server.
     * @return The new STUN client.
     */
    StunClient createUdpClient(InetAddress stunServerAddress);
    }
