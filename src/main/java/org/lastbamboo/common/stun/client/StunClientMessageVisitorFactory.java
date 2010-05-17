package org.lastbamboo.common.stun.client;

import org.littleshoot.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;

/**
 * Factory for creating STUN client message visitors.
 *  
 * @param <T> The type created visitors return.
 */
public class StunClientMessageVisitorFactory<T> implements
    StunMessageVisitorFactory<T>
    {

    private final StunTransactionTracker<T> m_transactionTracker;

    /**
     * Creates a new message visitor factory for STUN clients.
     * 
     * @param transactionTracker The class that keeps track of STUN
     * transactions.
     */
    public StunClientMessageVisitorFactory(
        final StunTransactionTracker<T> transactionTracker)
        {
        m_transactionTracker = transactionTracker;
        }

    public StunMessageVisitor<T> createVisitor(final IoSession session)
        {
        return new StunClientMessageVisitor<T>(this.m_transactionTracker);
        }
    }
