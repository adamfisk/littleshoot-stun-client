package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.lastbamboo.common.stun.stack.StunIoHandler;
import org.lastbamboo.common.stun.stack.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.BindingErrorResponse;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.BindingSuccessResponse;
import org.lastbamboo.common.stun.stack.message.ConnectErrorStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionListener;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTrackerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract STUN client.  Subclasses typically define transports.
 */
public abstract class AbstractStunClient implements StunClient, 
    StunTransactionListener
    {

    /**
     * The default STUN port.
     */
    private static final int STUN_PORT = 3478;
    
    private static final Logger LOG = 
        LoggerFactory.getLogger(AbstractStunClient.class);
    
    private final Collection<IoServiceListener> m_ioServiceListeners =
        new LinkedList<IoServiceListener>();

    /**
     * This is the address of the STUN server to connect to.
     */
    private final InetSocketAddress m_stunServerAddress;

    private final IoHandler m_ioHandler;

    protected final Map<UUID, StunMessage> m_idsToResponses =
        new ConcurrentHashMap<UUID, StunMessage>();

    protected InetSocketAddress m_localAddress;

    /**
     * Just keeps track of the current connection 5-tuple so we don't try
     * to connect to the host we're already connected to.
     */
    private IoSession m_currentIoSession;

    protected final StunTransactionTracker m_transactionTracker;

    private final InetSocketAddress m_originalLocalAddress;

    private final Collection<IoSession> m_sessions = 
        new LinkedList<IoSession>();
    
    /**
     * Free World Dialup.
     */
    private static final String DEFAULT_STUN_SERVER = "stun.fwdnet.net";

    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     */
    protected AbstractStunClient()
        {
        this(createInetAddress(DEFAULT_STUN_SERVER));
        }
    
    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     * 
     * @param transactionTracker The transaction tracker to use.
     */
    protected AbstractStunClient(
        final StunTransactionTracker<StunMessage> transactionTracker)
        {
        this (null, createInetAddress(DEFAULT_STUN_SERVER), 
            transactionTracker, null);
        }
    
    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     * @param connectTimeout The timeout to wait for connections.
     */
    protected AbstractStunClient(final InetAddress stunServerAddress)
        {
        this (null, stunServerAddress, null, null);
        }
    
    /**
     * Creates a new STUN client that binds to the specified local address
     * but otherwise uses default settings.
     * 
     * @param localAddress The local address to bind to.
     */
    public AbstractStunClient(final InetSocketAddress localAddress)
        {
        this (localAddress, createInetAddress(DEFAULT_STUN_SERVER), 
            null, null);
        }
    
    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     * @param connectTimeout The timeout to wait for connections.
     */
    private AbstractStunClient(final InetSocketAddress localAddress,
        final InetAddress stunServerAddress,
        final StunTransactionTracker transactionTracker, 
        final StunMessageVisitorFactory messageVisitorFactory)
        {
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        m_originalLocalAddress = localAddress;
        if (transactionTracker == null)
            {
            this.m_transactionTracker = new StunTransactionTrackerImpl();
            }
        else
            {
            this.m_transactionTracker = transactionTracker;
            }
        
        final StunMessageVisitorFactory messageVisitorFactoryToUse;
        if (messageVisitorFactory == null)
            {
            messageVisitorFactoryToUse =
                new StunClientMessageVisitorFactory(this.m_transactionTracker);
            }
        else
            {
            messageVisitorFactoryToUse = messageVisitorFactory;
            }
        m_stunServerAddress = 
            new InetSocketAddress(stunServerAddress, STUN_PORT);
        m_ioHandler = new StunIoHandler(messageVisitorFactoryToUse);
        }
    
    public void connect()
        {
        final IoSession session = 
            connect(m_originalLocalAddress, m_stunServerAddress); 
        
        // We set the local address here because the original could be null
        // to bind to an ephemeral port.
        this.m_localAddress = (InetSocketAddress) session.getLocalAddress();
        }
    
    protected final IoSession connect(final InetSocketAddress localAddress, 
        final InetSocketAddress stunServerAddress)
        {
        // We can't connect twice to the same 5-tuple, so check to verify we're
        // not reconnecting to the remote host we're already connected to.
        if (this.m_currentIoSession != null && 
            this.m_currentIoSession.getRemoteAddress().equals(stunServerAddress))
            {
            return this.m_currentIoSession;
            }
        
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(codecFactory);
        
        final IoConnector connector = createConnector();
        connector.getFilterChain().addLast("stunFilter", stunFilter);
        
        if (this.m_ioServiceListeners.isEmpty())
            {
            LOG.debug("No service listeners for: {}",
                getClass().getSimpleName());
            }
        synchronized (this.m_ioServiceListeners)
            {
            for (final IoServiceListener sl : this.m_ioServiceListeners)
                {
                connector.addListener(sl);
                }
            }
        LOG.debug("Connecting to: {}", stunServerAddress);
        final ConnectFuture cf = 
            connector.connect(stunServerAddress, localAddress, m_ioHandler);
        LOG.debug("About to join");
        cf.join();
        LOG.debug("Connected to: {}", stunServerAddress);
        final IoSession session = cf.getSession();
        this.m_sessions.add(session);
        this.m_currentIoSession = session;
        return session;
        }

    protected abstract IoConnector createConnector();
    
    public InetSocketAddress getServerReflexiveAddress()
        {
        final BindingRequest br = new BindingRequest();
        
        final StunMessage message = write(br, this.m_stunServerAddress);
        final StunMessageVisitor<InetSocketAddress> visitor = 
            new StunMessageVisitorAdapter<InetSocketAddress>()
            {
            @Override
            public InetSocketAddress visitBindingSuccessResponse(
                final BindingSuccessResponse response)
                {
                return response.getMappedAddress();
                }

            @Override
            public InetSocketAddress visitBindingErrorResponse(
                final BindingErrorResponse response)
                {
                LOG.warn("Received Binding Error Response: "+response);
                return null;
                }

            @Override
            public InetSocketAddress visitConnectErrorMesssage(
                final ConnectErrorStunMessage error)
                {
                LOG.warn("Received ICMP error: {}", error);
                return null;
                }
            };
          
        return message.accept(visitor);
        }
    
    public InetSocketAddress getHostAddress()
        {
        return m_localAddress;
        }
    
    public InetAddress getStunServerAddress()
        {
        return this.m_stunServerAddress.getAddress();
        }

    protected void waitIfNoResponse(final StunMessage request, 
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

    public Object onTransactionFailed(final StunMessage request,
        final StunMessage response)
        {
        return notifyWaiters(request, response);
        }
    
    public Object onTransactionSucceeded(final StunMessage request, 
        final StunMessage response)
        {
        return notifyWaiters(request, response);
        }

    private Object notifyWaiters(StunMessage request, StunMessage response)
        {
        synchronized (request)
            {
            this.m_idsToResponses.put(request.getTransactionId(), response);
            request.notify();
            }
        return null;
        }
    

    public final void addIoServiceListener(
        final IoServiceListener serviceListener)
        {
        LOG.debug("Adding service listener for: {}", this);
        this.m_ioServiceListeners.add(serviceListener);
        }
    

    public void close()
        {
        synchronized (m_sessions)
            {
            for (final IoSession session : m_sessions)
                {
                session.close();
                }
            }
        }
    
    private static InetAddress createInetAddress(final String host)
        {
        try
            {
            return InetAddress.getByName(host);
            }
        catch (final UnknownHostException e)
            {
            LOG.error("Could not lookup host!!", e);
            throw new IllegalArgumentException("Could not lookup host: " +
                host + "...  No network?");
            }
        }
    }
