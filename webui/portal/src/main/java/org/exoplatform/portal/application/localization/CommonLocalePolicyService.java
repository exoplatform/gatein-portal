package org.exoplatform.portal.application.localization;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.services.security.ConversationState;
import org.picocontainer.Startable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Racha on 07/04/2016.
 */
public class CommonLocalePolicyService  implements LocalePolicy, Startable {

    private static Log log = ExoLogger.getLogger(CommonLocalePolicyService.class);

    private LocaleConfigService localeConfigService;

    public CommonLocalePolicyService(LocaleConfigService localeConfigService) {
        this.localeConfigService = localeConfigService;
    }

    public static Locale determineLocale(ExoContainer container, String userId) {
        ConversationState currentState = ConversationState.getCurrent();
        if (userId != null && currentState != null && userId.equals(currentState.getIdentity().getUserId())) {
            Locale locale = getLocaleFromState(currentState);
            if (locale != null) {
                return locale;
            }
        }
        Locale locale = getUserProfileLocale(container, userId);
        if (locale != null) {
            return locale;
        }

        locale = Locale.getDefault();

        DataStorage dataStorage = (DataStorage) container.getComponentInstanceOfType(DataStorage.class);
        UserPortalConfigService portalConfigService = (UserPortalConfigService) container.getComponentInstanceOfType(UserPortalConfigService.class);
        PortalConfig pConfig = null;
        try {
            pConfig = dataStorage.getPortalConfig(SiteType.PORTAL.getName(), portalConfigService.getDefaultPortal());
            if (pConfig == null) {
                log.warn("No UserPortalConfig available! Portal locale set to 'en'");
            } else {
                locale = new Locale(pConfig.getLocale());
            }
        } catch (Exception ignored) {
            if (log.isDebugEnabled())
                log.debug("IGNORED: Failed to load UserPortalConfig: ", ignored);
        }

        return locale;
    }

    /**
     * @see LocalePolicy#determineLocale(org.exoplatform.services.resources.LocaleContextInfo)
     */
    public Locale determineLocale(LocaleContextInfo context) {
        if (context.getRequestLocale() != null) {
            return context.getRequestLocale();
        }

        ConversationState currentState = ConversationState.getCurrent();
        if (context.getRemoteUser() != null && currentState != null) {
            Locale locale = getLocaleFromState(currentState);
            if (locale != null) {
                return locale;
            }
        }

        if(context.getSupportedLocales() == null) {
            Set<Locale> supportedLocales = new HashSet<Locale>();
            for (LocaleConfig lc : localeConfigService.getLocalConfigs()) {
                supportedLocales.add(lc.getLocale());
            }
            context.setSupportedLocales(supportedLocales);
        }

        //
        Locale locale = null;
        if (context.getRemoteUser() == null)
            locale = getLocaleConfigForAnonymous(context);
        else
            locale = getLocaleConfigForRegistered(context);

        if (locale == null) {
            locale = context.getLocaleIfLangSupported(context.getPortalLocale());
            if (locale == null) {
                log.warn("Unsupported PortalConfig locale: " + context.getPortalLocale() + ". Falling back to default JVM locale.");
                locale = Locale.getDefault();
            }
        }

        if(currentState !=null) {
            currentState.setAttribute(Constants.USER_LANGUAGE, locale);
        }

        return locale;
    }

    public static Locale getLocaleFromState(ConversationState state) {
        Locale locale = (Locale) state.getAttribute(Constants.USER_LANGUAGE);
        if(locale != null) {
            return locale;
        }
        return null;
    }

    /**
     * Override this method to change the LocaleConfig determination for registered users. Default is: use user's profile
     * language, if not available fall back to LOCALE cookie, and finally if that is not available either fall back to browser
     * language preference.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigForRegistered(LocaleContextInfo context) {
        Locale locale = context.getLocaleIfLangSupported(context.getUserProfileLocale());
        if (locale == null)
            locale = getLocaleConfigFromCookie(context);
        if (locale == null)
            locale = getLocaleConfigFromSession(context);
        if (locale == null)
            locale = getLocaleConfigFromBrowser(context);

        return locale;
    }

    /**
     * Override this method to change the Locale determination based on browser language preferences. If you want to disable the
     * use of browser language preferences simply return null.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromBrowser(LocaleContextInfo context) {
        List<Locale> locales = context.getBrowserLocales();
        if (locales != null) {
            for (Locale loc : locales)
                return context.getLocaleIfLangSupported(loc);
        }
        return null;
    }

    /**
     * Override this method to change Locale determination for users that aren't logged in. By default the request's LOCALE
     * cookie is used, if that is not available the browser language preferences are used.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigForAnonymous(LocaleContextInfo context) {
        Locale locale = getLocaleConfigFromCookie(context);
        if (locale == null)
            locale = getLocaleConfigFromSession(context);
        if (locale == null)
            locale = getLocaleConfigFromBrowser(context);

        return locale;
    }

    /**
     * Override this method to change the Locale determination based on session attribute. Note: this is mostly a backup for
     * cookie, as either one usually has to be enabled for locale to remain synchronized between portal and non-portal pages.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromSession(LocaleContextInfo context) {
        return context.getSessionLocale();
    }

    /**
     * Override this method to change the Locale determination based on browser cookie. If you want to disable the use of
     * browser cookies simply return null.
     *
     * @param context locale context info available to implementations in order to determine appropriate Locale
     * @return Locale representing a language to use, or null
     */
    protected Locale getLocaleConfigFromCookie(LocaleContextInfo context) {
        List<Locale> locales = context.getCookieLocales();
        if (locales != null) {
            for (Locale locale : locales)
                return context.getLocaleIfLangSupported(locale);
        }
        return null;
    }

    /**
     * Starter interface method
     */
    public void start() {
    }

    /**
     * Starter interface method
     */
    public void stop() {
    }

    private static Locale getUserProfileLocale(ExoContainer container, String user) {
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

    public static void beginContext(OrganizationService orgService) {
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.begin((ComponentRequestLifecycle) orgService);
        }
    }

    public static void endContext(OrganizationService orgService) {
        // do the same check as in beginContext to make it symmetric
        if (orgService instanceof ComponentRequestLifecycle) {
            RequestLifeCycle.end();
        }

    }


}
