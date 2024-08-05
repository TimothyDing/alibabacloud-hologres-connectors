package com.alibaba.ververica.connectors.hologres;

import com.alibaba.hologres.client.HoloClient;
import com.alibaba.hologres.client.Put;
import com.alibaba.hologres.client.exception.HoloClientException;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;

/** JDBCTestUtils. */
public class HologresTestUtils {

    public static void checkResult(
            String[] expectedResult,
            String sql,
            String[] fields,
            String url,
            String userName,
            String password)
            throws SQLException {

        try (Connection dbConn = DriverManager.getConnection(url, userName, password);
                PreparedStatement statement = dbConn.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<String> results = new ArrayList<>();
            while (resultSet.next()) {
                List<String> result = new ArrayList<>();
                for (String field : fields) {
                    Object o = resultSet.getObject(field);
                    result.add(Objects.toString(o, "null"));
                }
                results.add(StringUtils.join(result, ","));
            }
            String[] sortedResult = results.toArray(new String[0]);
            Arrays.sort(expectedResult);
            Arrays.sort(sortedResult);
            assertArrayEquals(expectedResult, sortedResult);
        }
    }

    public static void checkResultWithTimeout(
            String[] expectedResult,
            String sql,
            String[] fields,
            String url,
            String userName,
            String password,
            long timeout)
            throws SQLException, InterruptedException {

        long endTimeout = System.currentTimeMillis() + timeout;
        boolean result = false;
        while (System.currentTimeMillis() < endTimeout) {
            try {
                checkResult(expectedResult, sql, fields, url, userName, password);
                result = true;
                break;
            } catch (AssertionError | SQLException throwable) {
                Thread.sleep(1000L);
            }
        }
        if (!result) {
            checkResult(expectedResult, sql, fields, url, userName, password);
        }
    }

    public static String[] expectedRowsToString(Object[][] expected) {
        return Arrays.stream(expected)
                .map(
                        row ->
                                Arrays.stream(row)
                                        .map(
                                                object ->
                                                        Objects.isNull(object)
                                                                ? "null"
                                                                : object.toString())
                                        .collect(Collectors.joining(",")))
                .toArray(String[]::new);
    }

    public static void insertValues(HoloClient client, String tableName, Object[][] values)
            throws HoloClientException {
        com.alibaba.hologres.client.model.TableSchema holoSchema = client.getTableSchema(tableName);
        for (Object[] value : values) {
            Put put = new Put(holoSchema);
            for (int i = 0; i < value.length; i++) {
                put.setObject(i, value[i]);
            }
            client.put(put);
        }
        client.flush();
    }
}
