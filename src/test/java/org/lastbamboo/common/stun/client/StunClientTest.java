package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
