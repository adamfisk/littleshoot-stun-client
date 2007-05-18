package org.lastbamboo.common.stun.client;

import java.net.InetSocketAddress;

/**
 * Interface for making STUN client requests.
 */
public interface StunClient
    {

    /**
     * Gets the public address for the specified port.
     * 
     * @param port The port to check.
     * @return The public address or the port.  Note that if the machine is 
     * not behind a firewall or NAT this will be the same as the local
     * address.
     */
    InetSocketAddress getPublicAddress(int port);
    }
