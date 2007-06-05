package org.lastbamboo.common.stun.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class StunClientTest extends TestCase
    {
    
    private final Logger LOG = LoggerFactory.getLogger(StunClientTest.class);

    public void testClient() throws Exception
        {
        final InetAddress address = 
            InetAddress.getByName("stun01.sipphone.com");
        final StunClient client = new UdpStunClient(address);
        final InetSocketAddress publicAddress = client.getPublicAddress(4839);
        
        LOG.debug("Got address: "+publicAddress);
        assertNotNull(publicAddress);
        }
    }
