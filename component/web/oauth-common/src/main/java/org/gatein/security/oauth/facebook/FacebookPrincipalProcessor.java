/*
 * Copyright (C) 2015 eXo Platform SAS.
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

package org.gatein.security.oauth.facebook;

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;
import org.gatein.security.oauth.spi.OAuthPrincipal;
import org.gatein.security.oauth.spi.OAuthPrincipalProcessor;
import org.gatein.security.oauth.utils.OAuthUtils;

/**
 * @author <a href="mailto:tuyennt@exoplatform.com">Tuyen Nguyen The</a>.
 */
public class FacebookPrincipalProcessor implements OAuthPrincipalProcessor {
  @Override
  public User convertToGateInUser(OAuthPrincipal principal) {
    String email = principal.getEmail();
    String username = principal.getUserName();
    if(email != null) {
      int index = email.indexOf('@');
      if(index > 0) {
        username = email.substring(0, index);
      }
    }

    User gateinUser = new UserImpl(OAuthUtils.refineUserName(username));
    gateinUser.setFirstName(principal.getFirstName());
    gateinUser.setLastName(principal.getLastName());
    gateinUser.setEmail(email);
    gateinUser.setDisplayName(principal.getDisplayName());

    return gateinUser;
  }
}
