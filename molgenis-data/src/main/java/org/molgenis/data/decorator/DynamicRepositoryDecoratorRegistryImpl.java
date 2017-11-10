package org.molgenis.data.decorator;

import org.molgenis.data.DataService;
import org.molgenis.data.Entity;
import org.molgenis.data.Repository;
import org.molgenis.data.decorator.meta.DecoratorConfiguration;
import org.molgenis.data.decorator.meta.DecoratorConfigurationMetadata;
import org.molgenis.data.decorator.meta.DynamicDecorator;
import org.molgenis.data.support.QueryImpl;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.molgenis.data.decorator.meta.DecoratorConfigurationMetadata.DECORATOR_CONFIGURATION;

@Component
public class DynamicRepositoryDecoratorRegistryImpl implements DynamicRepositoryDecoratorRegistry
{
	private final Map<String, DynamicRepositoryDecoratorFactory> factories = new HashMap<>();
	private final DataService dataService;

	public DynamicRepositoryDecoratorRegistryImpl(DataService dataService)
	{
		this.dataService = requireNonNull(dataService);
	}

	@Override
	public synchronized void addFactory(DynamicRepositoryDecoratorFactory factory)
	{
		String factoryId = factory.getName();
		factories.put(factoryId, factory);
	}

	@Override
	public synchronized Repository<Entity> decorate(Repository<Entity> repository)
	{
		String entityTypeId = repository.getEntityType().getId();

		if (!entityTypeId.equals(DECORATOR_CONFIGURATION))
		{
			DecoratorConfiguration configuration = dataService.findOne(DECORATOR_CONFIGURATION,
					new QueryImpl<DecoratorConfiguration>().eq(DecoratorConfigurationMetadata.ENTITY_TYPE_ID,
							entityTypeId), DecoratorConfiguration.class);

			if (configuration != null)
			{
				repository = decorateRepository(repository, configuration);
			}
		}

		return repository;
	}

	@SuppressWarnings("unchecked")
	private Repository<Entity> decorateRepository(Repository<Entity> repository, DecoratorConfiguration configuration)
	{
		List<DynamicDecorator> decorators = configuration.getDecorators().collect(toList());
		if (decorators.isEmpty())
		{
			return repository;
		}

		for (DynamicDecorator decorator : decorators)
		{
			DynamicRepositoryDecoratorFactory factory = factories.get(decorator.getId());
			if (factory != null)
			{
				repository = factory.createDecoratedRepository(repository);
			}
		}
		return repository;
	}
}