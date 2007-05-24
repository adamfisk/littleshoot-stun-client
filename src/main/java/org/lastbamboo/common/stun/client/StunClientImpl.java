package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.DatagramConnectorConfig;
import org.lastbamboo.common.stun.stack.BindingResponseListener;
import org.lastbamboo.common.stun.stack.message.BindingResponse;
import org.lastbamboo.common.stun.stack.message.StunDecoder;
import org.lastbamboo.common.stun.stack.message.StunEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessageFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageFactoryImpl;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactory;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributesFactoryImpl;
import org.lastbamboo.common.util.NetworkUtils;

/**
 * STUN client implementation. 
 */
public class StunClientImpl implements StunClient, BindingResponseListener
    {

    private static final Log LOG = LogFactory.getLog(StunClientImpl.class);
    
    private static final int STUN_PORT = 3478;
    
    private final InetAddress m_serverAddress;
    
    private final Object m_mappedAddressLock = new Object();

    private BindingResponse m_bindingResponse;

    /**
     * Creates a new STUN client.  This will connect on the default STUN port
     * of 3478.
     * 
     * @param serverAddress The address of the STUN server.
     */
    public StunClientImpl(final InetAddress serverAddress)
        {
        this.m_serverAddress = serverAddress;
        }

    public InetSocketAddress getPublicAddress(final int port)
        {
        
        final StunMessageVisitorFactory visitorFactory = 
            new StunClientMessageVisitorFactory(this);

        int requests = 0;
        
        synchronized (this.m_mappedAddressLock)
            {
            while (m_bindingResponse == null && requests < 10)
                {
                // Issue a new request every few seconds.  We're using UDP,
                // so some requests will be lost.  This just keeps sending
                // them until we get a response.
                requestMappedAddress(visitorFactory, port);
                try
                    {
                    this.m_mappedAddressLock.wait(2000);
                    }
                catch (final InterruptedException e)
                    {
                    LOG.error("Unexpected interrupt", e);
                    }
                requests++;
                }
            }
        
        if (this.m_bindingResponse != null)
            {
            return this.m_bindingResponse.getMappedAddress();
            }
        return null;
        }

    private void requestMappedAddress(
        final StunMessageVisitorFactory visitorFactory, final int port)
        {
        final IoConnector connector = new DatagramConnector();
        final IoConnectorConfig config = new DatagramConnectorConfig();
        
        final InetSocketAddress stunServer = 
            new InetSocketAddress(this.m_serverAddress, STUN_PORT);
        
        final InetSocketAddress localAddress = getLocalAddress(port); 
        
        final StunAttributesFactory attributesFactory =
            new StunAttributesFactoryImpl();
        
        final StunMessageFactory messageFactory = 
            new StunMessageFactoryImpl(attributesFactory);
        
        final IoHandler handler = 
            new StunClientIoHandler(messageFactory, visitorFactory);
        
        final ProtocolEncoder encoder = new StunEncoder();
        final ProtocolDecoder decoder = new StunDecoder(messageFactory);
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(encoder, decoder);
        config.getFilterChain().addLast("to-stun", stunFilter);
        
        
        final ConnectFuture future = 
            connector.connect(stunServer, localAddress, handler, config);
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
        this.m_bindingResponse = response;
        synchronized (this.m_mappedAddressLock)
            {
            this.m_mappedAddressLock.notify();
            }
        }

    
    }
