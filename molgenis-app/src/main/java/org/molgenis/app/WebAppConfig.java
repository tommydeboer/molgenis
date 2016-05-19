package org.molgenis.app;

import java.io.IOException;
import java.util.Map;

import org.molgenis.CommandLineOnlyConfiguration;
import org.molgenis.DatabaseConfig;
import org.molgenis.data.DataService;
import org.molgenis.data.RepositoryCollection;
import org.molgenis.data.config.HttpClientConfig;
import org.molgenis.data.elasticsearch.ElasticsearchRepositoryCollection;
import org.molgenis.data.elasticsearch.config.EmbeddedElasticSearchConfig;
import org.molgenis.data.elasticsearch.factory.EmbeddedElasticSearchServiceFactory;
import org.molgenis.data.system.RepositoryTemplateLoader;
import org.molgenis.dataexplorer.freemarker.DataExplorerHyperlinkDirective;
import org.molgenis.migrate.version.v1_11.Step20RebuildElasticsearchIndex;
import org.molgenis.migrate.version.v1_11.Step21SetLoggingEventBackend;
import org.molgenis.migrate.version.v1_13.Step22RemoveDiseaseMatcher;
import org.molgenis.migrate.version.v1_14.Step23RebuildElasticsearchIndex;
import org.molgenis.migrate.version.v1_15.Step24UpdateApplicationSettings;
import org.molgenis.migrate.version.v1_15.Step25LanguagesPermissions;
import org.molgenis.migrate.version.v1_16.Step26migrateJpaBackend;
import org.molgenis.migrate.version.v1_17.Step27MetaDataAttributeRoles;
import org.molgenis.migrate.version.v1_19.Step28MigrateSorta;
import org.molgenis.ui.MolgenisWebAppConfig;
import org.molgenis.util.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.google.gson.Gson;

import freemarker.template.TemplateException;

@Configuration
@EnableTransactionManagement
@EnableWebMvc
@EnableAsync
@ComponentScan(basePackages = "org.molgenis", excludeFilters = @Filter(type = FilterType.ANNOTATION, value = CommandLineOnlyConfiguration.class))
@Import(
{ WebAppSecurityConfig.class, DatabaseConfig.class, HttpClientConfig.class, EmbeddedElasticSearchConfig.class,
		GsonConfig.class })
public class WebAppConfig extends MolgenisWebAppConfig
{
	private static final Logger LOG = LoggerFactory.getLogger(WebAppConfig.class);

	@Autowired
	private DataService dataService;

	@Autowired
	@Qualifier("PostgreSqlRepositoryCollection")
	private RepositoryCollection postgreSqlRepositoryCollection;

	@Autowired
	private ElasticsearchRepositoryCollection elasticsearchRepositoryCollection;

	@Autowired
	private EmbeddedElasticSearchServiceFactory embeddedElasticSearchServiceFactory;

	@Autowired
	private Gson gson;

	@Autowired
	private Step20RebuildElasticsearchIndex step20RebuildElasticsearchIndex;

	@Autowired
	private Step23RebuildElasticsearchIndex step23RebuildElasticsearchIndex;

	@Override
	public RepositoryCollection getBackend()
	{
		return postgreSqlRepositoryCollection;
	}

	@Override
	public void addUpgrades()
	{
		upgradeService.addUpgrade(step20RebuildElasticsearchIndex);
		upgradeService.addUpgrade(new Step21SetLoggingEventBackend(dataSource));
		upgradeService.addUpgrade(new Step22RemoveDiseaseMatcher(dataSource));
		upgradeService.addUpgrade(step23RebuildElasticsearchIndex);
		upgradeService.addUpgrade(new Step24UpdateApplicationSettings(dataSource, idGenerator));
		upgradeService.addUpgrade(new Step25LanguagesPermissions(dataService));
		upgradeService.addUpgrade(new Step26migrateJpaBackend(dataSource, "MySQL", idGenerator));
		upgradeService.addUpgrade(new Step27MetaDataAttributeRoles(dataSource));
		upgradeService.addUpgrade(new Step28MigrateSorta(dataSource));
	}

	// @Override
	// protected void addReposToReindex(DataServiceImpl localDataService, MySqlEntityFactory localMySqlEntityFactory)
	// {
	// // Get the undecorated repos to index
	// MysqlRepositoryCollection description = new MysqlRepositoryCollection()
	// {
	// @Override
	// protected MysqlRepository createMysqlRepository()
	// {
	// return new MysqlRepository(localDataService, localMySqlEntityFactory, dataSource,
	// new AsyncJdbcTemplate(new JdbcTemplate(dataSource)));
	// }
	//
	// @Override
	// public boolean hasRepository(String dataType)
	// {
	// throw new UnsupportedOperationException();
	// }
	// };
	//
	// // metadata repositories get created here.
	// localDataService.getMeta().setDefaultBackend(description);
	// List<EntityMetaData> metas = DependencyResolver
	// .resolve(Sets.newHashSet(localDataService.getMeta().getEntityMetaDatas()));
	//
	// for (EntityMetaData emd : metas)
	// {
	// if (!emd.isAbstract() && !localDataService.hasRepository(emd.getName()))
	// {
	// if (MysqlRepositoryCollection.NAME.equals(emd.getBackend()))
	// {
	// localDataService.addRepository(description.createRepository(emd));
	// }
	// else if (ElasticsearchRepositoryCollection.NAME.equals(emd.getBackend()))
	// {
	// localDataService.addRepository(elasticsearchRepositoryCollection.createRepository(emd));
	// }
	// else
	// {
	// LOG.warn("description [{}] unknown for meta data [{}]", emd.getBackend(), emd.getName());
	// }
	// }
	// }
	// }

	@Override
	protected void addFreemarkerVariables(Map<String, Object> freemarkerVariables)
	{
		freemarkerVariables.put("dataExplorerLink",
				new DataExplorerHyperlinkDirective(molgenisPluginRegistry(), dataService));
	}

	@Override
	public FreeMarkerConfigurer freeMarkerConfigurer() throws IOException, TemplateException
	{
		FreeMarkerConfigurer result = super.freeMarkerConfigurer();
		// Look up unknown templates in the FreemarkerTemplate repository
		result.setPostTemplateLoaders(new RepositoryTemplateLoader(dataService));
		return result;
	}
}
