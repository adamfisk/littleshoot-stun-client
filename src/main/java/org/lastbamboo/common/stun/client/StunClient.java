package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.littleshoot.mina.common.IoServiceListener;
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
     * @throws IOException If there's an IO error writing the message.
     */
    InetSocketAddress getServerReflexiveAddress() throws IOException;

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
     * Writes a STUN binding request.  This uses the default STUN RTO value
     * of 100ms.
     * 
     * @param request The STUN binding request.
     * @param remoteAddress The address to send the request to.
     * @return The response message.
     * @throws IOException If there's an IO error writing the message.
     */
    StunMessage write(BindingRequest request, InetSocketAddress remoteAddress) 
        throws IOException;

    /**
     * Writes a STUN binding request with the RTO value used for 
     * retransmissions explicitly set.
     * 
     * @param request The STUN binding request.
     * @param remoteAddress The address to send the request to.
     * @param rto The value to use for RTO when calculating retransmission 
     * times.  Note this only applies to UDP.
     * @return The response message.
     * @throws IOException If there's an IO error writing the message.
     */
    StunMessage write(BindingRequest request, InetSocketAddress remoteAddress,
        long rto) throws IOException;

    void addIoServiceListener(IoServiceListener serviceListener);

    void connect() throws IOException;
    
    void close();

    /**
     * Returns whether or not the host port was successfully mapped to a public
     * address using a port mapping protocol such as UPnP or PMP.  Note this is
     * only relevant if the client has a private address.  If this client has
     * a public address, this will return <code>false</code> to indicate no
     * mapping has taken place.
     * 
     * @return <code>true</code> if the private host address port was 
     * successfully mapped to a public address and port, otherwise 
     * <code>false</code>.
     */
    boolean hostPortMapped();

    }
