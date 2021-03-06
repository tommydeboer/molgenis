package org.molgenis.integrationtest.platform;

import static org.molgenis.data.postgresql.PostgreSqlRepositoryCollection.POSTGRESQL;

import org.molgenis.data.EntityFactoryRegistrar;
import org.molgenis.data.RepositoryCollectionBootstrapper;
import org.molgenis.data.SystemRepositoryDecoratorFactoryRegistrar;
import org.molgenis.data.decorator.DynamicRepositoryDecoratorFactoryRegistrar;
import org.molgenis.data.decorator.meta.DynamicDecoratorPopulator;
import org.molgenis.data.event.BootstrappingEventPublisher;
import org.molgenis.data.i18n.I18nPopulator;
import org.molgenis.data.meta.system.SystemEntityTypeRegistrar;
import org.molgenis.data.meta.system.SystemPackageRegistrar;
import org.molgenis.data.platform.bootstrap.SystemEntityTypeBootstrapper;
import org.molgenis.data.postgresql.identifier.EntityTypeRegistryPopulator;
import org.molgenis.data.transaction.TransactionExceptionTranslatorRegistrar;
import org.molgenis.data.transaction.TransactionManager;
import org.molgenis.jobs.JobFactoryRegistrar;
import org.molgenis.security.acl.DataSourceAclTablesPopulator;
import org.molgenis.security.core.runas.RunAsSystemAspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** TODO code duplication with org.molgenis.bootstrap.MolgenisBootstrapper */
@Component
public class PlatformBootstrapper {
  private static final Logger LOG = LoggerFactory.getLogger(PlatformITConfig.class);

  private final DataSourceAclTablesPopulator dataSourceAclTablesPopulator;
  private final TransactionExceptionTranslatorRegistrar transactionExceptionTranslatorRegistrar;
  private final RepositoryCollectionBootstrapper repoCollectionBootstrapper;
  private final SystemEntityTypeRegistrar systemEntityTypeRegistrar;
  private final SystemPackageRegistrar systemPackageRegistrar;
  private final EntityFactoryRegistrar entityFactoryRegistrar;
  private final SystemEntityTypeBootstrapper systemEntityTypeBootstrapper;
  private final SystemRepositoryDecoratorFactoryRegistrar systemRepositoryDecoratorFactoryRegistrar;
  private final JobFactoryRegistrar jobFactoryRegistrar;
  private final DynamicRepositoryDecoratorFactoryRegistrar
      dynamicRepositoryDecoratorFactoryRegistrar;
  private final BootstrappingEventPublisher bootstrappingEventPublisher;
  private final TransactionManager transactionManager;
  private final I18nPopulator i18nPopulator;
  private final DynamicDecoratorPopulator dynamicDecoratorPopulator;
  private final EntityTypeRegistryPopulator entityTypeRegistryPopulator;

  PlatformBootstrapper(
      DataSourceAclTablesPopulator dataSourceAclTablesPopulator,
      TransactionExceptionTranslatorRegistrar transactionExceptionTranslatorRegistrar,
      RepositoryCollectionBootstrapper repoCollectionBootstrapper,
      SystemEntityTypeRegistrar systemEntityTypeRegistrar,
      SystemPackageRegistrar systemPackageRegistrar,
      EntityFactoryRegistrar entityFactoryRegistrar,
      SystemEntityTypeBootstrapper systemEntityTypeBootstrapper,
      SystemRepositoryDecoratorFactoryRegistrar systemRepositoryDecoratorFactoryRegistrar,
      JobFactoryRegistrar jobFactoryRegistrar,
      DynamicRepositoryDecoratorFactoryRegistrar dynamicRepositoryDecoratorFactoryRegistrar,
      BootstrappingEventPublisher bootstrappingEventPublisher,
      TransactionManager transactionManager,
      I18nPopulator i18nPopulator,
      DynamicDecoratorPopulator dynamicDecoratorPopulator,
      EntityTypeRegistryPopulator entityTypeRegistryPopulator) {
    this.dataSourceAclTablesPopulator = dataSourceAclTablesPopulator;
    this.transactionExceptionTranslatorRegistrar = transactionExceptionTranslatorRegistrar;
    this.repoCollectionBootstrapper = repoCollectionBootstrapper;
    this.systemEntityTypeRegistrar = systemEntityTypeRegistrar;
    this.systemPackageRegistrar = systemPackageRegistrar;
    this.entityFactoryRegistrar = entityFactoryRegistrar;
    this.systemEntityTypeBootstrapper = systemEntityTypeBootstrapper;
    this.systemRepositoryDecoratorFactoryRegistrar = systemRepositoryDecoratorFactoryRegistrar;
    this.jobFactoryRegistrar = jobFactoryRegistrar;
    this.dynamicRepositoryDecoratorFactoryRegistrar = dynamicRepositoryDecoratorFactoryRegistrar;
    this.bootstrappingEventPublisher = bootstrappingEventPublisher;
    this.transactionManager = transactionManager;
    this.i18nPopulator = i18nPopulator;
    this.dynamicDecoratorPopulator = dynamicDecoratorPopulator;
    this.entityTypeRegistryPopulator = entityTypeRegistryPopulator;
  }

