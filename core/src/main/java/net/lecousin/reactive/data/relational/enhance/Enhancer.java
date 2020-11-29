package net.lecousin.reactive.data.relational.enhance;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import javassist.CannotCompileException;
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
import net.lecousin.reactive.data.relational.model.ModelAccessException;
import net.lecousin.reactive.data.relational.model.ModelException;
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
				throw new ModelAccessException("Error accessing to enhanced class " + newClass.getName(), e);
			}
		}
	}
	
	private static void enhanceClass(ClassPool classPool, String className) {
		logger.info("Enhancing entity " + className);
		try {
			CtClass cl = classPool.get(className);
			if (!cl.hasAnnotation(Table.class))
				throw new ModelException("Class is not an entity (no @Table annotation): " + className);
			try {
				cl.getDeclaredField(STATE_FIELD_NAME);
				logger.warn("Entity already enhanced: " + className);
				return;
			} catch (NotFoundException e) {
				// ok
			}
			cl.defrost();

	        addStateAttribute(classPool, cl);
			
	        for (CtField field : cl.getDeclaredFields()) {
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
			throw new ModelAccessException("Unable to enhance entity " + className, e);
		}
	}
	
	private static void addStateAttribute(ClassPool classPool, CtClass cl) throws CannotCompileException, NotFoundException {
		CtField f = new CtField(classPool.get("net.lecousin.reactive.data.relational.enhance.EntityState"), STATE_FIELD_NAME, cl);
		cl.addField(f);
		ConstPool cpool = cl.getClassFile().getConstPool();

        AnnotationsAttribute attr =
                new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(Transient.class.getName(), cpool);
        attr.addAnnotation(annot);
        f.getFieldInfo().addAttribute(attr);
	}
	
	private static void enhanceSetter(CtField field, CtMethod setter) throws CannotCompileException {
		setter.insertBefore("if ($0._lcState != null) { $0._lcState.fieldSet(\"" + field.getName() + "\", $1); }");
	}
	
	private static void enhanceLazyMethods(CtClass cl, ClassPool classPool) throws ReflectiveOperationException, CannotCompileException, NotFoundException {
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
		enhanceLazyGetMethods(cl, classPool);
	}
	
	@SuppressWarnings("java:S3776")
	private static void enhanceLazyGetMethods(CtClass cl, ClassPool classPool) throws ReflectiveOperationException, CannotCompileException, NotFoundException {
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
				} else {
					ForeignKey fk = (ForeignKey) field.getAnnotation(ForeignKey.class);
					if (fk != null) {
						// if ForeignKey, ensure it is loaded
						method.setBody("return $0.get" + method.getName().substring(7) + "().loadEntity();");
					} else {
						method.setBody("return $0.loadEntity().map($0._lcState.getFieldMapper($0, \"" + propertyName + "\"));");
					}
				}
			}
		}
	}
	
}
