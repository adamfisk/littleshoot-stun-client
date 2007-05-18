package org.lastbamboo.common.stun.client;

import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Factory for creating STUN client message visitors. 
 */
public class StunClientMessageVisitorFactory implements
    StunMessageVisitorFactory
    {

    public StunMessageVisitor createVisitor(final IoSession session)
        {
        return new StunClientMessageVisitor(session);
        }

    }
