package org.molgenis.data.postgresql;

import org.mockito.ArgumentCaptor;
import org.molgenis.data.DataService;
import org.molgenis.data.MolgenisDataException;
import org.molgenis.data.Query;
import org.molgenis.data.UnknownAttributeException;
import org.molgenis.data.meta.model.Attribute;
import org.molgenis.data.meta.model.EntityType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;
import static org.molgenis.MolgenisFieldTypes.AttributeType.*;
import static org.molgenis.data.meta.model.EntityTypeMetadata.ENTITY_TYPE_META_DATA;
import static org.molgenis.data.meta.model.EntityTypeMetadata.EXTENDS;
import static org.molgenis.data.postgresql.PostgreSqlRepositoryCollection.POSTGRESQL;
import static org.testng.Assert.assertEquals;

public class PostgreSqlRepositoryCollectionTest
{
	private PostgreSqlRepositoryCollection postgreSqlRepoCollection;
	private JdbcTemplate jdbcTemplate;
	private DataService dataService;

	@BeforeMethod
	public void setUpBeforeMethod()
	{
		PostgreSqlEntityFactory postgreSqlEntityFactory = mock(PostgreSqlEntityFactory.class);
		DataSource dataSource = mock(DataSource.class);
		jdbcTemplate = mock(JdbcTemplate.class);
		dataService = mock(DataService.class);
		PlatformTransactionManager platformTransactionManager = mock(PlatformTransactionManager.class);
		postgreSqlRepoCollection = new PostgreSqlRepositoryCollection(postgreSqlEntityFactory, dataSource, jdbcTemplate,
				dataService, platformTransactionManager);
	}

