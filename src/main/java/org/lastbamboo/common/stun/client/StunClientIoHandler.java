package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.AbstractStunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Creates a new IO handler for handling protocol events for STUN clients.
 */
public class StunClientIoHandler extends AbstractStunIoHandler
    {
    
    private static final Log LOG = LogFactory.getLog(StunClientIoHandler.class);
    private final StunMessageFactory m_messageFactory;
    
    /**
     * Creates a new STUN IO handler.
     * 
     * @param messageFactory The factory for creating STUN messages.
     * @param visitorFactory The factory for creating message visitors.
     */
    public StunClientIoHandler(final StunMessageFactory messageFactory,
        final StunMessageVisitorFactory visitorFactory)
        {
        super(visitorFactory);
        m_messageFactory = messageFactory;
        }
    
    public void sessionOpened(final IoSession session)
        {
        LOG.debug("Session opened...");

        // Write a binding request.
        final StunMessage message = 
            this.m_messageFactory.createBindingRequest();
        session.write(message);
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
