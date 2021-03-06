package org.exoplatform.commons.utils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;

public class MailUtils {

  public static final String SENDER_NAME_PARAM  = "exo:notificationSenderName";

  public static final String SENDER_EMAIL_PARAM = "exo:notificationSenderEmail";

  private MailUtils() {
    // Util Class contianing static calls
  }

  public static String getSenderName() {
    SettingValue<?> name = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_NAME_PARAM);
    return name != null ? (String) name.getValue() : System.getProperty("exo.notifications.portalname", "eXo");
  }

  public static String getSenderEmail() {
    SettingValue<?> mail = getSettingService().get(Context.GLOBAL, Scope.GLOBAL.id(null), SENDER_EMAIL_PARAM);
    return mail != null ? (String) mail.getValue() : System.getProperty("gatein.email.smtp.from", "noreply@exoplatform.com");
  }

  private static SettingService getSettingService() {
    SettingService settingService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SettingService.class);
    if (settingService == null) {
      settingService = PortalContainer.getInstance().getComponentInstanceOfType(SettingService.class);
    }
    return settingService;
  }

}
