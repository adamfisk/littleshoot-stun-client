package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.id.uuid.UUID;
import org.lastbamboo.common.stun.stack.StunIoHandler;
import org.lastbamboo.common.stun.stack.StunProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.BindingErrorResponse;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.BindingSuccessResponse;
import org.lastbamboo.common.stun.stack.message.ConnectErrorStunMessage;
import org.lastbamboo.common.stun.stack.message.NullStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionListener;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTrackerImpl;
import org.littleshoot.util.CandidateProvider;
import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.ConnectFuture;
import org.littleshoot.mina.common.ExecutorThreadModel;
import org.littleshoot.mina.common.IoConnector;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoServiceListener;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.common.SimpleByteBufferAllocator;
import org.littleshoot.mina.filter.codec.ProtocolCodecFactory;
import org.littleshoot.mina.filter.codec.ProtocolCodecFilter;
import org.littleshoot.mina.transport.socket.nio.DatagramConnector;
import org.littleshoot.mina.transport.socket.nio.DatagramConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract STUN client.  Subclasses typically define transports.
 */
public class UdpStunClient implements StunClient, StunTransactionListener {

    private static final Logger LOG = 
        LoggerFactory.getLogger(UdpStunClient.class);
    
    private final Collection<IoServiceListener> m_ioServiceListeners =
        new ArrayList<IoServiceListener>();

    private RankedStunServer m_stunServer;
    
    private final IoHandler m_ioHandler;

    private final Map<UUID, StunMessage> m_idsToResponses =
        new ConcurrentHashMap<UUID, StunMessage>();

    private InetSocketAddress m_localAddress;

    /**
     * Just keeps track of the current connection 5-tuple so we don't try
     * to connect to the host we're already connected to.
     */
    private IoSession m_currentIoSession;

    private final StunTransactionTracker<StunMessage> m_transactionTracker;

    private final InetSocketAddress m_originalLocalAddress;

    private final Collection<IoSession> m_sessions = 
        new LinkedList<IoSession>();

    private final Queue<RankedStunServer> m_stunServers = 
        new PriorityQueue<UdpStunClient.RankedStunServer>();

    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     * 
     * @param transactionTracker The transaction tracker to use.
     * @param ioHandler The {@link IoHandler} to use.
     * @param stunServerCandidateProvider Provider for STUN server addresses.
     * @throws IOException If we can't get a STUN server address.
     */
    public UdpStunClient(
        final StunTransactionTracker<StunMessage> transactionTracker,
        final IoHandler ioHandler, 
        final CandidateProvider<InetSocketAddress> stunServerCandidateProvider) 
            throws IOException {
        this(null, stunServerCandidateProvider.getCandidates(),
                transactionTracker, ioHandler);
    }
    
    /**
     * Creates a new STUN client that connects to the specified STUN servers.
     * @param stunServerCandidateProvider Class that provides STUN servers to
     * use.
     * @throws IOException If we can't get a STUN server address. 
     */
    public UdpStunClient(
        final CandidateProvider<InetSocketAddress> stunServerCandidateProvider) 
            throws IOException {
        this(null, stunServerCandidateProvider.getCandidates(), null, null);
    }
    
    /**
     * Creates a new STUN client that connects to the specified STUN servers.
     * @param stunServerCandidateProvider Class that provides STUN servers to
     * use.
     * @throws IOException If we can't get a STUN server address. 
     */
    public UdpStunClient(final InetSocketAddress... stunServers) 
        throws IOException {
        this(null, Arrays.asList(stunServers), null, null);
    }

    
    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     * @param connectTimeout The timeout to wait for connections.
     * @throws IOException 
     */
    private UdpStunClient(final InetSocketAddress localAddress,
            final Collection<InetSocketAddress> stunServers,
            final StunTransactionTracker<StunMessage> transactionTracker,
            final IoHandler ioHandler) throws IOException {
        if (stunServers == null) {
            LOG.error("Null STUN server provider");
            throw new NullPointerException("Null STUN server provider");
        }
        LOG.info("Creating UDP STUN CLIENT");
        for (final InetSocketAddress isa : stunServers) {
            final RankedStunServer rss = new RankedStunServer(isa);
            this.m_stunServers.add(rss);
        }
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        m_originalLocalAddress = localAddress;
        if (transactionTracker == null) {
            this.m_transactionTracker = new StunTransactionTrackerImpl();
        } else {
            this.m_transactionTracker = transactionTracker;
        }

        m_stunServer = pickStunServerInetAddress();

        if (ioHandler == null) {
            final StunMessageVisitorFactory messageVisitorFactoryToUse = 
                new StunClientMessageVisitorFactory(
                    this.m_transactionTracker);
            m_ioHandler = new StunIoHandler(messageVisitorFactoryToUse);
        } else {
            m_ioHandler = ioHandler;
        }
    }
   
