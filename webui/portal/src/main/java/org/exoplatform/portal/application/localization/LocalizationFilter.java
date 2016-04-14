/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.exoplatform.portal.application.localization;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.security.ConversationState;

/**
 * This filter provides {@link HttpServletRequest#getLocale()} and {@link HttpServletRequest#getLocales()} override for
 * extra-portlet requests (i.e. unbridged .jsp). Thanks to it dynamic resources can be localized to keep in sync with the rest
 * of the portal. This filter is re-entrant, and can safely be installed for INCLUDE, FORWARD, and ERROR dispatch methods.
 * <p>
 * A concrete example of re-entrant use is login/jsp/login.jsp used when authentication fails at portal login.
 * <p>
 * By default {@link HttpServletRequest#getLocale()} and {@link HttpServletRequest#getLocales()} reflect browser language
 * preference. When using this filter these two calls employ the same Locale determination algorithm as
 * {@link LocalizationLifecycle} does.
 * <p>
 * This filter can be activated / deactivated via portal module's web.xml
 * <p>
 * If default portal language is other than English, it can be configured for the filter by using PortalLocale init param:
 * <p>
 *
 * <pre>
 * &lt;filter&gt;
 *   &lt;filter-name&gt;LocalizationFilter&lt;/filter-name&gt;
 *   &lt;filter-class&gt;org.exoplatform.portal.application.localization.LocalizationFilter&lt;/filter-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name>PortalLocale&lt;/param-name&gt;
 *     &lt;param-value>fr_FR&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * &lt;/filter&gt;
 * </pre>
 *
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class LocalizationFilter extends AbstractFilter {
    private static final String LOCALE_SESSION_ATTR = "user.locale";
    private static Log log = ExoLogger.getLogger("portal:LocalizationFilter");

    private static ThreadLocal<Locale> currentLocale = new ThreadLocal<Locale>();

    private Locale portalLocale = Locale.ENGLISH;

    @Override
    protected void afterInit(FilterConfig config) throws ServletException {
        String locale = config.getInitParameter("PortalLocale");
        locale = locale != null ? locale.trim() : null;
        if (locale != null && locale.length() > 0)
            portalLocale = LocaleContextInfo.getLocale(locale);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            // context is null in normal HTTP requests because it's not yet built
            // It is not null in case of include or forwad using dispatcher
            // Due to forwards, and includes the filter might be reentered
            // If current requestContext exists use its Locale
            PortalRequestContext context = PortalRequestContext.getCurrentInstance();
            if (context != null && context.getLocale() != null) {
                // No need to wrap if reentered
                boolean skipWrapping = currentLocale.get() != null;
                // overwrite any already set currentLocale
                currentLocale.set(context.getLocale());
                if (!skipWrapping) {
                    req = new HttpRequestWrapper(req);
                }
                chain.doFilter(req, res);
                return;
            }

            // If reentered we don't need to wrap
            if (currentLocale.get() != null) {
                chain.doFilter(request, response);
                return;
            }

            ConversationState state = ConversationState.getCurrent();
            if(state != null) {
              Locale locale = CommonLocalePolicyService.getLocaleFromState(state);
              // FIXME workaround of COR-357
              if (locale == null && req.getRemoteUser() == null) {
                locale = (Locale) req.getSession().getAttribute(LOCALE_SESSION_ATTR);
              }
              // End workaround
              if(locale != null) {
                currentLocale.set(locale);
                chain.doFilter(request, response);
                return;
              }
            }

            // Initialize currentLocale
            ExoContainer container = getContainer();
            if (container == null) {
                // Nothing we can do, move on
                chain.doFilter(req, res);
                return;
            }

            LocalePolicy localePolicy = (LocalePolicy) container.getComponentInstanceOfType(LocalePolicy.class);

            LocaleContextInfo localeCtx = new LocaleContextInfo();
            localeCtx.setRemoteUser(req.getRemoteUser());

            // First locale to check
            Locale userProfileLocale = getUserProfileLocale(container, req.getRemoteUser());
            localeCtx.setUserProfileLocale(userProfileLocale);
            // Second locale to check
            localeCtx.setBrowserLocales(Collections.list(request.getLocales()));
            // Third locale to check
            localeCtx.setPortalLocale(portalLocale);

            Locale locale = localePolicy.determineLocale(localeCtx);

            if (userProfileLocale == null && req.getRemoteUser() != null) {
              setUserProfileLocale(container, req.getRemoteUser(), locale.getLanguage());
            } else if(req.getRemoteUser() == null) {
              // FIXME workaround of COR-357
              req.getSession().setAttribute(LOCALE_SESSION_ATTR, locale);
              // End workaround
            }

            currentLocale.set(locale);
            chain.doFilter(new HttpRequestWrapper(req), res);
        } catch (Exception e) {
            throw new RuntimeException("LocalizationFilter exception: ", e);
        } finally {
            currentLocale.remove();
        }
    }

    private Locale getUserProfileLocale(ExoContainer container, String user) {
        UserProfile userProfile = null;
        OrganizationService svc = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

        if (user != null) {
            try {
                beginContext(svc);
                userProfile = svc.getUserProfileHandler().findUserProfileByName(user);
            } catch (Exception ignored) {
                log.error("IGNORED: Failed to load UserProfile for username: " + user, ignored);
            } finally {
                try {
                    endContext(svc);
                } catch (Exception ignored) {
                    // we don't care
                }
            }

            if (userProfile == null && log.isDebugEnabled())
                log.debug("Could not load user profile for " + user);
        }

        String lang = userProfile == null ? null : userProfile.getUserInfoMap().get(Constants.USER_LANGUAGE);
        return (lang != null) ? LocaleContextInfo.getLocale(lang) : null;
    }

    private void setUserProfileLocale(ExoContainer container, String user, String language) {
      if (user == null) {
        return;
      }
  
      OrganizationService svc = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
      try {
        beginContext(svc);
        UserProfile userProfile = svc.getUserProfileHandler().findUserProfileByName(user);
        if(userProfile != null) {
          String lang = userProfile.getAttribute(Constants.USER_LANGUAGE);
          if(lang == null || !language.equals(lang)) {
            userProfile.setAttribute(Constants.USER_LANGUAGE, language);
            svc.getUserProfileHandler().saveUserProfile(userProfile, false);
          }
        }
      } catch (Exception ignored) {
        log.error("IGNORED: Failed to load UserProfile for username: " + user, ignored);
      } finally {
        try {
          endContext(svc);
        } catch (Exception ignored) {
          // we don't care
        }
      }
    }

    public void beginContext(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
        }
    }

    public void endContext(OrganizationService orgService) {
        // do the same check as in beginContext to make it symmetric
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.end();
        }
    }

    public void destroy() {
    }

    public static Locale getCurrentLocale() {
        return currentLocale.get();
    }
}
