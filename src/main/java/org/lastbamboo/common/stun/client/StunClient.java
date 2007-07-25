package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.StunMessage;

/**
 * Interface for making STUN client requests.
 */
public interface StunClient
    {

    /**
     * Gets the host address for this client, or the local address on a local
     * network interface.
     * 
     * @return The host address and port.
     */
    InetSocketAddress getHostAddress();

    /**
     * Accessor for the "server reflexive address" for this ICE candidate, or
     * the address from the perspective of a public STUN server.  This can
     * block for a little while as the client continues sending packets if 
     * there's packet loss.
     * 
     * @return The server reflexive address for this ICE candidate.
     */
    InetSocketAddress getServerReflexiveAddress();

    /**
     * Accessor for the address of the STUN server.
     * 
     * @return The address of the STUN server.
     */
    InetAddress getStunServerAddress();

    /**
     * Accessor the relay address using the STUN relay usage.  If the client
     * doesn't have an allocated address, this attempts to allocate one before
     * returning. 
     * 
     * @return The allocated relay address, or <code>null</code> if no relay
     * address could be obtained.
     */
    InetSocketAddress getRelayAddress();
    
    /**
     * Writes a STUN binding request.
     * 
     * @param request The STUN binding request.
     * @param remoteAddress The address to send the request to.
     * @return The response message.
     */
    StunMessage write(BindingRequest request, InetSocketAddress remoteAddress);

    }
