package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetAddress;

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
            final StunClient stun = new UdpStunClient(StunConstants.SERVERS);
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
