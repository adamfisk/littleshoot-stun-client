package org.lastbamboo.common.stun.client;

import static org.junit.Assert.assertNotNull;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for STUN clients.
 */
public class StunClientTest 
    {
    
    private final Logger LOG = LoggerFactory.getLogger(StunClientTest.class);

    @Test public void testClient() throws Exception
        {
        // We do this a bunch of times because the server selection is random.
        for (int i = 0; i < 20; i++)
            {
            final UdpStunClient client = new UdpStunClient("_stun._udp.littleshoot.org");
            final InetSocketAddress srflx = client.getServerReflexiveAddress();
            //System.out.println("Got address: "+srflx);
            assertNotNull("Did not get server reflexive address", srflx);
            }
        }
    }