  public void bootstrap(ContextRefreshedEvent event) {
    TransactionTemplate transactionTemplate = new TransactionTemplate();
    transactionTemplate.setTransactionManager(transactionManager);
    transactionTemplate.execute(
        (action) -> {
          try {
            RunAsSystemAspect.runAsSystem(
                () -> {
                  LOG.info("Bootstrapping registries ...");
                  bootstrappingEventPublisher.publishBootstrappingStartedEvent();

                  LOG.trace("Populating data source with ACL tables ...");
                  dataSourceAclTablesPopulator.populate();
                  LOG.debug("Populated data source with ACL tables");

                  LOG.trace("Bootstrapping transaction exception translators ...");
                  transactionExceptionTranslatorRegistrar.register(event.getApplicationContext());
                  LOG.debug("Bootstrapped transaction exception translators");

                  LOG.trace("Registering repository collections ...");
                  repoCollectionBootstrapper.bootstrap(event, POSTGRESQL);
                  LOG.trace("Registered repository collections");

                  LOG.trace("Registering system entity meta data ...");
                  systemEntityTypeRegistrar.register(event);
                  LOG.trace("Registered system entity meta data");

                  LOG.trace("Registering system packages ...");
                  systemPackageRegistrar.register(event);
                  LOG.trace("Registered system packages");

                  LOG.trace("Registering entity factories ...");
                  entityFactoryRegistrar.register(event);
                  LOG.trace("Registered entity factories");

                  LOG.trace("Registering entity factories ...");
                  systemRepositoryDecoratorFactoryRegistrar.register(event);
                  LOG.trace("Registered entity factories");
                  LOG.debug("Bootstrapped registries");

                  LOG.trace("Registering dynamic decorator factories ...");
                  dynamicRepositoryDecoratorFactoryRegistrar.register(
                      event.getApplicationContext());
                  LOG.trace("Registered dynamic repository decorator factories");

                  LOG.trace("Bootstrapping system entity types ...");
                  systemEntityTypeBootstrapper.bootstrap(event);
                  LOG.debug("Bootstrapped system entity types");

                  LOG.trace("Registering job factories ...");
                  jobFactoryRegistrar.register(event);
                  LOG.trace("Registered job factories");

                  LOG.trace("Populating database with I18N strings ...");
                  i18nPopulator.populateL10nStrings();
                  LOG.trace("Populated database with I18N strings");

                  LOG.trace("Populating database with Dynamic Decorator Configurations ...");
                  dynamicDecoratorPopulator.populate();
                  LOG.trace("Populated database with Dynamic Decorators Configurations");

                  LOG.trace("Populating the entity type registry ...");
                  entityTypeRegistryPopulator.populate();
                  LOG.trace("Populated the entity type registry");

                  bootstrappingEventPublisher.publishBootstrappingFinishedEvent();
                });
          } catch (Exception unexpected) {
            LOG.error("Error bootstrapping tests!", unexpected);
            throw new RuntimeException(unexpected);
          }
          return (Void) null;
        });
  }
}
