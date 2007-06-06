package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.AbstractStunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Creates a new IO handler for handling protocol events for STUN clients.
 */
public class StunClientIoHandler extends AbstractStunIoHandler
    {
    
    private static final Log LOG = LogFactory.getLog(StunClientIoHandler.class);
    
    /**
     * Creates a new STUN IO handler.
     * 
     * @param visitorFactory The factory for creating message visitors.
     */
    public StunClientIoHandler(final StunMessageVisitorFactory visitorFactory)
        {
        super(visitorFactory);
        }
    
    public void sessionOpened(final IoSession session)
        {
        LOG.debug("Session opened...");
        }

    public void sessionClosed(final IoSession session)
        {
        // Print out total number of bytes read from the remote peer.
        LOG.debug("Total " + session.getReadBytes() + " byte(s)");
        }
    
    public void exceptionCaught(final IoSession session, final Throwable cause)
        {
        LOG.warn("Exception on STUN client", cause);
        session.close();
        }
    
    }
