package org.exoplatform.portal.jdbc.migration;

import java.lang.reflect.Field;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.picocontainer.Startable;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class RDBMSMigrationManager implements Startable {
  private static final Log     LOG                          = ExoLogger.getLogger(RDBMSMigrationManager.class);

  public static final String   MIGRATION_SETTING_GLOBAL_KEY = "MIGRATION_SETTING_GLOBAL";

  private Thread               migrationThread;

  private SiteMigrationService siteMigrationService;
  
  private PageMigrationService pageMigrationService;
  
  private NavigationMigrationService navMigrationService;
  
  private AppRegistryMigrationService appMigrationService;

  // TODO: need move setting service from common into gatein
  private SettingService settingService;

  public RDBMSMigrationManager(SiteMigrationService siteMigrationService,
                               PageMigrationService pageMigrationService,
                               NavigationMigrationService navMigrationService,
                               AppRegistryMigrationService appMigrationService,
                               SettingService settingService) {
    this.siteMigrationService = siteMigrationService;
    this.pageMigrationService = pageMigrationService;
    this.navMigrationService = navMigrationService;
    this.appMigrationService = appMigrationService;
    this.settingService = settingService;
  }

  @Override
  public void start() {
    initMigrationSetting();

    Runnable migrateTask = new Runnable() {
      @Override
      public void run() {
        //
        Field field = null;
        try {
          if (!MigrationContext.isDone()) {
            field = SessionImpl.class.getDeclaredField("FORCE_USE_GET_NODES_LAZILY");
            if (field != null) {
              field.setAccessible(true);
              field.set(null, true);
            }
            //
            LOG.info("START ASYNC MIGRATION---------------------------------------------------");
            //
            if (!MigrationContext.isDone()) {
              if (!MigrationContext.isSiteDone()) {
                siteMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_MIGRATION_KEY, MigrationContext.isSiteDone());
              }

              if (MigrationContext.isSiteDone() && !MigrationContext.isPageDone()) {
                pageMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY, MigrationContext.isPageDone());
              }
              
              if (MigrationContext.isPageDone() && !MigrationContext.isNavDone()) {
                navMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY, MigrationContext.isNavDone());
              }

              
              if (MigrationContext.isNavDone() && !MigrationContext.isAppDone()) {
                appMigrationService.start();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY, MigrationContext.isAppDone());
              }
            }
            //
            LOG.info("END ASYNC MIGRATION-----------------------------------------------------");

            if (MigrationContext.isDone()) {
              LOG.info("START CLEANUP PORTAL DATA ---------------------------------------------------");

              // Site
              if (MigrationContext.isSiteDone() && !MigrationContext.isSiteCleanupDone()) {
                siteMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_CLEANUP_KEY, Boolean.TRUE);
              }

              // Page
              if (MigrationContext.isPageDone() && !MigrationContext.isPageCleanupDone()) {
                pageMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY, Boolean.TRUE);
              }

              // cleanup
              if (MigrationContext.isNavDone() && MigrationContext.isNavCleanupDone()) {
                navMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY, Boolean.TRUE);
              }

              // cleanup
              if (MigrationContext.isAppDone() && !MigrationContext.isAppCleanupDone()) {
                appMigrationService.doRemove();
                updateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY, Boolean.TRUE);

                updateSettingValue(MigrationContext.PORTAL_RDBMS_MIGRATION_STATUS_KEY, Boolean.TRUE);
                MigrationContext.setDone(true);
              }

              LOG.info("END CLEANUP PORTAL DATA -----------------------------------------------------");
            }
          }

        } catch (Exception e) {
          LOG.error("Failed to running Migration data from JCR to RDBMS", e);
        } finally {
          if (field != null) {
            try {
              field.set(null, false);
            } catch (Exception e) {
              LOG.warn(e.getMessage(), e);
            }
          }
        }
      }
    };
    this.migrationThread = new Thread(migrateTask);
    this.migrationThread.setPriority(Thread.NORM_PRIORITY);
    this.migrationThread.setName("PORTAL-MIGRATION-RDBMS");
    this.migrationThread.start();
  }

  private void initMigrationSetting() {
    MigrationContext.setDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_MIGRATION_STATUS_KEY));
    //
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_SITE_CLEANUP_KEY));
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_PAGE_CLEANUP_KEY));
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_NAV_CLEANUP_KEY));
    
    MigrationContext.setSiteDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_MIGRATION_KEY));
    MigrationContext.setSiteCleanupDone(getOrCreateSettingValue(MigrationContext.PORTAL_RDBMS_APP_CLEANUP_KEY));
  }

  private boolean getOrCreateSettingValue(String key) {
    SettingValue<Boolean> setting = (SettingValue<Boolean>)this.settingService.get(Context.GLOBAL, Scope.GLOBAL.id(null), key);
    if (setting != null) {
      return setting.getValue();
    } else {
      updateSettingValue(key, Boolean.FALSE);
      return false;
    }
  }

  private void updateSettingValue(String key, Boolean status) {
    settingService.set(Context.GLOBAL, Scope.GLOBAL.id(null), key, SettingValue.create(status));
  }

  @Override
  public void stop() {
    siteMigrationService.stop();
    try {
      this.migrationThread.join();
    } catch (InterruptedException e) {
      LOG.error(e);
    }
  }

}
