package org.lastbamboo.common.stun.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.lastbamboo.common.stun.stack.StunConstants;
import org.littleshoot.util.CandidateProvider;
import org.littleshoot.util.SrvCandidateProvider;
import org.littleshoot.util.SrvUtil;
import org.littleshoot.util.SrvUtilImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for STUN clients.
 */
public class StunClientTest {

    private final Logger LOG = LoggerFactory.getLogger(StunClientTest.class);

    @Test
    public void testClient() throws Exception {
        final SrvUtil srv = new SrvUtilImpl();
        final CandidateProvider<InetSocketAddress> stunCandidateProvider = 
            new SrvCandidateProvider(srv, "_stun._udp.littleshoot.org", 
                new InetSocketAddress("stun.littleshoot.org", StunConstants.STUN_PORT));
        // We do this a bunch of times because the server selection is random.
        for (int i = 0; i < 20; i++) {
            final UdpStunClient client = new UdpStunClient(
                    stunCandidateProvider);
            final InetSocketAddress srflx = client.getServerReflexiveAddress();
            // System.out.println("Got address: "+srflx);
            assertNotNull("Did not get server reflexive address", srflx);
        }
    }
    
    @Test
    public void testDifferentServers() throws Exception {
        final int port = StunConstants.STUN_PORT;
        
        // See http://www.voip-info.org/wiki/view/STUN
        final InetSocketAddress[] servers = {
            //new InetSocketAddress("stun.ekiga.net", port),
            //new InetSocketAddress("stun.fwdnet.net", port),
            new InetSocketAddress("stun.ideasip.com", port),
            new InetSocketAddress("stun01.sipphone.com", port),
            new InetSocketAddress("stun.softjoys.com", port),
            new InetSocketAddress("stun.voipbuster.com", port),
            new InetSocketAddress("stun.voxgratia.org", port),
            new InetSocketAddress("stun.xten.com", port),
            //new InetSocketAddress("stunserver.org", port),
            new InetSocketAddress("stun.sipgate.net", 10000),
            new InetSocketAddress("numb.viagenie.ca", port) 
        };
        
        InetAddress ia = null;
        for (final InetSocketAddress server : servers) {
            LOG.info("Hitting: "+server);
            final StunClient sc = new UdpStunClient(server);
            sc.connect();
            final InetSocketAddress ip = sc.getServerReflexiveAddress();
            if (ia != null) {
                assertEquals(ia, ip.getAddress());
            }
            else {
                ia = ip.getAddress();
            }
        }
    }
    
    @Test
    public void testRanking() throws Exception {
        final int port = StunConstants.STUN_PORT;
        
        // See http://www.voip-info.org/wiki/view/STUN
        final InetSocketAddress[] servers = {
            //new InetSocketAddress("stun.ekiga.net", port),
            //new InetSocketAddress("stunserver.org", port),
            //new InetSocketAddress("stun.fwdnet.net", port),
                
            // Re-enable these to see dynamic ranking in action..
            new InetSocketAddress("stun.ideasip.com", port),
            new InetSocketAddress("stun01.sipphone.com", port),
            new InetSocketAddress("stun.softjoys.com", port),
            new InetSocketAddress("stun.voipbuster.com", port),
            new InetSocketAddress("stun.voxgratia.org", port),
            new InetSocketAddress("stun.xten.com", port),
            new InetSocketAddress("stun.sipgate.net", 10000),
            new InetSocketAddress("numb.viagenie.ca", port) 
        };

        final StunClient sc = new UdpStunClient(servers);
        sc.connect();
        for (int i = 0; i < 20; i++) {
            sc.getServerReflexiveAddress();
        }
    }
}
