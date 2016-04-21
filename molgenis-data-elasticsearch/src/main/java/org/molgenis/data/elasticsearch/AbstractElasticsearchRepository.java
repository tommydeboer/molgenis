package org.molgenis.data.elasticsearch;

import static java.util.Objects.requireNonNull;
import static org.molgenis.data.RepositoryCapability.AGGREGATEABLE;
import static org.molgenis.data.RepositoryCapability.INDEXABLE;
import static org.molgenis.data.RepositoryCapability.MANAGABLE;
import static org.molgenis.data.RepositoryCapability.QUERYABLE;
import static org.molgenis.data.RepositoryCapability.WRITABLE;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.elasticsearch.common.primitives.Ints;
import org.molgenis.data.AggregateQuery;
import org.molgenis.data.AggregateResult;
import org.molgenis.data.Entity;
import org.molgenis.data.EntityListener;
import org.molgenis.data.EntityMetaData;
import org.molgenis.data.Fetch;
import org.molgenis.data.Query;
import org.molgenis.data.Repository;
import org.molgenis.data.RepositoryCapability;
import org.molgenis.data.elasticsearch.ElasticsearchService.IndexingMode;
import org.molgenis.data.elasticsearch.util.ElasticsearchEntityUtils;
import org.molgenis.data.support.QueryImpl;

import com.google.common.collect.Sets;

public abstract class AbstractElasticsearchRepository implements Repository<Entity>
{
	protected final SearchService elasticSearchService;

	public AbstractElasticsearchRepository(SearchService elasticSearchService)
	{
		this.elasticSearchService = requireNonNull(elasticSearchService, "elasticSearchService is null");
	}

	@Override
	public Set<RepositoryCapability> getCapabilities()
	{
		return Sets.newHashSet(AGGREGATEABLE, QUERYABLE, WRITABLE, INDEXABLE, MANAGABLE);
	}

	@Override
	public abstract EntityMetaData getEntityMetaData();

	@Override
	public long count()
	{
		return elasticSearchService.count(getEntityMetaData());
	}

	@Override
	public Query<Entity> query()
	{
		return new QueryImpl<Entity>(this);
	}

	@Override
	public long count(Query<Entity> q)
	{
		return elasticSearchService.count(q, getEntityMetaData());
	}

	@Override
	public Stream<Entity> findAll(Query<Entity> q)
	{
		return elasticSearchService.searchAsStream(q, getEntityMetaData());
	}

	@Override
	public Entity findOne(Query<Entity> q)
	{
		Iterable<Entity> entities = elasticSearchService.search(q, getEntityMetaData());
		Iterator<Entity> it = entities.iterator();
		return it.hasNext() ? it.next() : null;
	}

	@Override
	public Entity findOneById(Object id)
	{
		return elasticSearchService.get(id, getEntityMetaData());
	}

	@Override
	public Entity findOneById(Object id, Fetch fetch)
	{
		return elasticSearchService.get(id, getEntityMetaData(), fetch);
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids)
	{
		return elasticSearchService.get(ids, getEntityMetaData());
	}

	@Override
	public Stream<Entity> findAll(Stream<Object> ids, Fetch fetch)
	{
		return elasticSearchService.get(ids, getEntityMetaData(), fetch);
	}

	@Override
	public Iterator<Entity> iterator()
	{
		Query<Entity> q = new QueryImpl<>();
		return elasticSearchService.searchAsStream(q, getEntityMetaData()).iterator();
	}

	@Override
	public Stream<Entity> stream(Fetch fetch)
	{
		Query<Entity> q = new QueryImpl<Entity>().fetch(fetch);
		return elasticSearchService.searchAsStream(q, getEntityMetaData());
	}

	@Override
	public void close() throws IOException
	{
		// noop
	}

	@Override
	public String getName()
	{
		return getEntityMetaData().getName();
	}

	@Override
	public AggregateResult aggregate(AggregateQuery aggregateQuery)
	{
		return elasticSearchService.aggregate(aggregateQuery, getEntityMetaData());
	}

	@Override
	public void add(Entity entity)
	{
		elasticSearchService.index(entity, getEntityMetaData(), IndexingMode.ADD);
		elasticSearchService.refresh();
	}

	@Override
	public Integer add(Stream<Entity> entities)
	{
		long nrIndexedEntities = elasticSearchService.index(entities, getEntityMetaData(), IndexingMode.ADD);
		elasticSearchService.refresh();
		return Ints.checkedCast(nrIndexedEntities);
	}

	@Override
	public void flush()
	{
		elasticSearchService.flush();
	}

	@Override
	public void clearCache()
	{
		// noop
	}

	@Override
	public void update(Entity entity)
	{
		elasticSearchService.index(entity, getEntityMetaData(), IndexingMode.UPDATE);
		elasticSearchService.refresh();
	}

	@Override
	public void update(Stream<Entity> entities)
	{
		elasticSearchService.index(entities, getEntityMetaData(), IndexingMode.UPDATE);
		elasticSearchService.refresh();
	}

	@Override
	public void delete(Entity entity)
	{
		elasticSearchService.delete(entity, getEntityMetaData());
		elasticSearchService.refresh();
	}

	@Override
	public void delete(Stream<Entity> entities)
	{
		elasticSearchService.delete(entities, getEntityMetaData());
		elasticSearchService.refresh();
	}

	@Override
	public void deleteById(Object id)
	{
		elasticSearchService.deleteById(ElasticsearchEntityUtils.toElasticsearchId(id), getEntityMetaData());
		elasticSearchService.refresh();
	}

	@Override
	public void deleteAll(Stream<Object> ids)
	{
		elasticSearchService.deleteById(ElasticsearchEntityUtils.toElasticsearchIds(ids), getEntityMetaData());
		elasticSearchService.refresh();
	}

	@Override
	public void deleteAll()
	{
		elasticSearchService.delete(getEntityMetaData().getName());
		createMappings();
		elasticSearchService.refresh();
	}

	@Override
	public void create()
	{
		createMappings();
	}

	@Override
	public void drop()
	{
		elasticSearchService.delete(getEntityMetaData().getName());
	}

	@Override
	public void addEntityListener(EntityListener entityListener)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeEntityListener(EntityListener entityListener)
	{
		throw new UnsupportedOperationException();
	}

	private void createMappings()
	{
		elasticSearchService.createMappings(getEntityMetaData());
	}
}