	@Test
	public void updateAttribute()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getLabel()).thenReturn("label");
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getLabel()).thenReturn("updated label");
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void updateAttributeNillableToNotNillable()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isNillable()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isNillable()).thenReturn(false);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate).execute(captor.capture());
		assertEquals(captor.getValue(), "ALTER TABLE \"entity\" ALTER COLUMN \"attr\" SET NOT NULL");
	}

	@Test
	public void updateAttributeNotNillableToNillable()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate).execute(captor.capture());
		assertEquals(captor.getValue(), "ALTER TABLE \"entity\" ALTER COLUMN \"attr\" DROP NOT NULL");
	}

	@Test(expectedExceptions = MolgenisDataException.class)
	public void updateAttributeNotNillableToNillableIdAttr()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(entityType.getIdAttribute()).thenReturn(attr);
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Test
	public void updateAttributeUniqueToNotUnique()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isUnique()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isUnique()).thenReturn(false);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate).execute(captor.capture());
		assertEquals(captor.getValue(), "ALTER TABLE \"entity\" DROP CONSTRAINT \"entity_attr_key\"");
	}

	@Test(expectedExceptions = MolgenisDataException.class)
	public void updateAttributeUniqueToNotUniqueIdAttr()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(entityType.getIdAttribute()).thenReturn(attr);
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isUnique()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isUnique()).thenReturn(false);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Test
	public void updateAttributeNotUniqueToUnique()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isUnique()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isUnique()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate).execute(captor.capture());
		assertEquals(captor.getValue(), "ALTER TABLE \"entity\" ADD CONSTRAINT \"entity_attr_key\" IS_UNIQUE (\"attr\")");
	}

	@Test
	public void updateAttributeDataTypeToDataType()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(STRING);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(INT);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate).execute(captor.capture());
		assertEquals(captor.getValue(),
				"ALTER TABLE \"entity\" ALTER COLUMN \"attr\" SET DATA TYPE integer USING \"attr\"::integer");
	}

	@Test
	public void updateAttributeSingleRefDataTypeToDataType()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(XREF);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(STRING);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(2)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList("ALTER TABLE \"entity\" DROP CONSTRAINT \"entity_attr_fkey\"",
				"ALTER TABLE \"entity\" ALTER COLUMN \"attr\" SET DATA TYPE character varying(255) USING \"attr\"::character varying(255)"));
	}

	@Test
	public void updateAttributeSingleRefDataTypeToSingleRefDataType()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(XREF);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(CATEGORICAL);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void updateAttributeMultiRefDataTypeToMultiRefDataType()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(MREF);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(CATEGORICAL_MREF);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void updateAttributeDataTypeToSingleRefDataType()
	{
		Attribute refIdAttr = when(mock(Attribute.class).getName()).thenReturn("refIdAttr").getMock();
		when(refIdAttr.getDataType()).thenReturn(STRING);
		EntityType refEntityType = when(mock(EntityType.class).getName()).thenReturn("refEntity").getMock();
		when(refEntityType.getIdAttribute()).thenReturn(refIdAttr);
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(STRING);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(XREF);
		when(updatedAttr.getRefEntity()).thenReturn(refEntityType);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(2)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList(
				"ALTER TABLE \"entity\" ALTER COLUMN \"attr\" SET DATA TYPE character varying(255) USING \"attr\"::character varying(255)",
				"ALTER TABLE \"entity\" ADD CONSTRAINT \"entity_attr_fkey\" FOREIGN KEY (\"attr\") REFERENCES \"refEntity\"(\"refIdAttr\")"));
	}

	@Test(expectedExceptions = MolgenisDataException.class)
	public void updateAttributeDataTypeToDataTypeIdAttr()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(entityType.getIdAttribute()).thenReturn(attr);
		when(attr.getDataType()).thenReturn(STRING);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(updatedAttr.getDataType()).thenReturn(INT);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Test
	public void updateAttributeWithExpressionBefore()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getExpression()).thenReturn("expression");
		when(attr.getDataType()).thenReturn(STRING);
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getExpression()).thenReturn(null);
		when(updatedAttr.getDataType()).thenReturn(STRING);
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" ADD \"attr\" character varying(255)");
	}

	@Test
	public void updateAttributeWithExpressionAfter()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(attr.getDataType()).thenReturn(STRING);
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getExpression()).thenReturn(null);
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getExpression()).thenReturn("expression");
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" DROP COLUMN \"attr\"");
	}

	@Test
	public void updateAttributeWithExpressionBeforeAfter()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getExpression()).thenReturn("expression");
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getExpression()).thenReturn("expression");
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void updateAttributeCompoundBefore()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(COMPOUND);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(STRING);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" ADD \"attr\" character varying(255) NOT NULL");
	}

	@Test
	public void updateAttributeCompoundAfter()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(STRING);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(COMPOUND);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" DROP COLUMN \"attr\"");
	}

	@Test
	public void updateAttributeCompoundBeforeAfter()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(COMPOUND);
		when(attr.isNillable()).thenReturn(false);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(COMPOUND);
		when(updatedAttr.isNillable()).thenReturn(true);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void updateAttributeAbstractEntity()
	{
		EntityType abstractEntityType = when(mock(EntityType.class).getName()).thenReturn("root").getMock();
		when(abstractEntityType.isAbstract()).thenReturn(true);
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(abstractEntityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isNillable()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isNillable()).thenReturn(false);

		EntityType entityType0 = when(mock(EntityType.class).getName()).thenReturn("entity0").getMock();
		when(entityType0.getExtends()).thenReturn(abstractEntityType);
		when(entityType0.isAbstract()).thenReturn(true);
		EntityType entityType0a = when(mock(EntityType.class).getName()).thenReturn("entity0a").getMock();
		when(entityType0a.getExtends()).thenReturn(entityType0);
		when(entityType0a.isAbstract()).thenReturn(false);
		EntityType entityType0b = when(mock(EntityType.class).getName()).thenReturn("entity0b").getMock();
		when(entityType0b.getExtends()).thenReturn(entityType0);
		when(entityType0b.isAbstract()).thenReturn(false);
		EntityType entityType1 = when(mock(EntityType.class).getName()).thenReturn("entity1").getMock();
		when(entityType1.getExtends()).thenReturn(abstractEntityType);
		when(entityType1.isAbstract()).thenReturn(false);

		//noinspection unchecked
		Query<EntityType> entityQ = mock(Query.class);
		when(dataService.query(ENTITY_TYPE_META_DATA, EntityType.class)).thenReturn(entityQ);
		//noinspection unchecked
		Query<EntityType> entityQ0 = mock(Query.class);
		//noinspection unchecked
		Query<EntityType> entityQ1 = mock(Query.class);
		when(entityQ.eq(EXTENDS, abstractEntityType)).thenReturn(entityQ0);
		when(entityQ.eq(EXTENDS, entityType0)).thenReturn(entityQ1);
		when(entityQ0.findAll()).thenReturn(Stream.of(entityType0, entityType1));
		when(entityQ1.findAll()).thenReturn(Stream.of(entityType0a, entityType0b));

		postgreSqlRepoCollection.updateAttribute(abstractEntityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(3)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList("ALTER TABLE \"entity0a\" ALTER COLUMN \"attr\" SET NOT NULL",
				"ALTER TABLE \"entity0b\" ALTER COLUMN \"attr\" SET NOT NULL",
				"ALTER TABLE \"entity1\" ALTER COLUMN \"attr\" SET NOT NULL"));
	}

	@Test(expectedExceptions = UnknownAttributeException.class)
	public void updateAttributeDoesNotExist()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.isNillable()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(updatedAttr.isNillable()).thenReturn(false);
		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Test
	public void updateAttributeRefEntityXref()
	{
		Attribute refIdAttr0 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr0").getMock();
		when(refIdAttr0.getDataType()).thenReturn(STRING);
		EntityType refEntityType0 = when(mock(EntityType.class).getName()).thenReturn("refEntity0").getMock();
		when(refEntityType0.getIdAttribute()).thenReturn(refIdAttr0);

		Attribute refIdAttr1 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr1").getMock();
		when(refIdAttr1.getDataType()).thenReturn(STRING);
		EntityType refEntityType1 = when(mock(EntityType.class).getName()).thenReturn("refEntity1").getMock();
		when(refEntityType1.getIdAttribute()).thenReturn(refIdAttr1);

		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";

		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(XREF);
		when(attr.getRefEntity()).thenReturn(refEntityType0);

		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(XREF);
		when(updatedAttr.getRefEntity()).thenReturn(refEntityType1);

		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(2)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList("ALTER TABLE \"entity\" DROP CONSTRAINT \"entity_attr_fkey\"",
				"ALTER TABLE \"entity\" ADD CONSTRAINT \"entity_attr_fkey\" FOREIGN KEY (\"attr\") REFERENCES \"refEntity1\"(\"refIdAttr1\")"));
	}

	@Test
	public void updateAttributeRefEntityXrefDifferentIdAttrType()
	{
		Attribute refIdAttr0 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr0").getMock();
		when(refIdAttr0.getDataType()).thenReturn(INT);
		EntityType refEntityType0 = when(mock(EntityType.class).getName()).thenReturn("refEntity0").getMock();
		when(refEntityType0.getIdAttribute()).thenReturn(refIdAttr0);

		Attribute refIdAttr1 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr1").getMock();
		when(refIdAttr1.getDataType()).thenReturn(STRING);
		EntityType refEntityType1 = when(mock(EntityType.class).getName()).thenReturn("refEntity1").getMock();
		when(refEntityType1.getIdAttribute()).thenReturn(refIdAttr1);

		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";

		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(XREF);
		when(attr.getRefEntity()).thenReturn(refEntityType0);

		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(XREF);
		when(updatedAttr.getRefEntity()).thenReturn(refEntityType1);

		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(3)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList("ALTER TABLE \"entity\" DROP CONSTRAINT \"entity_attr_fkey\"",
				"ALTER TABLE \"entity\" ALTER COLUMN \"attr\" SET DATA TYPE character varying(255) USING \"attr\"::character varying(255)",
				"ALTER TABLE \"entity\" ADD CONSTRAINT \"entity_attr_fkey\" FOREIGN KEY (\"attr\") REFERENCES \"refEntity1\"(\"refIdAttr1\")"));
	}

	@Test(expectedExceptions = MolgenisDataException.class, expectedExceptionsMessageRegExp = "Updating entity \\[entity\\] attribute \\[attr\\] referenced entity from \\[refEntity0\\] to \\[refEntity1\\] not allowed for type \\[MREF\\]")
	public void updateAttributeRefEntityMref()
	{
		Attribute refIdAttr0 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr0").getMock();
		when(refIdAttr0.getDataType()).thenReturn(STRING);
		EntityType refEntityType0 = when(mock(EntityType.class).getName()).thenReturn("refEntity0").getMock();
		when(refEntityType0.getIdAttribute()).thenReturn(refIdAttr0);

		Attribute refIdAttr1 = when(mock(Attribute.class).getName()).thenReturn("refIdAttr1").getMock();
		when(refIdAttr1.getDataType()).thenReturn(STRING);
		EntityType refEntityType1 = when(mock(EntityType.class).getName()).thenReturn("refEntity1").getMock();
		when(refEntityType1.getIdAttribute()).thenReturn(refIdAttr1);

		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(idAttr.getDataType()).thenReturn(STRING);
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);

		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.getDataType()).thenReturn(MREF);
		when(attr.getRefEntity()).thenReturn(refEntityType0);

		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.getDataType()).thenReturn(MREF);
		when(updatedAttr.getRefEntity()).thenReturn(refEntityType1);

		postgreSqlRepoCollection.updateAttribute(entityType, attr, updatedAttr);
	}

	@Test
	public void addAttribute()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.getDataType()).thenReturn(STRING);
		postgreSqlRepoCollection.addAttribute(entityType, attr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" ADD \"attr\" character varying(255) NOT NULL");
	}

	@Test
	public void addAttributeUnique()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.getDataType()).thenReturn(STRING);
		when(attr.isUnique()).thenReturn(true);
		postgreSqlRepoCollection.addAttribute(entityType, attr);
		verify(jdbcTemplate).execute(
				"ALTER TABLE \"entity\" ADD \"attr\" character varying(255) NOT NULL,ADD CONSTRAINT \"entity_attr_key\" UNIQUE (\"attr\")");
	}

	@Test
	public void addAttributeAbstractEntity()
	{
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		EntityType abstractEntityType = when(mock(EntityType.class).getName()).thenReturn("root").getMock();
		when(abstractEntityType.isAbstract()).thenReturn(true);
		EntityType entityType0 = when(mock(EntityType.class).getName()).thenReturn("entity0").getMock();
		when(entityType0.getExtends()).thenReturn(abstractEntityType);
		when(entityType0.isAbstract()).thenReturn(true);
		EntityType entityType0a = when(mock(EntityType.class).getName()).thenReturn("entity0a").getMock();
		when(entityType0a.getExtends()).thenReturn(entityType0);
		when(entityType0a.isAbstract()).thenReturn(false);
		when(entityType0a.getIdAttribute()).thenReturn(idAttr);
		EntityType entityType0b = when(mock(EntityType.class).getName()).thenReturn("entity0b").getMock();
		when(entityType0b.getExtends()).thenReturn(entityType0);
		when(entityType0b.isAbstract()).thenReturn(false);
		when(entityType0b.getIdAttribute()).thenReturn(idAttr);
		EntityType entityType1 = when(mock(EntityType.class).getName()).thenReturn("entity1").getMock();
		when(entityType1.getExtends()).thenReturn(abstractEntityType);
		when(entityType1.isAbstract()).thenReturn(false);
		when(entityType1.getIdAttribute()).thenReturn(idAttr);

		//noinspection unchecked
		Query<EntityType> entityQ = mock(Query.class);
		when(dataService.query(ENTITY_TYPE_META_DATA, EntityType.class)).thenReturn(entityQ);
		//noinspection unchecked
		Query<EntityType> entityQ0 = mock(Query.class);
		//noinspection unchecked
		Query<EntityType> entityQ1 = mock(Query.class);
		when(entityQ.eq(EXTENDS, abstractEntityType)).thenReturn(entityQ0);
		when(entityQ.eq(EXTENDS, entityType0)).thenReturn(entityQ1);
		when(entityQ0.findAll()).thenReturn(Stream.of(entityType0, entityType1));
		when(entityQ1.findAll()).thenReturn(Stream.of(entityType0a, entityType0b));

		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.getDataType()).thenReturn(STRING);

		postgreSqlRepoCollection.addAttribute(abstractEntityType, attr);
		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(3)).execute(captor.capture());
		assertEquals(captor.getAllValues(),
				newArrayList("ALTER TABLE \"entity0a\" ADD \"attr\" character varying(255) NOT NULL",
						"ALTER TABLE \"entity0b\" ADD \"attr\" character varying(255) NOT NULL",
						"ALTER TABLE \"entity1\" ADD \"attr\" character varying(255) NOT NULL"));
	}

	@Test
	public void addAttributeCompound()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.getDataType()).thenReturn(COMPOUND);
		postgreSqlRepoCollection.addAttribute(entityType, attr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void addAttributeWithExpression()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		when(entityType.getIdAttribute()).thenReturn(idAttr);
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		when(attr.getExpression()).thenReturn("expression");
		when(attr.getDataType()).thenReturn(STRING);
		postgreSqlRepoCollection.addAttribute(entityType, attr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test
	public void addAttributeOneToManyMappedBy()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		when(entityType.getBackend()).thenReturn(POSTGRESQL);
		EntityType refEntityType = when(mock(EntityType.class).getName()).thenReturn("refEntity").getMock();
		when(refEntityType.getBackend()).thenReturn(POSTGRESQL);
		Attribute idAttr = when(mock(Attribute.class).getName()).thenReturn("id").getMock();
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		Attribute refAttr = when(mock(Attribute.class).getName()).thenReturn("refAttr").getMock();

		when(refAttr.getDataType()).thenReturn(XREF);
		when(refAttr.isInversedBy()).thenReturn(true);
		when(refAttr.getInversedBy()).thenReturn(attr);

		when(attr.getDataType()).thenReturn(ONE_TO_MANY);
		when(attr.getRefEntity()).thenReturn(refEntityType);
		when(attr.getMappedBy()).thenReturn(refAttr);
		when(attr.isMappedBy()).thenReturn(true);

		when(entityType.getIdAttribute()).thenReturn(idAttr);

		postgreSqlRepoCollection.addAttribute(entityType, attr);
		verify(jdbcTemplate).execute("ALTER TABLE \"refEntity\" ADD \"refAttr_order\" SERIAL");
	}

	@Test(expectedExceptions = MolgenisDataException.class)
	public void addAttributeAlreadyExists()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		postgreSqlRepoCollection.addAttribute(entityType, attr);
	}

	@Test
	public void deleteAttribute()
	{
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(attr.getDataType()).thenReturn(STRING);
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		postgreSqlRepoCollection.deleteAttribute(entityType, attr);
		verify(jdbcTemplate).execute("ALTER TABLE \"entity\" DROP COLUMN \"attr\"");
	}

	@Test
	public void deleteAttributeMref()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		EntityType refEntityMeta = when(mock(EntityType.class).getName()).thenReturn("refEntity").getMock();

		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();

		when(attr.getDataType()).thenReturn(MREF);
		when(attr.getRefEntity()).thenReturn(refEntityMeta);

		when(entityType.getAttribute(attrName)).thenReturn(attr);
		postgreSqlRepoCollection.deleteAttribute(entityType, attr);
		verify(jdbcTemplate).execute("DROP TABLE \"entity_attr\"");
	}

	@Test
	public void deleteAttributeOneToManyMappedBy()
	{
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		EntityType refEntityMeta = when(mock(EntityType.class).getName()).thenReturn("refEntity").getMock();

		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		String refAttrName = "refAttr";
		Attribute refAttr = when(mock(Attribute.class).getName()).thenReturn(refAttrName).getMock();

		when(attr.getDataType()).thenReturn(ONE_TO_MANY);
		when(attr.getRefEntity()).thenReturn(refEntityMeta);
		when(attr.getMappedBy()).thenReturn(refAttr);
		when(attr.isMappedBy()).thenReturn(true);

		when(refAttr.getDataType()).thenReturn(XREF);
		when(refAttr.getRefEntity()).thenReturn(entityType);
		when(refAttr.getInversedBy()).thenReturn(attr);
		when(refAttr.isInversedBy()).thenReturn(true);

		when(entityType.getAttribute(attrName)).thenReturn(attr);
		postgreSqlRepoCollection.deleteAttribute(entityType, attr);
		verify(jdbcTemplate).execute("ALTER TABLE \"refEntity\" DROP COLUMN \"refAttr_order\"");
	}

	@Test
	public void deleteAttributeAbstractEntity()
	{
		EntityType abstractEntityType = when(mock(EntityType.class).getName()).thenReturn("root").getMock();
		when(abstractEntityType.isAbstract()).thenReturn(true);
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(attr.getDataType()).thenReturn(STRING);
		when(abstractEntityType.getAttribute(attrName)).thenReturn(attr);
		when(attr.isNillable()).thenReturn(true);
		Attribute updatedAttr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(updatedAttr.isNillable()).thenReturn(false);
		EntityType entityType0 = when(mock(EntityType.class).getName()).thenReturn("entity0").getMock();
		when(entityType0.getExtends()).thenReturn(abstractEntityType);
		when(entityType0.isAbstract()).thenReturn(true);
		EntityType entityType0a = when(mock(EntityType.class).getName()).thenReturn("entity0a").getMock();
		when(entityType0a.getExtends()).thenReturn(entityType0);
		when(entityType0a.isAbstract()).thenReturn(false);
		EntityType entityType0b = when(mock(EntityType.class).getName()).thenReturn("entity0b").getMock();
		when(entityType0b.getExtends()).thenReturn(entityType0);
		when(entityType0b.isAbstract()).thenReturn(false);
		EntityType entityType1 = when(mock(EntityType.class).getName()).thenReturn("entity1").getMock();
		when(entityType1.getExtends()).thenReturn(abstractEntityType);
		when(entityType1.isAbstract()).thenReturn(false);

		//noinspection unchecked
		Query<EntityType> entityQ = mock(Query.class);
		when(dataService.query(ENTITY_TYPE_META_DATA, EntityType.class)).thenReturn(entityQ);
		//noinspection unchecked
		Query<EntityType> entityQ0 = mock(Query.class);
		//noinspection unchecked
		Query<EntityType> entityQ1 = mock(Query.class);
		when(entityQ.eq(EXTENDS, abstractEntityType)).thenReturn(entityQ0);
		when(entityQ.eq(EXTENDS, entityType0)).thenReturn(entityQ1);
		when(entityQ0.findAll()).thenReturn(Stream.of(entityType0, entityType1));
		when(entityQ1.findAll()).thenReturn(Stream.of(entityType0a, entityType0b));

		postgreSqlRepoCollection.deleteAttribute(abstractEntityType, attr);

		ArgumentCaptor<String> captor = forClass(String.class);
		verify(jdbcTemplate, times(3)).execute(captor.capture());
		assertEquals(captor.getAllValues(), newArrayList("ALTER TABLE \"entity0a\" DROP COLUMN \"attr\"",
				"ALTER TABLE \"entity0b\" DROP COLUMN \"attr\"", "ALTER TABLE \"entity1\" DROP COLUMN \"attr\""));
	}

	@Test
	public void deleteAttributeWithExpression()
	{
		String attrName = "attr";
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn(attrName).getMock();
		when(attr.getExpression()).thenReturn("expression");
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		when(entityType.getAttribute(attrName)).thenReturn(attr);
		postgreSqlRepoCollection.deleteAttribute(entityType, attr);
		verifyZeroInteractions(jdbcTemplate);
	}

	@Test(expectedExceptions = UnknownAttributeException.class)
	public void deleteAttributeUnknownAttribute()
	{
		Attribute attr = when(mock(Attribute.class).getName()).thenReturn("attr").getMock();
		EntityType entityType = when(mock(EntityType.class).getName()).thenReturn("entity").getMock();
		postgreSqlRepoCollection.deleteAttribute(entityType, attr);
	}
}