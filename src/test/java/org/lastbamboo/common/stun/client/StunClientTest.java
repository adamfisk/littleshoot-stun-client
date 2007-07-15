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
        final InetSocketAddress stunServerAddress = 
            new InetSocketAddress(address, 3478);
        
        final StunClient client = new UdpStunClient(stunServerAddress);
        final InetSocketAddress publicAddress = 
            client.getServerReflexiveAddress();
        
        LOG.debug("Got address: "+publicAddress);
        assertNotNull(publicAddress);
        }
    }
