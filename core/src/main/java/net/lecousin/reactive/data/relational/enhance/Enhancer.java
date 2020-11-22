package net.lecousin.reactive.data.relational.enhance;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import reactor.core.publisher.Mono;

@SuppressWarnings({"squid:S3011"})
public final class Enhancer {
	
	private static final Log logger = LogFactory.getLog(Enhancer.class);
	
	static Map<Class<?>, Field> entities = new HashMap<>();
	
	static final String STATE_FIELD_NAME = "_lcState";
	
	private Enhancer() {
		// no instance
	}
	
	public static Collection<Class<?>> getEntities() {
		return entities.keySet();
	}

	public static void enhance(String... entityClasses) {
		enhance(Arrays.asList(entityClasses));
	}
	
	public static void enhance(Collection<String> entityClasses) {
		ClassPool classPool = ClassPool.getDefault();
		
		for (String className : entityClasses) {
			enhanceClass(classPool, className);
		}
		for (Class<?> newClass : entities.keySet()) {
			try {
				Field fieldInfo = newClass.getDeclaredField(STATE_FIELD_NAME);
				fieldInfo.setAccessible(true);
				entities.put(newClass, fieldInfo);
			} catch (Exception e) {
				throw new RuntimeException("Error accessing to enhanced class " + newClass.getName(), e);
			}
		}
	}
	
	private static void enhanceClass(ClassPool classPool, String className) {
		logger.info("Enhancing entity " + className);
		try {
			CtClass cl = classPool.get(className);
			if (cl.getAttribute(STATE_FIELD_NAME) != null) {
				logger.warn("Entity already enhanced: " + className);
				return;
			}
			cl.defrost();

	        addStateAttribute(classPool, cl);
			
	        for (CtField field : cl.getDeclaredFields()) {
	        	// TODO we should find annotation in a better way
	        	if (!field.hasAnnotation(Id.class) &&
	        		!field.hasAnnotation(Column.class) &&
	        		!field.hasAnnotation(Version.class) &&
	        		!field.hasAnnotation(ForeignKey.class))
	        		continue;
	        	// TODO make sure it is not transient
	        	
	        	String accessorSuffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
	        	try {
	        		CtMethod accessor = cl.getDeclaredMethod("set" + accessorSuffix);
	        		enhanceSetter(field, accessor);
	        	} catch (NotFoundException e) {
	        		// ignore
	        	}
	        }
	        
        	enhanceLazyMethods(cl, classPool);

	        Class<?> newClass = cl.toClass();
	        entities.put(newClass, null);
		} catch (Exception e) {
			throw new RuntimeException("Unable to enhance entity " + className, e);
		}
	}
	
	private static void addStateAttribute(ClassPool classPool, CtClass cl) throws Exception {
		CtField f = new CtField(classPool.get("net.lecousin.reactive.data.relational.enhance.EntityState"), STATE_FIELD_NAME, cl);
		cl.addField(f);
		ConstPool cpool = cl.getClassFile().getConstPool();

        AnnotationsAttribute attr =
                new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(Transient.class.getName(), cpool);
        attr.addAnnotation(annot);
        f.getFieldInfo().addAttribute(attr);
	}
	
	private static void enhanceSetter(CtField field, CtMethod setter) throws Exception {
		setter.insertBefore("if ($0._lcState != null) { $0._lcState.fieldSet(\"" + field.getName() + "\", $1); }");
	}
	
	private static void enhanceLazyMethods(CtClass cl, ClassPool classPool) throws Exception {
		boolean hasLoadEntity = false;
		for (CtMethod method : cl.getMethods()) {
			if (method.getName().equals("entityLoaded") && method.getParameterTypes().length == 0 && method.getReturnType().getName().equals("boolean")) {
				method.setBody("return $0._lcState != null && $0._lcState.isLoaded();");
			} else if (method.getName().equals("loadEntity") && method.getParameterTypes().length == 0 && method.getReturnType().getName().equals(Mono.class.getName())) {
				method.setBody("return $0._lcState.load($0);");
				hasLoadEntity = true;
			}
		}
		if (!hasLoadEntity) {
			CtMethod m = CtNewMethod.make("public reactor.core.publisher.Mono loadEntity() { return $0._lcState.load($0); }", cl);
		    cl.addMethod(m);
		}
		for (CtMethod method : cl.getMethods()) {
			if (method.getName().startsWith("lazyGet")) {
				String propertyName = method.getName().substring(7);
				propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
				CtField field = cl.getField(propertyName);
				ForeignTable ft = (ForeignTable) field.getAnnotation(ForeignTable.class);
				if (ft != null) {
					if (field.getType().isArray() || field.getType().subtypeOf(classPool.get(Collection.class.getName()))) {
						method.setBody("return $0._lcState.lazyGetForeignTableCollectionField($0, \"" + propertyName + "\", \"" + ft.joinKey() + "\");");
					} else {
						method.setBody("return $0._lcState.lazyGetForeignTableField($0, \"" + propertyName + "\", \"" + ft.joinKey() + "\");");
					}
					continue;
				}
				ForeignKey fk = (ForeignKey) field.getAnnotation(ForeignKey.class);
				if (fk != null) {
					// if ForeignKey, ensure it is loaded
					method.setBody("return $0.get" + method.getName().substring(7) + "().loadEntity();");
					continue;
				}
				method.setBody("return $0.loadEntity().map($0._lcState.getFieldMapper($0, \"" + propertyName + "\"));");
			}
		}
	}
	
}
