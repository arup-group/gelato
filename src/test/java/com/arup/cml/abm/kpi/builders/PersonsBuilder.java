package com.arup.cml.abm.kpi.builders;

import org.junit.rules.TemporaryFolder;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.nio.file.Path;

public class PersonsBuilder {

    TemporaryFolder tmpDir;
    String defaultPerson1 = "Bobby";
    String defaultPerson2 = "Bobbina";
    double defaultIncome = 10000.0;
    String defaultSubpopulation = "default";
    Table persons = Table.create("persons").addColumns(
            StringColumn.create("person"),
            DoubleColumn.create("income"),
            StringColumn.create("subpopulation")
    );

    public PersonsBuilder(TemporaryFolder tmpDir) {
        this.tmpDir = tmpDir;
    }

    public PersonsBuilder withPerson(
            String person, double income, String subpopulation
    ) {
        persons.stringColumn("person").append(person);
        persons.doubleColumn("income").append(income);
        persons.stringColumn("subpopulation").append(subpopulation);
        return this;
    }

    public PersonsBuilder withPerson(
            String person
    ) {
        return this.withPerson(person, defaultIncome, defaultSubpopulation);
    }

    public PersonsBuilder withDefaultPersons() {
        this.withPerson(
                defaultPerson1, defaultIncome, defaultSubpopulation
        );
        return this.withPerson(
                defaultPerson2, defaultIncome, defaultSubpopulation
        );
    }

    public PersonsBuilder withPersonWithMissingIncome(String person, String subpopulation) {
        persons.stringColumn("person").append(person);
        persons.doubleColumn("income").appendMissing();
        persons.stringColumn("subpopulation").append(subpopulation);
        return this;
    }

    public String build() {
        if (persons.isEmpty()) {
            // empty table gets into trouble reading all the columns, if the table is empty, it is assumed it's not
            // being used, so filling it with dud vales, just for the shape is ok
            this.withDefaultPersons();
        }
        String path = String.valueOf(Path.of(tmpDir.getRoot().getAbsolutePath(), "output_persons.csv"));
        CsvWriteOptions options = CsvWriteOptions.builder(path).separator(';').build();
        persons.write().usingOptions(options);
        return path;
    }
}