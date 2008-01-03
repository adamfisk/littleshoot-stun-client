package org.lastbamboo.common.stun.client;

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
        final StunClient client = new UdpStunClient();
        final InetSocketAddress publicAddress = 
            client.getServerReflexiveAddress();
        
        LOG.debug("Got address: "+publicAddress);
        assertNotNull("Null public address -- STUN client could not access public address", 
                publicAddress);
        }
    }
