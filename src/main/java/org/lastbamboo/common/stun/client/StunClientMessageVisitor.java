package org.lastbamboo.common.stun.client;

import org.lastbamboo.common.stun.stack.message.BindingErrorResponse;
import org.lastbamboo.common.stun.stack.message.BindingSuccessResponse;
import org.lastbamboo.common.stun.stack.message.IcmpErrorStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.transaction.StunClientTransaction;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor for STUN messages on STUN clients. 
 * 
 * @param <T> The type the specific visitor returns.
 */
public class StunClientMessageVisitor<T> extends StunMessageVisitorAdapter<T>
    {

    private final Logger m_log = 
        LoggerFactory.getLogger(StunClientMessageVisitor.class);
    protected final StunTransactionTracker<T> m_transactionTracker;

    /**
     * Creates a new STUN client message visitor.
     * 
     * @param transactionTracker The class that keeps track of transactions.
     */
    public StunClientMessageVisitor(
        final StunTransactionTracker<T> transactionTracker)
        {
        m_transactionTracker = transactionTracker;
        }
    
    public T visitIcmpErrorMesssage(final IcmpErrorStunMessage message)
        {
        if (m_log.isDebugEnabled())
            {
            m_log.debug("Received ICMP error: {}", message);
            }
        return notifyTransaction(message);
        }
    
    public T visitBindingErrorResponse(
        final BindingErrorResponse response)
        {
        if (m_log.isDebugEnabled())
            {
            m_log.debug("Received Binding Error response: {}", response);
            }
        return notifyTransaction(response);
        }

    public T visitBindingSuccessResponse(
        final BindingSuccessResponse response)
        {
        if (m_log.isDebugEnabled())
            {
            m_log.debug("Received Binding Response: {}", response);
            }
        return notifyTransaction(response);
        }
    
    private T notifyTransaction(final StunMessage response)
        {
        final StunClientTransaction<T> ct = 
            this.m_transactionTracker.getClientTransaction(response);
        m_log.debug("Accessed transaction: {}", ct);
        
        if (ct == null)
            {
            // This will happen fairly frequently with UDP because messages
            // are retransmitted in case any are lost.
            m_log.debug("No matching transaction for response: {}", response);
            return null;
            }
        
        return response.accept(ct);
        }

    }
