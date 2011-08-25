package org.lastbamboo.common.stun.client;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.Test;


public class PublicIpAddressTest {

    @Test public void testPublicIP() throws Exception {
        final InetAddress address = new PublicIpAddress().getPublicIpAddress();
        assertTrue("Null address?", address != null);
    }
}
