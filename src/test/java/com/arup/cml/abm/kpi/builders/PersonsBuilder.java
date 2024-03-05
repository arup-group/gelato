package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;
import java.util.Set;

public class PersonsBuilder {

    TemporaryFolder tmpDir;
    Table persons = Table.create("persons").addColumns(
            StringColumn.create("person"),
            DoubleColumn.create("income"),
            StringColumn.create("subpopulation")
    );

    public PersonsBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
    }

    private void fillWithDudValues() {
        Set<String> numberCols = Set.of("income", "monetaryDistanceRate", "dailyMonetaryConstant");
        for (Column col : persons.columns()) {
            if (numberCols.contains(col.name())) {
                col.append(1.0);
            } else {
                col.append("dud");
            }
        }
    }

    public String build() {
        if (persons.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            fillWithDudValues();
        }
        String path = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_persons.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(path).separator(';').build();
        persons.write().usingOptions(options);
        return path;
    }
}