package org.lastbamboo.common.stun.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.BindingResponse;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.transaction.StunClientTransaction;
import org.lastbamboo.common.stun.stack.transaction.StunTransactionTracker;

/**
 * A visitor for STUN messages on STUN clients. 
 */
public class StunClientMessageVisitor implements StunMessageVisitor
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
        notifyTransaction(response);
        }
    
    private void notifyTransaction(final BindingResponse response)
        {
        final StunClientTransaction ct = 
            this.m_transactionTracker.getClientTransaction(response);
        LOG.debug("Accessed transaction: "+ct);
        
        if (ct == null)
            {
            // This will happen fairly frequently with UDP because messages
            // are retransmitted in case any are lost.
            LOG.debug("No matching transaction for response: "+response);
            return;
            }
        
        response.accept(ct);
        }

    }
