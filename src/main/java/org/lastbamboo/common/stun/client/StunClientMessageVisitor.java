package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.stun.stack.message.BindingErrorResponse;
import org.lastbamboo.common.stun.stack.message.BindingSuccessResponse;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.transaction.StunClientTransaction;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;

/**
 * A visitor for STUN messages on STUN clients. 
 */
public class StunClientMessageVisitor<T> extends StunMessageVisitorAdapter<T>
    {

    private static final Log LOG = 
        LogFactory.getLog(StunClientMessageVisitor.class);
    private final StunTransactionTracker m_transactionTracker;

    /**
     * Creates a new STUN client message visitor.
     * 
     * @param transactionTracker The class that keeps track of transactions.
     */
    public StunClientMessageVisitor(
        final StunTransactionTracker transactionTracker)
        {
        m_transactionTracker = transactionTracker;
        }
    
    public T visitBindingErrorResponse(
        final BindingErrorResponse response)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Received binding error response: "+response);
            }
        return notifyTransaction(response);
        }

    public T visitBindingSuccessResponse(
        final BindingSuccessResponse response)
        {
        if (LOG.isDebugEnabled())
            {
            LOG.debug("Received binding response: "+response);
            }
        return notifyTransaction(response);
        }
    
    private T notifyTransaction(final StunMessage response)
        {
        final StunClientTransaction<T> ct = 
            this.m_transactionTracker.getClientTransaction(response);
        LOG.debug("Accessed transaction: "+ct);
        
        if (ct == null)
            {
            // This will happen fairly frequently with UDP because messages
            // are retransmitted in case any are lost.
            LOG.debug("No matching transaction for response: "+response);
            return null;
            }
        
        return response.accept(ct);
        }

    }