    public void connect() throws IOException {
        IoSession session;
        try {
            session = connect(m_originalLocalAddress, m_stunServer.isa);
        } catch (final IOException e) {
            onFailure(m_stunServer);
            throw e;
        }

        // We set the local address here because the original could be null
        // to bind to an ephemeral port.
        this.m_localAddress = (InetSocketAddress) session.getLocalAddress();
    }

    private void onFailure(final RankedStunServer rss) throws IOException {
        // It needs to get placed again in the ranking.
        if (m_stunServer.failures < 5) {
            m_stunServer.failures++;
            m_stunServers.remove(rss);
            m_stunServers.add(rss);
        }
        this.m_stunServer = pickStunServerInetAddress();
    }
    
    private void onSuccess(final RankedStunServer m_stunServer2) {
        if (m_stunServer.successes < 5) {
            this.m_stunServer.successes++;
        }
    }

    private final IoSession connect(final InetSocketAddress localAddress,
            final InetSocketAddress stunServer) throws IOException {
        // We can't connect twice to the same 5-tuple, so check to verify we're
        // not reconnecting to the remote host we're already connected to.
        if (this.m_currentIoSession != null
                && this.m_currentIoSession.getRemoteAddress().equals(
                        stunServer)) {
            return this.m_currentIoSession;
        }

        final ProtocolCodecFactory codecFactory = new StunProtocolCodecFactory();
        final ProtocolCodecFilter stunFilter = new ProtocolCodecFilter(
                codecFactory);

        final IoConnector connector = createConnector();
        connector.getFilterChain().addLast("stunFilter", stunFilter);

        if (this.m_ioServiceListeners.isEmpty()) {
            LOG.debug("No service listeners for: {}", getClass()
                    .getSimpleName());
        }
        synchronized (this.m_ioServiceListeners) {
            for (final IoServiceListener sl : this.m_ioServiceListeners) {
                connector.addListener(sl);
            }
        }
        LOG.debug("Connecting to: {}", stunServer);
        final ConnectFuture cf = connector.connect(stunServer,
                localAddress, m_ioHandler);
        LOG.debug("About to join");
        cf.join();
        LOG.debug("Connected to: {}", stunServer);
        final IoSession session = cf.getSession();
        if (session == null) {
            throw new IOException("Could not get session with: "
                    + stunServer);
        }
        this.m_sessions.add(session);
        this.m_currentIoSession = session;
        return session;
    }

    public InetSocketAddress getHostAddress() {
        return m_localAddress;
    }

    public InetAddress getStunServerAddress() {
        return this.m_stunServer.isa.getAddress();
    }

    protected void waitIfNoResponse(final StunMessage request,
            final long waitTime) {
        LOG.debug("Waiting " + waitTime + " milliseconds...");
        if (waitTime == 0L)
            return;
        if (!m_idsToResponses.containsKey(request.getTransactionId())) {
            try {
                LOG.debug("Actually waiting...");
                request.wait(waitTime);
            } catch (final InterruptedException e) {
                LOG.error("Unexpected interrupt", e);
            }
        }
    }

    public Object onTransactionFailed(final StunMessage request,
            final StunMessage response) {
        return notifyWaiters(request, response);
    }

    public Object onTransactionSucceeded(final StunMessage request,
            final StunMessage response) {
        return notifyWaiters(request, response);
    }

    private Object notifyWaiters(StunMessage request, StunMessage response) {
        synchronized (request) {
            this.m_idsToResponses.put(request.getTransactionId(), response);
            request.notify();
        }
        return null;
    }

    public final void addIoServiceListener(
            final IoServiceListener serviceListener) {
        LOG.debug("Adding service listener for: {}", this);
        this.m_ioServiceListeners.add(serviceListener);
    }

    public void close() {
        LOG.debug("Closing sessions...");
        synchronized (m_sessions) {
            for (final IoSession session : m_sessions) {
                LOG.debug("Closing: {}", session);
                session.close();
            }
        }
    }

    private IoConnector createConnector() {
        final DatagramConnector connector = new DatagramConnector();
        final DatagramConnectorConfig cfg = connector.getDefaultConfig();
        cfg.getSessionConfig().setReuseAddress(true);
        cfg.setThreadModel(ExecutorThreadModel.getInstance(getClass()
                .getSimpleName()));
        return connector;
    }

