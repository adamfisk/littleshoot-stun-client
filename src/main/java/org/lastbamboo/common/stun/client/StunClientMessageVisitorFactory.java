package org.lastbamboo.common.stun.client;

import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.BindingResponseListener;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Factory for creating STUN client message visitors. 
 */
public class StunClientMessageVisitorFactory implements
    StunMessageVisitorFactory
    {

    private final BindingResponseListener m_bindingResponseListener;

    /**
     * Creates a new message visitor factory for STUN clients.
     * 
     * @param brl The listener for binding response messages
     */
    public StunClientMessageVisitorFactory(final BindingResponseListener brl)
        {
        this.m_bindingResponseListener = brl;
        }

    public StunMessageVisitor createVisitor(final IoSession session)
        {
        return new StunClientMessageVisitor(this.m_bindingResponseListener,
            session);
        }

    }
