package org.lastbamboo.common.stun.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.apache.commons.id.uuid.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.DatagramConnector;
import org.apache.mina.transport.socket.nio.DatagramConnectorConfig;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.NullStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;

/**
 * STUN client implementation for ICE UDP. 
 */
public class UdpStunClient extends AbstractStunClient
    {
    
    private final Log LOG = LogFactory.getLog(UdpStunClient.class);
    
    
    /**
     * Creates a new STUN client for ICE processing.  This client is capable
     * of obtaining "server reflexive" and "host" candidates.  We don't use
     * relaying for UDP, so this does not currently support generating
     * "relayed" candidates.
     */
    public UdpStunClient()
        {
        super();
        }
    
    /**
     * Creates a new STUN client that connects to the specified STUN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to.
     */
    public UdpStunClient(final InetSocketAddress stunServerAddress)
        {
        super(stunServerAddress, 10*1000);
        }

    protected IoConnector createConnector(final int connectTimeout)
        {
        final DatagramConnector connector = new DatagramConnector();
        final DatagramConnectorConfig cfg = connector.getDefaultConfig();
        cfg.getSessionConfig().setReuseAddress(true);
        
        //connector.setDefaultConfig(cfg);
        return connector;
        }
    

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress)
        {
        
        // TODO: We need to somehow modify the IoSession to send the request
        // to the specified address.
        final IoSession session = connect(remoteAddress);
        
        // This method will retransmit the same request multiple times because
        // it's being sent unreliably.  All of these requests will be 
        // identical, using the same transaction ID.
        final UUID id = request.getTransactionId();
        
        this.m_transactionFactory.createClientTransaction(request, this);
        
        int requests = 0;
        
        // Use an RTO of 100ms, as discussed in 
        // draft-ietf-behave-rfc3489bis-06.txt section 7.1.  Note we just 
        // use this value and don't cache previously discovered values for
        // the RTO.
        final long rto = 100L;
        long waitTime = 0L;
        synchronized (request)
            {
            while (!m_idsToResponses.containsKey(id) && requests < 7)
                {
                waitIfNoResponse(request, waitTime);
                
                // See draft-ietf-behave-rfc3489bis-06.txt section 7.1.  We
                // continually send the same request until we receive a 
                // response, never sending more that 7 requests and using
                // an expanding interval between requests based on the 
                // estimated round-trip-time to the server.  This is because
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
        
        if (m_idsToResponses.containsKey(id))
            {
            final StunMessage response = this.m_idsToResponses.get(id);
            return response;
            }
        
        return new NullStunMessage();
        }
    
    protected InetSocketAddress getLocalAddress(final IoSession ioSession)
        {
        // This insanity is needed because IoSession.getLocalAddress does
        // not, in fact, return the local address!!
        try
            {
            final Method getChannel = 
                ioSession.getClass().getDeclaredMethod("getChannel", new Class[0]);
            getChannel.setAccessible(true);
            
            final DatagramChannel channel = 
                (DatagramChannel) getChannel.invoke(ioSession, new Object[0]);
            return (InetSocketAddress) channel.socket().getLocalSocketAddress();
            }
        catch (SecurityException e)
            {
            LOG.error("Error accessing local address", e);
            }
        catch (NoSuchMethodException e)
            {
            LOG.error("Error accessing local address", e);
            }
        catch (IllegalAccessException e)
            {
            LOG.error("Error accessing local address", e);
            }
        catch (InvocationTargetException e)
            {
            LOG.error("Error accessing local address", e);
            }

        return null;
        }

    public InetSocketAddress getRelayAddress()
        {
        // We don't support UDP relays at this time.
        LOG.warn("Attempted to get a UDP relay!!");
        return null;
        }
    }
