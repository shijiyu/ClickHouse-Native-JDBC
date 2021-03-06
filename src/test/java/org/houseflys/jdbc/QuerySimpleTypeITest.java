package org.houseflys.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Test;

public class QuerySimpleTypeITest extends AbstractITest {

    @Test
    public void successfullyByteColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement
                    .executeQuery("SELECT toInt8(" + Byte.MIN_VALUE + "), toUInt8(" + Byte.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getByte(1), Byte.MIN_VALUE);
                Assert.assertEquals(rs.getByte(2), Byte.MAX_VALUE);
            }
        });
    }


    @Test
    public void successfullyShortColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement
                    .executeQuery("SELECT toInt16(" + Short.MIN_VALUE + "), toUInt16(" + Short.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getShort(1), Short.MIN_VALUE);
                Assert.assertEquals(rs.getShort(2), Short.MAX_VALUE);
            }
        });
    }

    @Test
    public void successfullyIntColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement
                    .executeQuery("SELECT toInt32(" + Integer.MIN_VALUE + "), toUInt32(" + Integer.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getInt(1), Integer.MIN_VALUE);
                Assert.assertEquals(rs.getInt(2), Integer.MAX_VALUE);
            }
        });
    }

    @Test
    public void successfullyLongColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement
                    .executeQuery("SELECT toInt64(" + Long.MIN_VALUE + "), toUInt64(" + Long.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getLong(1), Long.MIN_VALUE);
                Assert.assertEquals(rs.getLong(2), Long.MAX_VALUE);
            }
        });
    }

    @Test
    public void successfullyFloatColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement
                    .executeQuery("SELECT toFloat32(" + Float.MIN_VALUE + "), toFloat32(" + Float.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getFloat(1), Float.MIN_VALUE, 0.000000000001);
                Assert.assertEquals(rs.getFloat(2), Float.MAX_VALUE, 0.000000000001);
            }
        });
    }


    @Test
    public void successfullyDoubleColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement.executeQuery("SELECT toFloat64(4.9E-32), toFloat64(" + Double.MAX_VALUE + ")");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getDouble(1), Double.MIN_VALUE, 0.000000000001);
                Assert.assertEquals(rs.getDouble(2), Double.MAX_VALUE, 0.000000000001);
            }
        });
    }

    @Test
    public void successfullyUUIDColumn() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connect) throws Exception {
                Statement statement = connect.createStatement();
                ResultSet rs = statement.executeQuery("SELECT materialize('01234567-89ab-cdef-0123-456789abcdef')");

                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getString(1), "01234567-89ab-cdef-0123-456789abcdef");
            }
        });
    }
}
