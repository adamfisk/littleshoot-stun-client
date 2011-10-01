package org.lastbamboo.common.stun.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;

import org.littleshoot.stun.stack.StunConstants;

/**
 * Repository controlling which STUN servers to use.
 */
public class StunServerRepository {

    private static Collection<InetSocketAddress> servers = 
        new HashSet<InetSocketAddress>(StunConstants.SERVERS);
    
    public static void setStunServers(
        final Collection<InetSocketAddress> ss) {
        if (!ss.isEmpty()) {
            servers = ss;
        }
    }
    
    public static Collection<InetSocketAddress> getServers() {
        return servers;
    }
    
    public static void addStunServers(
        final Collection<InetSocketAddress> ss) {
        synchronized (servers) {
            servers.addAll(ss);
        }
    }
}
