package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.DatagramConnectorConfig;
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
 * STUN client implementation. 
 */
public class StunClientImpl implements StunClient
    {

    private static final Log LOG = LogFactory.getLog(StunClientImpl.class);
    
    private static final int STUN_PORT = 3478;
    
    private final InetAddress m_serverAddress;

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
        final IoConnector connector = new DatagramConnector();
        final IoConnectorConfig config = new DatagramConnectorConfig();
        
        final InetSocketAddress stunServer = 
            new InetSocketAddress(this.m_serverAddress, STUN_PORT);
        
        final StunAttributesFactory attributesFactory =
            new StunAttributesFactoryImpl();
        
        final StunMessageFactory messageFactory = 
            new StunMessageFactoryImpl(attributesFactory);
        final StunMessageVisitorFactory visitorFactory = 
            new StunClientMessageVisitorFactory();
        final IoHandler handler = 
            new StunClientIoHandler(messageFactory, visitorFactory);
        
        final ProtocolEncoder encoder = new StunEncoder();
        final ProtocolDecoder decoder = new StunDecoder(messageFactory);
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(encoder, decoder);
        config.getFilterChain().addLast("to-stun", stunFilter);
        final ConnectFuture future = 
            connector.connect(stunServer, handler, config);
        
        
        //future.join();
        //final IoSession session = future.getSession();
        
        
        //final StunMessage message = messageFactory.createBindingRequest();
        
        //final ByteBuffer buffer = ByteBuffer.wrap("Test this out\r\n".getBytes());
        //session.write(buffer);
        
        //session.getCloseFuture().join();
        
        //acceptor.unbind( address );
        
        return null;
        }

    
    }
