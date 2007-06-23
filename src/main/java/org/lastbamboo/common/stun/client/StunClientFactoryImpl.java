package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for constructing STUN clients.
 */
public class StunClientFactoryImpl implements StunClientFactory
    {
    
    private static final Logger LOG = 
        LoggerFactory.getLogger(StunClientFactoryImpl.class);

    private final StunTransactionFactory m_transactionFactory;
    private final StunMessageVisitorFactory m_messageVisitorFactory;
    private final InetAddress m_serverAddress;

    /**
     * Creates a new factory for creating STUN clients.
     * 
     * @param transactionFactory The factory for creating STUN transactions.
     * @param messageVisitorFactory The class for visiting STUN messages once
     * they're read.
     * @param serverAddress The address of the STUN server.  We'll use the
     * default STUN port.
     */
    public StunClientFactoryImpl(
        final StunTransactionFactory transactionFactory,
        final StunMessageVisitorFactory messageVisitorFactory,
        final String serverAddress)
        {
        this(transactionFactory, messageVisitorFactory,
            getInetAddress(serverAddress));
        }
    

    /**
     * Creates a new factory for creating STUN clients.
     * 
     * @param transactionFactory The factory for creating STUN transactions.
     * @param messageVisitorFactory The class for visiting STUN messages once
     * they're read.
     * @param serverAddress The address of the STUN server.  We'll use the
     * default STUN port.
     */
    public StunClientFactoryImpl(
        final StunTransactionFactory transactionFactory, 
        final StunMessageVisitorFactory messageVisitorFactory, 
        final InetAddress serverAddress)
        {
        m_transactionFactory = transactionFactory;
        m_messageVisitorFactory = messageVisitorFactory;
        m_serverAddress = serverAddress;
        }

    private static InetAddress getInetAddress(final String serverAddress)
        {
        try
            {
            return InetAddress.getByName(serverAddress);
            }
        catch (final UnknownHostException e)
            {
            // This probably means we're offline.
            LOG.error("Could not resolve server address", e);
            
            // We just allow the address to be null.  Otherwise, tests will
            // fail when we're offline, not to mention this is called at
            // spring bean creation time, so we want to avoid throwing
            // exceptions.
            return null;
            }
        }

    public StunClient createUdpClient()
        {
        return new UdpStunClient(this.m_transactionFactory, 
            this.m_messageVisitorFactory, this.m_serverAddress);
        }

    public StunClient createUdpClient(final InetAddress stunServerAddress)
        {
        return new UdpStunClient(this.m_transactionFactory, 
            this.m_messageVisitorFactory, stunServerAddress);
        }

    }
