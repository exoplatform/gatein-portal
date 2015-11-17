package org.picketlink.idm.impl.store.hibernate;

import org.hibernate.SessionFactory;
import org.picketlink.idm.impl.store.hibernate.HibernateIdentityStoreSessionImpl;

/**
 * This class extends org.picketlink.idm.impl.store.hibernate.HibernateIdentityStoreSessionImpl
 * to change the modifier of a method
 * 
 */
public class ExoHibernateIdentityStoreSessionImpl extends HibernateIdentityStoreSessionImpl {

  public ExoHibernateIdentityStoreSessionImpl(SessionFactory sessionFactory, boolean lazyStartOfHibernateTransaction) {
    super(sessionFactory, lazyStartOfHibernateTransaction);
  }

  /**
   * {@inheritDoc}
   * 
   *  Change modifier of this method
   */
  public void startHibernateTransactionIfNotStartedYet() {
    super.startHibernateTransactionIfNotStartedYet();
  }
}
