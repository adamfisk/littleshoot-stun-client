package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.AbstractStunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageFactory;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

public class StunClientIoHandler extends AbstractStunIoHandler
    {
    
    private static final Log LOG = LogFactory.getLog(StunClientIoHandler.class);
    private final StunMessageFactory m_messageFactory;
    //private final StunMessageFactory m_messageFactory;
    
    public StunClientIoHandler(final StunMessageFactory messageFactory,
        final StunMessageVisitorFactory visitorFactory)
        {
        super(visitorFactory);
        m_messageFactory = messageFactory;
        }
    
    public void sessionCreated(final IoSession session) throws Exception
        {
        LOG.debug("Created session");
        SessionUtil.initialize(session);
        }
    
    public void sessionOpened(final IoSession session)
        {
        LOG.debug("Session opened...");

        final StunMessage message = 
            this.m_messageFactory.createBindingRequest();
        session.write(message);
        }

    public void sessionClosed(final IoSession session)
        {
        // Print out total number of bytes read from the remote peer.
        System.err.println( "Total " + session.getReadBytes() + " byte(s)" );
        }

    public void sessionIdle(final IoSession session, final IdleStatus status)
        {
        // Close the connection if reader is idle.
        if( status == IdleStatus.READER_IDLE )
            session.close();
        }
    
    public void exceptionCaught(final IoSession session, final Throwable cause)
        {
        cause.printStackTrace();
        session.close();
        }
    
    }
