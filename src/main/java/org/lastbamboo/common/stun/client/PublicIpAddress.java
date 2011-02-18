package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.stack.StunConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generalized class that uses various techniques to obtain a public IP address.
 */
public class PublicIpAddress {

    private static final Logger log = 
        LoggerFactory.getLogger(PublicIpAddress.class);
    private static InetAddress publicIp;
    private static long lastLookupTime;
    
    private static final InetSocketAddress[] servers = {
        //new InetSocketAddress("stun.ekiga.net", StunConstants.STUN_PORT),
        //new InetSocketAddress("stun.fwdnet.net", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.ideasip.com", StunConstants.STUN_PORT),
        new InetSocketAddress("stun01.sipphone.com", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.softjoys.com", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.voipbuster.com", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.voxgratia.org", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.xten.com", StunConstants.STUN_PORT),
        //new InetSocketAddress("stunserver.org", StunConstants.STUN_PORT),
        new InetSocketAddress("stun.sipgate.net", 10000),
        new InetSocketAddress("numb.viagenie.ca", StunConstants.STUN_PORT) 
    };
    
    /**
     * Determines the public IP address of this node.
     * 
     * @return The public IP address for this node.
     */
    public static InetAddress getPublicIpAddress() {
        final long now = System.currentTimeMillis();
        if (now - lastLookupTime < 100 * 1000) {
            return publicIp;
        }
        try {
            final StunClient stun = new UdpStunClient(servers);
            stun.connect();
            publicIp = stun.getServerReflexiveAddress().getAddress();
            lastLookupTime = System.currentTimeMillis();
            return publicIp;
        } catch (final IOException e) {
            log.warn("Could not get server reflexive address", e);
            return null;
        }
    }
}
