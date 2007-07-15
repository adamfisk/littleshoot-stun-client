package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.id.uuid.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.lastbamboo.common.stun.stack.decoder.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.message.SuccessfulBindingResponse;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactoryImpl;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionListener;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTrackerImpl;

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
    
    private final Log LOG = LogFactory.getLog(AbstractStunClient.class);
    
    private final IoConnector m_connector;

    /**
     * This is the address of the STUN server to connect to.
     */
    private final InetSocketAddress m_stunServerAddress;

    private final StunClientIoHandler m_ioHandler;

    protected final StunTransactionFactory m_transactionFactory;
    
    protected final Map<UUID, StunMessage> m_idsToResponses =
        new ConcurrentHashMap<UUID, StunMessage>();

    protected final IoSession m_ioSession;

    private final InetSocketAddress m_localAddress;

    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     * 
     * @param connector The class for creating the connection.
     */
    protected AbstractStunClient()
        {
        this(new InetSocketAddress("stun01.sipphone.com", STUN_PORT), 10*1000);
        }


    public AbstractStunClient(final InetSocketAddress localAddress, 
        final InetSocketAddress stunServerAddress, final int connectTimeout)
        {
        final StunTransactionTracker tracker = new StunTransactionTrackerImpl();
        m_transactionFactory = new StunTransactionFactoryImpl(tracker);
        
        final StunMessageVisitorFactory messageVisitorFactory =
            new StunClientMessageVisitorFactory(tracker);
        
        m_connector = createConnector(connectTimeout);
        m_stunServerAddress = stunServerAddress;
        m_ioHandler = new StunClientIoHandler(messageVisitorFactory);
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(codecFactory);
        
        m_connector.getFilterChain().addLast("stunFilter", stunFilter);
        final ConnectFuture connectFuture = 
            m_connector.connect(m_stunServerAddress, localAddress, m_ioHandler);
        
        connectFuture.join();
        this.m_ioSession = connectFuture.getSession();
        if (localAddress == null)
            {
            this.m_localAddress = getLocalAddress(m_ioSession);
            }
        else
            {
            this.m_localAddress = localAddress;
            }
        }

    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     * @param connectTimeout The timeout to wait for connections.
     */
    protected AbstractStunClient(final InetSocketAddress stunServerAddress, 
        final int connectTimeout)
        {
        this(null, stunServerAddress, connectTimeout);
        }


    protected abstract IoConnector createConnector(int connectTimeout);
    protected abstract InetSocketAddress getLocalAddress(IoSession ioSession);
    
    public InetSocketAddress getServerReflexiveAddress()
        {
        final SuccessfulBindingResponse response = getBindingResponse();
        if (response == null)
            {
            return null;
            }
        
        return response.getMappedAddress();
        }
    
    public InetSocketAddress getHostAddress()
        {
        return m_localAddress;
        }
    
    public InetAddress getStunServerAddress()
        {
        return this.m_stunServerAddress.getAddress();
        }
    
    public IoSession getIoSession() 
        {
        return this.m_ioSession;
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
