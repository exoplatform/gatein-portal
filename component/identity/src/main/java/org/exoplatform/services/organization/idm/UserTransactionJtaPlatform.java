/*
 * JBoss, a division of Red Hat
 * Copyright 2012, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.organization.idm;

import javax.transaction.UserTransaction;

import org.hibernate.engine.jndi.JndiException;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;

// We need fallback to "java:jboss/UserTransaction" because "java:comp/UserTransaction" is not available
// during eXo kernel boot in AS7 environment.
// TODO: Remove class once https://issues.jboss.org/browse/HIBERNATE-137 will be fixed
public class UserTransactionJtaPlatform extends JBossAppServerJtaPlatform {
    // Should always work with AS7 even during GateIn(exo kernel) boot from MSC thread
    public static final String JBOSS_CTX_UT_NAME = "java:jboss/UserTransaction";

    @Override
    protected UserTransaction locateUserTransaction() {
        try {
            return (UserTransaction) jndiService().locate(UT_NAME);
        } catch (JndiException jndiException) {
            return (UserTransaction) jndiService().locate(JBOSS_CTX_UT_NAME);
        }
    }
}
