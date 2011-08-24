package org.lastbamboo.common.stun.client;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.channels.UnresolvedAddressException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.littleshoot.stun.stack.StunConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generalized class that uses various techniques to obtain a public IP address.
 * 
 * TODO: We need to add new methods for this -- Apache lookups and IRC 
 * lookups.
 * 
 * Google: 
 * 
 * Use https://encrypted.google.com/
 * inurl:"server-status" "Apache Server Status for"  
 * 
 * IRC: perl -MIO -e'$x=IO::Socket::INET->new("$ARGV[0]:6667");print $x "USER x x x x\nNICK testip$$\nWHOIS testip$$\n";while(<$x>){if(/PING (\S+)/){print $x "PONG $1\n"}elsif(/^\S+ 378 .* (\S+)/){die$1}}' irc.freenode.org
 *
 * Many thanks to Samy Kamkar!!
 */
public class PublicIpAddress {

    private static final Logger LOG = 
        LoggerFactory.getLogger(PublicIpAddress.class);
    private static InetAddress publicIp;
    private static long lastLookupTime;
    
    private PublicIpAddress() {}
    
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
        
        // Note these will all fail if you don't have a network connection --
        // that should not be confused with them being blocked. 
        try {
            final StunClient stun = new UdpStunClient(StunConstants.SERVERS);
            stun.connect();
            publicIp = stun.getServerReflexiveAddress().getAddress();
            lastLookupTime = System.currentTimeMillis();
            return publicIp;
        } catch (final IOException e) {
            LOG.warn("Could not get server reflexive address", e);
        } catch (final UnresolvedAddressException e) {
            LOG.warn("Not connected to the network?", e);
        }
        publicIp = wikiMediaLookup();
        if (publicIp != null) {
            lastLookupTime = System.currentTimeMillis();
            return publicIp;
        }
        publicIp = ifConfigLookup();
        if (publicIp != null) {
            lastLookupTime = System.currentTimeMillis();
            return publicIp;
        }
        return null;
    }

    private static InetAddress ifConfigLookup() {
        final HttpClient client = new HttpClient();
        final GetMethod get = new GetMethod("http://ifconfig.me");
        // The service returns just the IP if we pretend we're curl.
        get.setRequestHeader("User-Agent", 
            "curl/7.19.7 (universal-apple-darwin10.0) libcurl/7.19.7 OpenSSL/0.9.8r zlib/1.2.3");
        get.setFollowRedirects(true);
        try {
            final int response = client.executeMethod(get);
            if (response < 200 || response > 299) {
                LOG.warn("Got non-200 level response: "+response);
                return null;
            }
            final String body = new String(get.getResponseBody(), "UTF-8");
            LOG.info("Got response body:\n{}", body);
            return InetAddress.getByName(body.trim());
        } catch (final HttpException e) {
            LOG.warn("HTTP error?", e);
        } catch (final IOException e) {
            LOG.warn("Error connecting?", e);
        } catch (final Exception e) {
            LOG.warn("Some other error?", e);
        } finally {
            get.releaseConnection();
        }
        return null;
    }

    private static InetAddress wikiMediaLookup() {
        final HttpClient client = new HttpClient();
        final GetMethod get = 
            new GetMethod("http://geoiplookup.wikimedia.org/");
        get.setFollowRedirects(true);
        try {
            final int response = client.executeMethod(get);
            if (response < 200 || response > 299) {
                LOG.warn("Got non-200 level response: "+response);
                return null;
            }
            final String body = new String(get.getResponseBody(), "UTF-8");
            LOG.info("Got response body:\n{}", body);
            
            final String jsonStr = StringUtils.substringAfter(body, "=").trim();
            final JSONObject json = (JSONObject) JSONValue.parse(jsonStr);
            final String inet = (String) json.get("IP");
            return InetAddress.getByName(inet);
        } catch (final HttpException e) {
            LOG.warn("HTTP error?", e);
        } catch (final IOException e) {
            LOG.warn("Error connecting?", e);
        } catch (final Exception e) {
            LOG.warn("Some other error?", e);
        } finally {
            get.releaseConnection();
        }
        return null;
    }
}
