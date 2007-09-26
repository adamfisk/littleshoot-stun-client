package org.lastbamboo.common.stun.client;

import java.net.InetSocketAddress;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.NullStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;

/**
 * STUN client implementation for ICE UDP. 
 */
public class TcpStunClient extends AbstractStunClient
    {
    
    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     */
    public TcpStunClient()
        {
        super();
        }

    @Override
    protected IoConnector createConnector()
        {
        final SocketConnector connector = new SocketConnector();
        final SocketConnectorConfig config = connector.getDefaultConfig();
        config.setConnectTimeout(10*1000);
        config.getSessionConfig().setReuseAddress(true);
        return connector;
        }

    public InetSocketAddress getRelayAddress()
        {
        // TODO Auto-generated method stub
        return null;
        }

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress, final long rto)
        {
        // TCP doesn't use the retransmission interval, RTO, because it's
        // reliable.
        return write(request, remoteAddress);
        }
    
    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress)
        {
        final IoSession session = connect(this.m_localAddress, remoteAddress);
        final UUID id = request.getTransactionId();
        
        this.m_transactionTracker.addTransaction(request, this,
            this.m_localAddress, remoteAddress);
        synchronized (request)
            {
            session.write(request);
            
            // See draft-ietf-behave-rfc3489bis-07.txt section 7.2.2.
            waitIfNoResponse(request, 7900);
            }
        
        if (m_idsToResponses.containsKey(id))
            {
            final StunMessage response = this.m_idsToResponses.get(id);
            return response;
            }
        return new NullStunMessage();
        }
    }
