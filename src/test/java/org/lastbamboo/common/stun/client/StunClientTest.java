package org.lastbamboo.common.stun.client;

import static org.junit.Assert.assertNotNull;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.lastbamboo.common.stun.stack.StunConstants;
import org.lastbamboo.common.util.CandidateProvider;
import org.lastbamboo.common.util.SrvCandidateProvider;
import org.lastbamboo.common.util.SrvUtil;
import org.lastbamboo.common.util.SrvUtilImpl;
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
        final SrvUtil srv = new SrvUtilImpl();
        final CandidateProvider<InetSocketAddress> stunCandidateProvider =
            new SrvCandidateProvider(srv, "_stun._udp.littleshoot.org", 
                new InetSocketAddress("stun.littleshoot.org", 
                    StunConstants.STUN_PORT));
        // We do this a bunch of times because the server selection is random.
        for (int i = 0; i < 20; i++)
            {
            final UdpStunClient client = 
                new UdpStunClient(stunCandidateProvider);
            final InetSocketAddress srflx = client.getServerReflexiveAddress();
            //System.out.println("Got address: "+srflx);
            assertNotNull("Did not get server reflexive address", srflx);
            }
        }
    }
