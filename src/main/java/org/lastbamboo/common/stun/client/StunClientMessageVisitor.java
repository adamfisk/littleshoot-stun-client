package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.BindingResponse;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;

/**
 * A visitor for STUN messages on STUN clients. 
 */
public class StunClientMessageVisitor implements StunMessageVisitor
    {

    private static final Log LOG = 
        LogFactory.getLog(StunClientMessageVisitor.class);
    private final IoSession m_session;

    /**
     * Creates a new STUN client message visitor.
     * 
     * @param session The MINA {@link IoSession}.
     */
    public StunClientMessageVisitor(final IoSession session)
        {
        m_session = session;
        }

    public void visitBindingRequest(final BindingRequest binding)
        {
        LOG.error("Should not receive binding request on client");
        }

    public void visitBindingResponse(final BindingResponse response)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Received binding response: "+response);
            }
        }

    }
