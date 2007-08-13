package org.lastbamboo.common.stun.client;

import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating STUN client message visitors. 
 */
public class StunClientMessageVisitorFactory implements
    StunMessageVisitorFactory
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    private final StunTransactionTracker m_transactionTracker;

    /**
     * Creates a new message visitor factory for STUN clients.
     * 
     * @param transactionTracker The class that keeps track of STUN
     * transactions.
     */
    public StunClientMessageVisitorFactory(
        final StunTransactionTracker transactionTracker)
        {
        m_transactionTracker = transactionTracker;
        }

    public StunMessageVisitor createVisitor(final IoSession session)
        {
        return new StunClientMessageVisitor(this.m_transactionTracker);
        }

    public void onIcmpError()
        {
        m_log.warn("Received ICMP error!!");
        }

    }
