package net.lecousin.reactive.data.relational;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import javassist.CtClass;
import net.lecousin.reactive.data.relational.enhance.Enhancer;
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
import net.lecousin.reactive.data.relational.model.metadata.EntityStaticMetadata;

public class LcReactiveDataRelationalRestarter implements ApplicationListener<ApplicationEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(LcReactiveDataRelationalRestarter.class);
	
	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE - 1; // just after Restarter
	}
	
	@Override
	@SuppressWarnings({"squid:S3011", "squid:S1872", "squid:S3776"})
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl.getClass().getSimpleName().equals("RestartClassLoader")) {
				logger.info("Restarter class loader detected: restart enhancing process");
				List<String> names = new LinkedList<>();
				for (Class<?> entity : EntityStaticMetadata.getClasses()) {
					names.add(entity.getName());
				}
				try {
					Enhancer.enhance(names, classes -> {
						try {
							Field f = cl.getClass().getDeclaredField("updatedFiles");
							f.setAccessible(true);
							ClassLoaderFiles files = (ClassLoaderFiles)f.get(cl);
							for (CtClass ct : classes) {
								files.addFile(ct.getName().replace('.', '/').concat(".class"), new ClassLoaderFile(Kind.MODIFIED, ct.toBytecode()));
							}
							List<Class<?>> reloaded = new LinkedList<>();
							for (CtClass ct : classes) {
								reloaded.add(cl.loadClass(ct.getName()));
							}
							return reloaded;
						} catch (Exception e) {
							throw new ModelAccessException("Error reloading entities", e);
						}
					});
				} catch (ModelException e) {
					throw new ModelAccessException("Error reloading entities", e);
				}
			}
		}
	}

}
