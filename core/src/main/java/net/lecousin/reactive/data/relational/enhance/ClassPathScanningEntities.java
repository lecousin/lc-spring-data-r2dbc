/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.lecousin.reactive.data.relational.enhance;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.ClassUtils;

/**
 * Search for entity classes from the classpath.
 * 
 * @author Guillaume Le Cousin
 *
 */
public class ClassPathScanningEntities {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathScanningEntities.class);
	
	private ClassPathScanningEntities() {
		// no instance
	}
	
	public static Set<String> scan() throws IOException {
		long startTime = System.currentTimeMillis();
		PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
		Resource[] classResources = resourceResolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "**/*.class");
		SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String annotationName = Table.class.getName();
		Set<String> classes = new HashSet<>();
		for (Resource classResource : classResources) {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classResource);
				AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
				if (annotationMetadata.hasAnnotation(annotationName)) {
					classes.add(annotationMetadata.getClassName());
				}
			} catch (@SuppressWarnings("java:S1181") Throwable t) {
				// ignore
			}
		}
		LOGGER.info("{} entity classes found in {} ms.", classes.size(), System.currentTimeMillis() - startTime);
		return classes;
	}
	
	public static Class<?> searchNonEntityClass(String packageName) throws IOException {
		PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
		Resource[] classResources = resourceResolver.getResources(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(packageName) +  "/*.class");
		SimpleMetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		String annotationName = Table.class.getName();
		for (Resource classResource : classResources) {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(classResource);
				AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
				if (!annotationMetadata.hasAnnotation(annotationName)) {
					return ClassPathScanningEntities.class.getClassLoader().loadClass(annotationMetadata.getClassName());
				}
			} catch (@SuppressWarnings("java:S1181") Throwable t) {
				// ignore
			}
		}
		return null;
	}

}