    public InetSocketAddress getServerReflexiveAddress() throws IOException {
        for (int i = 0; i < this.m_stunServers.size(); i++) {
            LOG.info("Getting server reflexive address from: {}",
                    this.m_stunServer);
            final BindingRequest br = new BindingRequest();
            final StunMessage message = write(br, this.m_stunServer.isa);
            final StunMessageVisitor<InetSocketAddress> visitor = 
                new StunMessageVisitorAdapter<InetSocketAddress>() {
                @Override
                public InetSocketAddress visitBindingSuccessResponse(
                        final BindingSuccessResponse response) {
                    return response.getMappedAddress();
                }

                @Override
                public InetSocketAddress visitBindingErrorResponse(
                        final BindingErrorResponse response) {
                    LOG.warn("Received Binding Error Response: " + response);
                    return null;
                }

                @Override
                public InetSocketAddress visitConnectErrorMesssage(
                        final ConnectErrorStunMessage error) {
                    LOG.warn("Received ICMP error: {}", error);
                    return null;
                }
            };

            final InetSocketAddress isa = message.accept(visitor);
            if (isa == null) {
                onFailure(m_stunServer);
                continue;
            }
            onSuccess(this.m_stunServer);
            
            // Always keep rotating.
            this.m_stunServer = pickStunServerInetAddress();
            return isa;
        }

        // If we get here, all our attempts failed. Maybe the client's offline?
        throw new IOException("Could not get server reflexive address!");
    }

    public StunMessage write(final BindingRequest request,
        final InetSocketAddress remoteAddress) throws IOException {
        // Use an RTO of 100ms, as discussed in
        // draft-ietf-behave-rfc3489bis-06.txt section 7.1. Note we just
        // use this value and don't cache previously discovered values for
        // the RTO.
        final long rto = 100L;
        return write(request, remoteAddress, rto);
    }

    public StunMessage write(final BindingRequest request,
            final InetSocketAddress remoteAddress, final long rto)
            throws IOException {
        // Note we've typically already "connected" around creation time with
        // the connect method, but it's cheap with UDP.
        final IoSession session = connect(this.m_localAddress, remoteAddress);

        // This method will retransmit the same request multiple times because
        // it's being sent unreliably. All of these requests will be
        // identical, using the same transaction ID.
        final UUID id = request.getTransactionId();

        this.m_transactionTracker.addTransaction(request, this,
                this.m_localAddress, remoteAddress);

        int requests = 0;

        long waitTime = 0L;
        synchronized (request) {
            while (!m_idsToResponses.containsKey(id) && requests < 7) {
                waitIfNoResponse(request, waitTime);

                // See draft-ietf-behave-rfc3489bis-06.txt section 7.1. We
                // continually send the same request until we receive a
                // response, never sending more that 7 requests and using
                // an expanding interval between requests based on the
                // estimated round-trip-time to the server. This is because
                // some requests can be lost with UDP.
                session.write(request);

                // Wait a little longer with each send.
                waitTime = (2 * waitTime) + rto;

                requests++;
            }

            // Now we wait for 1.6 seconds after the last request was sent.
            // If we still don't receive a response, then the transaction
            // has failed.
            waitIfNoResponse(request, 1600);
        }

        if (m_idsToResponses.containsKey(id)) {
            final StunMessage response = this.m_idsToResponses.get(id);
            return response;
        }

        LOG.warn("Did not get response from: " + remoteAddress);
        return new NullStunMessage();
    }

    public InetSocketAddress getRelayAddress() {
        // We don't support UDP relays at this time.
        LOG.warn("Attempted to get a UDP relay!!");
        return null;
    }

    public boolean hostPortMapped() {
        // We don't map ports for clients (only for classes that also accept
        // incoming connections).
        return false;
    }

    private RankedStunServer pickStunServerInetAddress() throws IOException {
        return pickStunServerInetAddress(null);
    }

    private RankedStunServer pickStunServerInetAddress(
            final InetSocketAddress skipAddress) throws IOException {
        if (m_stunServers.isEmpty()) {
            LOG.warn("Could not get STuN addresses!!");
            throw new IOException("No STUN addresses returned!");
        }
        if (skipAddress != null) {
            m_stunServers.remove(skipAddress);
        }
        final RankedStunServer rss = m_stunServers.peek();
        return rss;
    }

    private class RankedStunServer implements Comparable<RankedStunServer>{

        private final InetSocketAddress isa;
        private int successes;
        private int failures;
        private RankedStunServer(final InetSocketAddress isa) {
            this.isa = isa;
        }
        private int getScore() {
            return successes - failures;
        }
        

        @Override
        public String toString() {
            return "RankedStunServer [isa=" + isa + " score="+getScore()+"]";
        }
        
        public int compareTo(final RankedStunServer rss) {
            final Integer score1 = rss.getScore();
            return score1.compareTo(getScore());
        }
    }
}
