package net.lecousin.reactive.data.relational.h2.test;

import org.springframework.test.context.ContextConfiguration;

import net.lecousin.reactive.data.relational.test.treemodel.AbstractTestTreeModel;

@ContextConfiguration(classes = { H2TestConfiguration.class })
public class TestTree extends AbstractTestTreeModel {

}
