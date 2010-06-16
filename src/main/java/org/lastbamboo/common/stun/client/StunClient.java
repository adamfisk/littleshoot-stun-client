package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.stack.StunAddressProvider;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.littleshoot.mina.common.IoServiceListener;

/**
 * Interface for making STUN client requests.
 */
public interface StunClient extends StunAddressProvider
    {
    
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
    
    }
