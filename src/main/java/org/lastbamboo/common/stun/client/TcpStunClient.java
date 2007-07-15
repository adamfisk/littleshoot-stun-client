package org.lastbamboo.common.stun.client;

import java.net.InetSocketAddress;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.SuccessfulBindingResponse;

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
    
    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     * @param connectTimeout The connect timeout. 
     */
    public TcpStunClient(final InetSocketAddress stunServerAddress, 
        final int connectTimeout)
        {
        super(stunServerAddress, connectTimeout);
        }
    
    public SuccessfulBindingResponse getBindingResponse()
        {
        final BindingRequest bindingRequest = new BindingRequest();
        final UUID id = bindingRequest.getTransactionId();
        
        this.m_transactionFactory.createClientTransaction(bindingRequest, this);
        synchronized (bindingRequest)
            {
            m_ioSession.write(bindingRequest);
            
            // See draft-ietf-behave-rfc3489bis-07.txt section 7.2.2.
            waitIfNoResponse(bindingRequest, 7900);
            }
        
        
        if (m_idsToResponses.containsKey(id))
            {
            // TODO: This cast is unfortunate.  Anything better?  Any 
            // generics solution?
            final SuccessfulBindingResponse response = 
                (SuccessfulBindingResponse) this.m_idsToResponses.get(id);
            return response;
            }
        return null;
        }
    

    @Override
    protected IoConnector createConnector(final int connectTimeout)
        {
        final SocketConnector connector = new SocketConnector();
        final SocketConnectorConfig config = connector.getDefaultConfig();
        config.setConnectTimeout(connectTimeout);
        return connector;
        }

    @Override
    protected InetSocketAddress getLocalAddress(final IoSession ioSession)
        {
        return (InetSocketAddress) ioSession.getLocalAddress();
        }

    public InetSocketAddress getRelayAddress()
        {
        // TODO Auto-generated method stub
        return null;
        }
    }
