package org.houseflys.jdbc;

import org.houseflys.jdbc.connect.PhysicalConnection;
import org.houseflys.jdbc.connect.PhysicalInfo;
import org.houseflys.jdbc.connect.PhysicalInfo.ServerInfo;
import org.houseflys.jdbc.data.Block;
import org.houseflys.jdbc.misc.Validate;
import org.houseflys.jdbc.protocol.EOFStreamResponse;
import org.houseflys.jdbc.protocol.HelloResponse;
import org.houseflys.jdbc.protocol.QueryRequest.ClientInfo;
import org.houseflys.jdbc.protocol.QueryResponse;
import org.houseflys.jdbc.protocol.RequestOrResponse;
import org.houseflys.jdbc.settings.ClickHouseConfig;
import org.houseflys.jdbc.settings.ClickHouseDefines;
import org.houseflys.jdbc.statement.ClickHouseStatement;
import org.houseflys.jdbc.statement.ClickHousePreparedInsertStatement;
import org.houseflys.jdbc.statement.ClickHousePreparedQueryStatement;
import org.houseflys.jdbc.stream.InputFormat;
import org.houseflys.jdbc.wrapper.SQLConnection;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClickHouseConnection extends SQLConnection {

    private static final Pattern VALUES_REGEX = Pattern.compile("[V|v][A|a][L|l][U|u][E|e][S|s]\\s*\\(");

    // Just to be variable
    private final AtomicBoolean isClosed;
    private final ClickHouseConfig configure;
    private final AtomicReference<PhysicalInfo> atomicInfo;

    protected ClickHouseConnection(ClickHouseConfig configure, PhysicalInfo info) {
        this.isClosed = new AtomicBoolean(false);
        this.configure = configure;
        this.atomicInfo = new AtomicReference<PhysicalInfo>(info);
    }

    @Override
    public void close() throws SQLException {
        if (!isClosed() && isClosed.compareAndSet(false, true)) {
            PhysicalConnection connection = atomicInfo.get().connection();
            connection.disPhysicalConnection();
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return isClosed.get();
    }

    @Override
    public Statement createStatement() throws SQLException {
        Validate.isTrue(!isClosed(), "Unable to create Statement, Because the connection is closed.");
        return new ClickHouseStatement(this);
    }

    @Override
    public PreparedStatement prepareStatement(String query) throws SQLException {
        Validate.isTrue(!isClosed(), "Unable to create PreparedStatement, Because the connection is closed.");
        Matcher matcher = VALUES_REGEX.matcher(query);
        return matcher.find() ? new ClickHousePreparedInsertStatement(matcher.end() - 1, query, this) :
            new ClickHousePreparedQueryStatement(this, query);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        configure.parseJDBCProperties(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        Properties properties = new Properties();
        properties.put(name, value);
        configure.parseJDBCProperties(properties);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        Validate.isTrue(!isClosed(), "Unable to create Array, Because the connection is closed.");
        return new ClickHouseArray(elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        Validate.isTrue(!isClosed(), "Unable to create Struct, Because the connection is closed.");
        return new ClickHouseStruct(typeName, attributes);
    }

    public QueryResponse sendQueryRequest(final String query) throws SQLException {
        PhysicalConnection connection = getHealthyPhysicalConnection();

        connection.sendQuery(query, atomicInfo.get().client(), configure.settings());
        List<RequestOrResponse> data = new ArrayList<RequestOrResponse>();

        while (true) {
            RequestOrResponse response = connection.receiveResponse(configure.queryTimeout());

            if (response instanceof EOFStreamResponse) {
                return new QueryResponse(data);
            }

            data.add(response);
        }
    }

    public Integer sendInsertRequest(final String insertQuery, final InputFormat input) throws SQLException {
        PhysicalConnection connection = getHealthyPhysicalConnection();

        int rows = 0;
        connection.sendQuery(insertQuery, atomicInfo.get().client(), configure.settings());
        Block header = connection.receiveSampleBlock(configure.queryTimeout());

        while (true) {
            Block block = input.next(header, 8192);

            if (block.rows() == 0) {
                connection.sendData(new Block());
                connection.receiveEndOfStream(configure.queryTimeout());
                return rows;
            }

            connection.sendData(block);
            rows += block.rows();
        }
    }

    private PhysicalConnection getHealthyPhysicalConnection() throws SQLException {
        PhysicalInfo oldInfo = atomicInfo.get();
        if (!oldInfo.connection().ping(configure.queryTimeout())) {
            PhysicalInfo newInfo = createPhysicalInfo(configure);
            PhysicalInfo closeableInfo = atomicInfo.compareAndSet(oldInfo, newInfo) ? oldInfo : newInfo;
            closeableInfo.connection().disPhysicalConnection();
        }

        return atomicInfo.get().connection();
    }

    public static ClickHouseConnection createClickHouseConnection(ClickHouseConfig configure) throws SQLException {
        return new ClickHouseConnection(configure, createPhysicalInfo(configure));
    }

    private static PhysicalInfo createPhysicalInfo(ClickHouseConfig configure) throws SQLException {
        PhysicalConnection physical = PhysicalConnection.openPhysicalConnection(configure);
        return new PhysicalInfo(clientInfo(physical, configure), serverInfo(physical, configure), physical);
    }

    private static ClientInfo clientInfo(PhysicalConnection physical, ClickHouseConfig configure) throws SQLException {
        Validate.isTrue(physical.address() instanceof InetSocketAddress);
        InetSocketAddress address = (InetSocketAddress) physical.address();
        String clientName = String.format("%s %s", ClickHouseDefines.NAME, "client");
        String initialAddress = String.format("%s:%d", address.getHostName(), address.getPort());
        return new ClientInfo(initialAddress, address.getHostName(), clientName);
    }

    private static ServerInfo serverInfo(PhysicalConnection physical, ClickHouseConfig configure) throws SQLException {
        try {
            long reversion = ClickHouseDefines.CLIENT_REVERSION;
            physical.sendHello("client", reversion, configure.database(), configure.username(), configure.password());

            HelloResponse response = physical.receiveHello(configure.queryTimeout());
            TimeZone timeZone = TimeZone.getTimeZone(response.serverTimeZone());
            return new ServerInfo(response.reversion(), timeZone, response.serverDisplayName());
        } catch (SQLException rethrows) {
            physical.disPhysicalConnection();
            throw rethrows;
        }
    }
}
