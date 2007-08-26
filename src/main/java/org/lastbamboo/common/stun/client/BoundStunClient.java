package org.lastbamboo.common.stun.client;

import java.net.InetSocketAddress;

/**
 * STUN client that is also bound to a listening port. 
 */
public interface BoundStunClient extends StunClient
    {

    /**
     * Gets the address the server is bound to.
     * 
     * @return The address the server is bound to.
     */
    InetSocketAddress getBoundAddress();
    }
