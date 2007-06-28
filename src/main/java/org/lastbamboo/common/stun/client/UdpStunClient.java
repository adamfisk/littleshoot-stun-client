package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.id.uuid.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.lastbamboo.common.stun.stack.decoder.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.message.SuccessfulBindingResponse;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionListener;
import org.lastbamboo.common.util.NetworkUtils;

/**
 * STUN client implementation for UDP STUN messages. 
 */
public class UdpStunClient implements StunClient, StunTransactionListener
    {

    /**
     * The default STUN port.
     */
    private static final int STUN_PORT = 3478;
    
    private final Log LOG = LogFactory.getLog(UdpStunClient.class);
    
    private final DatagramConnector m_connector;

    private final InetSocketAddress m_stunServer;

    private final StunClientIoHandler m_ioHandler;

    private final Map<InetSocketAddress, IoSession> m_addressMap =
        new ConcurrentHashMap<InetSocketAddress, IoSession>();

    private final StunTransactionFactory m_transactionFactory;
    
    private final Map<UUID, StunMessage> m_idsToResponses =
        new ConcurrentHashMap<UUID, StunMessage>();
    
    /**
     * Creates a new STUN client.  This will connect on the default STUN port
     * of 3478.
     * 
     * @param transactionFactory The factory for creating STUN transactions.
     * @param messageVisitorFactory Factory for creating message visitors.
     * @param serverAddress The address of the STUN server.
     */
    public UdpStunClient(final StunTransactionFactory transactionFactory,
        final StunMessageVisitorFactory messageVisitorFactory,
        final InetAddress serverAddress)
        {
        m_transactionFactory = transactionFactory;
        m_connector = new DatagramConnector();
        m_stunServer = new InetSocketAddress(serverAddress, STUN_PORT);
        m_ioHandler = new StunClientIoHandler(messageVisitorFactory);
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(codecFactory);
        
        m_connector.getFilterChain().addLast("stunFilter", stunFilter);
        }

    public InetSocketAddress getPublicAddress(final int port)
        {
        // This method will retransmit the same request multiple times because
        // it's being sent unreliably.  All of these requests will be 
        // identical, using the same transaction ID.
        final BindingRequest bindingRequest = new BindingRequest();
        
        final UUID id = bindingRequest.getTransactionId();
        
        this.m_transactionFactory.createClientTransaction(bindingRequest, this);
        
        final InetSocketAddress localAddress = getLocalAddress(port); 
        int requests = 0;
        
        // Use an RTO of 100ms, as discussed in 
        // draft-ietf-behave-rfc3489bis-06.txt section 7.1.  Note we just 
        // use this value and don't cache previously discovered values for
        // the RTO.
        final long rto = 100L;
        long waitTime = 0L;
        synchronized (bindingRequest)
            {
            while (!m_idsToResponses.containsKey(id) && requests < 7)
                {
                waitIfNoResponse(bindingRequest, waitTime);
                
                // See draft-ietf-behave-rfc3489bis-06.txt section 7.1.  We
                // continually send the same request until we receive a 
                // response, never sending more that 7 requests and using
                // an expanding interval between requests based on the 
                // estimated round-trip-time to the server.  This is because
                // some requests can be lost with UDP.
                requestMappedAddress(bindingRequest, localAddress);
                
                // Wait a little longer with each send.
                waitTime = (2 * waitTime) + rto;
                
                requests++;
                }
            
            // Now we wait for 1.6 seconds after the last request was sent.
            // If we still don't receive a response, then the transaction 
            // has failed.  
            waitIfNoResponse(bindingRequest, 1600);
            }
        
        
        if (m_idsToResponses.containsKey(id))
            {
            // TODO: This cast is unfortunate.  Anything better?  What can
            // we do here?  Any generics solution?
            final SuccessfulBindingResponse response = 
                (SuccessfulBindingResponse) this.m_idsToResponses.get(id);
            return response.getMappedAddress();
            }
        return null;
        }

    private void waitIfNoResponse(final StunMessage request, 
        final long waitTime)
        {
        LOG.debug("Waiting "+waitTime+" milliseconds...");
        if (waitTime == 0L) return;
        if (!m_idsToResponses.containsKey(request.getTransactionId()))
            {
            try
                {
                LOG.debug("Actually waiting...");
                request.wait(waitTime);
                }
            catch (final InterruptedException e)
                {
                LOG.error("Unexpected interrupt", e);
                }
            }
        }

    private void requestMappedAddress(final StunMessage bindingRequest, 
        final InetSocketAddress localAddress)
        {
        LOG.debug("Requesting mapped address...");
        final IoSession session = getIoSession(localAddress);
        session.write(bindingRequest);
        }

    private IoSession getIoSession(final InetSocketAddress localAddress)
        {
        final IoSession session = this.m_addressMap.get(localAddress);
        if (session == null)
            {
            LOG.debug("Creating new connect future for address: "+localAddress);
            final ConnectFuture connectFuture = 
                m_connector.connect(m_stunServer, localAddress, m_ioHandler);
            connectFuture.join();
            final IoSession sess = connectFuture.getSession();
            this.m_addressMap.put(localAddress, sess);
            return sess;
            }
        return session;
        }

    private InetSocketAddress getLocalAddress(final int port)
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

    public void onTransactionFailed(final StunMessage request)
        {
        synchronized (request)
            {
            request.notify();
            }
        }

    public void onTransactionSucceeded(final StunMessage request, 
        final StunMessage response)
        {
        synchronized (request)
            {
            this.m_idsToResponses.put(request.getTransactionId(), response);
            request.notify();
            }
        }

    
    }
