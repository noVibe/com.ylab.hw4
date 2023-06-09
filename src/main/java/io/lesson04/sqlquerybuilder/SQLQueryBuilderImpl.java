package io.lesson04.sqlquerybuilder;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SQLQueryBuilderImpl implements SQLQueryBuilder {
    private final DataSource dataSource;

    public SQLQueryBuilderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String queryForTable(String tableName) throws SQLException {
        if (getTables(tableName).size() < 1) {
            return null;
        }
        StringBuilder str = new StringBuilder("SELECT ");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet result = metaData.getColumns(null, null, tableName, null);
            while (result.next()) {
                str.append(result.getString("COLUMN_NAME")).append(", ");
            }
            str.append("FROM ").append(tableName);
            str.deleteCharAt(str.lastIndexOf(","));
            return str.toString();
        } catch (StringIndexOutOfBoundsException e) {
            return "<Table with no columns>";
        }
    }

    @Override

    public List<String> getTables(String tableName) throws SQLException {
        List<String> resultList;
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE", "SYSTEM VIEW"}); //new String[]{"TABLE", "SYSTEM_TABLE"}
            resultList = new ArrayList<>();
            while (resultSet.next()) {
                resultList.add(resultSet.getString("TABLE_NAME"));
            }
        }
        return resultList;
    }
}

