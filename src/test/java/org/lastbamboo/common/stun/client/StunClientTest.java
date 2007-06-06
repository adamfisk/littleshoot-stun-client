package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionFactoryImpl;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTrackerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * Test for STUN clients.
 */
public class StunClientTest extends TestCase
    {
    
    private final Logger LOG = LoggerFactory.getLogger(StunClientTest.class);

    public void testClient() throws Exception
        {
        final InetAddress address = 
            InetAddress.getByName("stun01.sipphone.com");
        
        final StunTransactionTracker transactionTracker =
            new StunTransactionTrackerImpl();
        final StunTransactionFactory transactionFactory = 
            new StunTransactionFactoryImpl(transactionTracker);
        final StunClientMessageVisitorFactory messageVisitorFactory =
            new StunClientMessageVisitorFactory(transactionTracker);
        
        final StunClient client = new UdpStunClient(transactionFactory, 
            messageVisitorFactory, address);
        final InetSocketAddress publicAddress = client.getPublicAddress(4839);
        
        LOG.debug("Got address: "+publicAddress);
        assertNotNull(publicAddress);
        }
    }
