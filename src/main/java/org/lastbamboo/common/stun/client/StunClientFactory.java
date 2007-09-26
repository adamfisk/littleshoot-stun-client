package org.lastbamboo.common.stun.client;

import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;

/**
 * Interface for classes that create new STUN clients. 
 */
public interface StunClientFactory
    {

    StunClient newStunClient(StunMessageVisitorFactory visitorFactory);
    }
