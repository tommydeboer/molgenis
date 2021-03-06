package org.molgenis.data.migrate.version;

import static org.dbunit.database.DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS;
import static org.dbunit.database.DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES;
import static org.dbunit.database.DatabaseConfig.PROPERTY_DATATYPE_FACTORY;
import static org.dbunit.database.DatabaseConfig.PROPERTY_ESCAPE_PATTERN;

import java.sql.SQLException;
import org.dbunit.assertion.DbUnitAssert;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.xml.sax.InputSource;

class Step47AddMaxLengthIT {

  private PGSimpleDataSource dataSource;
  private DbUnitAssert assertions;

  @BeforeEach
  void setup() throws Exception {
    dataSource = new PGSimpleDataSource();
    dataSource.setURL(System.getProperty("db_uri", "jdbc:postgresql://localhost/molgenis"));
    dataSource.setUser(System.getProperty("db_user", "molgenis"));
    dataSource.setPassword(System.getProperty("db_password", "molgenis"));

    var in = getClass().getResourceAsStream("attributes.xml");
    var dataSet = new CachedDataSet(new XmlProducer(new InputSource(in)), true);

    DatabaseDataSourceConnection conn = getConnection();
    DatabaseOperation.CLEAN_INSERT.execute(conn, dataSet);
    assertions = new DbUnitAssert();
  }

  private DatabaseDataSourceConnection getConnection() throws SQLException {
    var conn = new DatabaseDataSourceConnection(dataSource, "public");
    final var config = conn.getConfig();
    config.setProperty(FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
    config.setProperty(PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
    config.setProperty(PROPERTY_ESCAPE_PATTERN, "\"?\"");
    config.setProperty(FEATURE_ALLOW_EMPTY_FIELDS, true);
    return conn;
  }

  @Test
  void testUpgrade() throws Exception {
    var step47 = new Step47AddMaxLength(dataSource);
    Assertions.assertDoesNotThrow(step47::upgrade);

    var actual =
        getConnection()
            .createQueryTable(
                "Attribute",
                "select * from \"sys_md_Attribute#c8d9a252\" where name = 'maxLength'");

    var in = getClass().getResourceAsStream("step47-done.xml");
    var expected =
        new CachedDataSet(new XmlProducer(new InputSource(in)), true).getTable("Attribute");

    assertions.assertEquals(expected, actual);
  }
}
