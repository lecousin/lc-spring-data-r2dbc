package net.lecousin.reactive.data.relational.postgres.test;

import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.test.treemodel.AbstractTestTreeModel;

@ContextConfiguration(classes = { PostgresTestConfiguration.class })
public class TestTree extends AbstractTestTreeModel {

}
