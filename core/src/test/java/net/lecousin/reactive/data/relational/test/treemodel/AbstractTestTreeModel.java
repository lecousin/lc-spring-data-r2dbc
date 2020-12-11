package net.lecousin.reactive.data.relational.test.treemodel;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import net.lecousin.reactive.data.relational.repository.LcR2dbcRepositoryFactoryBean;
import net.lecousin.reactive.data.relational.test.AbstractLcReactiveDataRelationalTest;

@EnableR2dbcRepositories(repositoryFactoryBeanClass = LcR2dbcRepositoryFactoryBean.class)
public abstract class AbstractTestTreeModel extends AbstractLcReactiveDataRelationalTest {

	@Autowired
	private NodeRepository repo;
	
	@Override
	protected Collection<Class<?>> usedEntities() {
		return Arrays.asList(Node.class);
	}
	
	@Test
	public void testTwoRoots() {
		Node root1 = new Node(null, "root1");
		Node root2 = new Node(null, "root2");
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		
		List<Node> roots = repo.findAll().collectList().block();
		Assertions.assertEquals(2, roots.size());
		root1 = roots.stream().filter(root -> "root1".equals(root.getName())).findFirst().get();
		root2 = roots.stream().filter(root -> "root2".equals(root.getName())).findFirst().get();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test3levels() {
		Node root1 = new Node(null, "root1");
		Node root2 = new Node(null, "root2");
		
		root1.setChildren(new LinkedList<>());
		Node sub1_1 = new Node(root1, "sub1.1");
		Node sub1_2 = new Node(root1, "sub1.2");
		root2.setChildren(new LinkedList<>());
		Node sub2_1 = new Node(root2, "sub2.1");
		
		sub1_1.setChildren(new LinkedList<>());
		Node sub1_1_1 = new Node(sub1_1, "sub1.1.1");
		Node sub1_1_2 = new Node(sub1_1, "sub1.1.2");
		sub1_2.setChildren(new LinkedList<>());
		Node sub1_2_1 = new Node(sub1_2, "sub1.2.1");
		Node sub1_2_2 = new Node(sub1_2, "sub1.2.2");
		
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		
		List<Node> nodes = repo.findAll().collectList().block();
		Assertions.assertEquals(9, nodes.size());
		root1 = nodes.stream().filter(root -> "root1".equals(root.getName())).findFirst().get();
		root2 = nodes.stream().filter(root -> "root2".equals(root.getName())).findFirst().get();
		Assertions.assertEquals(2, root1.lazyGetChildren().collectList().block().size());
		Assertions.assertEquals(1, root2.lazyGetChildren().collectList().block().size());
		
		nodes = repo.fetch1Level().collectList().block();
		root1 = nodes.stream().filter(root -> "root1".equals(root.getName())).findFirst().get();
		root2 = nodes.stream().filter(root -> "root2".equals(root.getName())).findFirst().get();
		Assertions.assertEquals(2, root1.getChildren().size());
		Assertions.assertEquals(1, root2.getChildren().size());
		sub1_1 = root1.getChildren().stream().filter(node -> "sub1.1".equals(node.getName())).findFirst().get();
		sub1_2 = root1.getChildren().stream().filter(node -> "sub1.2".equals(node.getName())).findFirst().get();
		sub2_1 = root2.getChildren().stream().filter(node -> "sub2.1".equals(node.getName())).findFirst().get();
		
		nodes = repo.fetch2Levels().collectList().block();
		root1 = nodes.stream().filter(root -> "root1".equals(root.getName())).findFirst().get();
		root2 = nodes.stream().filter(root -> "root2".equals(root.getName())).findFirst().get();
		Assertions.assertEquals(2, root1.getChildren().size());
		Assertions.assertEquals(1, root2.getChildren().size());
		sub1_1 = root1.getChildren().stream().filter(node -> "sub1.1".equals(node.getName())).findFirst().get();
		sub1_2 = root1.getChildren().stream().filter(node -> "sub1.2".equals(node.getName())).findFirst().get();
		sub2_1 = root2.getChildren().stream().filter(node -> "sub2.1".equals(node.getName())).findFirst().get();
		Assertions.assertEquals(2, sub1_1.getChildren().size());
		Assertions.assertEquals(2, sub1_2.getChildren().size());
		Assertions.assertEquals(0, sub2_1.getChildren().size());
		sub1_1_1 = sub1_1.getChildren().stream().filter(node -> "sub1.1.1".equals(node.getName())).findFirst().get();
		sub1_1_2 = sub1_1.getChildren().stream().filter(node -> "sub1.1.2".equals(node.getName())).findFirst().get();
		sub1_2_1 = sub1_2.getChildren().stream().filter(node -> "sub1.2.1".equals(node.getName())).findFirst().get();
		sub1_2_2 = sub1_2.getChildren().stream().filter(node -> "sub1.2.2".equals(node.getName())).findFirst().get();
		
		sub1_2_1.setName("sub1.2.1.updated");
		repo.saveAll(Arrays.asList(root1, root2)).collectList().block();
		
		nodes = repo.fetch2Levels().collectList().block();
		root1 = nodes.stream().filter(root -> "root1".equals(root.getName())).findFirst().get();
		root2 = nodes.stream().filter(root -> "root2".equals(root.getName())).findFirst().get();
		Assertions.assertEquals(2, root1.getChildren().size());
		Assertions.assertEquals(1, root2.getChildren().size());
		sub1_1 = root1.getChildren().stream().filter(node -> "sub1.1".equals(node.getName())).findFirst().get();
		sub1_2 = root1.getChildren().stream().filter(node -> "sub1.2".equals(node.getName())).findFirst().get();
		sub2_1 = root2.getChildren().stream().filter(node -> "sub2.1".equals(node.getName())).findFirst().get();
		Assertions.assertEquals(2, sub1_1.getChildren().size());
		Assertions.assertEquals(2, sub1_2.getChildren().size());
		Assertions.assertEquals(0, sub2_1.getChildren().size());
		sub1_1_1 = sub1_1.getChildren().stream().filter(node -> "sub1.1.1".equals(node.getName())).findFirst().get();
		sub1_1_2 = sub1_1.getChildren().stream().filter(node -> "sub1.1.2".equals(node.getName())).findFirst().get();
		sub1_2_1 = sub1_2.getChildren().stream().filter(node -> "sub1.2.1.updated".equals(node.getName())).findFirst().get();
		sub1_2_2 = sub1_2.getChildren().stream().filter(node -> "sub1.2.2".equals(node.getName())).findFirst().get();
		
		repo.delete(root1).block();
		nodes = repo.findAll().collectList().block();
		Assertions.assertEquals(2, nodes.size());
	}
	
}
