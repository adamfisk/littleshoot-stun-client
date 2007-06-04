package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.DatagramConnectorConfig;
import org.lastbamboo.common.stun.stack.BindingResponseListener;
import org.lastbamboo.common.stun.stack.message.BindingResponse;
import org.lastbamboo.common.stun.stack.message.StunDecoder;
import org.lastbamboo.common.stun.stack.message.StunEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageFactoryImpl;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactory;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactoryImpl;
import org.lastbamboo.common.util.NetworkUtils;

/**
 * STUN client implementation for UDP STUN messages. 
 */
public class UdpStunClient implements StunClient, BindingResponseListener
    {

    /**
     * The default STUN port.
     */
    private static final int STUN_PORT = 3478;
    
    private final Log LOG = LogFactory.getLog(UdpStunClient.class);
    
    private final InetAddress m_serverAddress;
    
    private final Object m_mappedAddressLock = new Object();

    private BindingResponse m_bindingResponse;

    private final DatagramConnector m_connector;

    private final InetSocketAddress m_stunServer;

    private final StunClientIoHandler m_ioHandler;

    private final DatagramConnectorConfig m_connectorConfig;

    //private final StunMessageFactoryImpl m_messageFactory;

    private final StunMessage m_bindingRequest;

    /**
     * Creates a new STUN client.  This will connect on the default STUN port
     * of 3478.
     * 
     * @param serverAddress The address of the STUN server.
     */
    public UdpStunClient(final InetAddress serverAddress)
        {
        this.m_serverAddress = serverAddress;
        final StunMessageVisitorFactory visitorFactory = 
            new StunClientMessageVisitorFactory(this);
        
        m_connector = new DatagramConnector();
        m_connectorConfig = new DatagramConnectorConfig();
        
        m_stunServer = new InetSocketAddress(this.m_serverAddress, STUN_PORT);
        
        final StunAttributesFactory attributesFactory =
            new StunAttributesFactoryImpl();
        
        final StunMessageFactory messageFactory = 
            new StunMessageFactoryImpl(attributesFactory);
        
        m_ioHandler = new StunClientIoHandler(visitorFactory);
        
        final ProtocolEncoder encoder = new StunEncoder();
        final ProtocolDecoder decoder = new StunDecoder(messageFactory);
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(encoder, decoder);
        m_connectorConfig.getFilterChain().addLast("to-stun", stunFilter);
        
        // This class will retransmit the same request multiple times because
        // it's being sent unreliably.  All of these requests will be 
        // identical, using the same transaction ID.
        m_bindingRequest =  messageFactory.createBindingRequest();
        }

    public InetSocketAddress getPublicAddress(final int port)
        {
        final InetSocketAddress localAddress = getLocalAddress(port); 
        int requests = 0;
        
        // Use an RTO of 100ms, as discussed in 
        // draft-ietf-behave-rfc3489bis-06.txt section 7.1.  Note we just 
        // use this value and don't cache previously discovered values for
        // the RTO.
        final long rto = 100L;
        long waitTime = 0L;
        synchronized (this.m_mappedAddressLock)
            {
            while (m_bindingResponse == null && requests < 7)
                {
                // See draft-ietf-behave-rfc3489bis-06.txt section 7.1.  We
                // continually send the same request until we receive a 
                // response, never sending more that 7 requests and using
                // an expanding interval between requests based on the 
                // estimated round-trip-time to the server.  This is because
                // some requests can be lost with UDP.
                requestMappedAddress(localAddress);
                
                // Wait a little longer with each send.
                waitTime = (2 * waitTime) + rto;
                waitIfNoResponse(waitTime);
                requests++;
                }
            
            // Now we wait for 1.6 seconds after the last request was sent.
            // If we still don't receive a response, then the transaction 
            // has failed.  
            waitIfNoResponse(1600);
            }
        
        
        if (this.m_bindingResponse != null)
            {
            return this.m_bindingResponse.getMappedAddress();
            }
        return null;
        }

    private void waitIfNoResponse(final long waitTime)
        {
        if (m_bindingResponse == null)
            {
            try
                {
                this.m_mappedAddressLock.wait(waitTime);
                }
            catch (final InterruptedException e)
                {
                LOG.error("Unexpected interrupt", e);
                }
            }
        }

    private void requestMappedAddress(final InetSocketAddress localAddress)
        {
        LOG.debug("Requesting mapped address...");
        final ConnectFuture cf = 
            m_connector.connect(m_stunServer, localAddress, m_ioHandler, 
                m_connectorConfig);
        final IoFutureListener futureListener = new IoFutureListener()
            {
            public void operationComplete(final IoFuture future)
                {
                final IoSession session = future.getSession();
                session.write(m_bindingRequest);
                }
            };
        cf.addListener(futureListener);
        }

    private InetSocketAddress getLocalAddress(int port)
        {
        try
            {
            return new InetSocketAddress(NetworkUtils.getLocalHost(), port);
            }
        catch (final UnknownHostException e)
            {
            LOG.error("Could not find host", e);
            return null;
            }
        }

    public void onBindingResponse(final BindingResponse response)
        {
        final byte[] responseTransactionId = response.getTransactionId();
        final byte[] requestTransactionId = 
            this.m_bindingRequest.getTransactionId();
        if (!Arrays.equals(responseTransactionId, requestTransactionId))
            {
            // This should never happen because we only have a single
            // request per visitor.
            LOG.error("Unexpected transaction ID!!");
            }
        else
            {
            this.m_bindingResponse = response;
            }

        synchronized (this.m_mappedAddressLock)
            {
            LOG.debug("Got binding response, notifying...");
            this.m_mappedAddressLock.notify();
            }
        }

    
    }
