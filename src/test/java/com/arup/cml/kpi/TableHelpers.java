package com.arup.cml.kpi;

import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import static org.assertj.core.api.Assertions.assertThat;

public class TableHelpers {
    public static void assertTableColumns(Table actual, Table expected) throws AssertionError {
        assertThat(actual.columns().size()).isEqualTo(expected.columns().size());
        for (Column column:actual.columns()){
            assertThat(column).usingRecursiveComparison().isEqualTo(expected.column(column.name()));
        }
    }
}
