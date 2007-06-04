package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.stun.stack.BindingResponseListener;
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
    private final BindingResponseListener m_bindingResponseListener;

    /**
     * Creates a new STUN client message visitor.
     * 
     * @param listener The listener for binding response messages. 
     */
    public StunClientMessageVisitor(final BindingResponseListener listener)
        {
        m_bindingResponseListener = listener;
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
        
        this.m_bindingResponseListener.onBindingResponse(response);
        }

    }
