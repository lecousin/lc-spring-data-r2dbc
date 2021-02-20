package net.lecousin.reactive.data.relational.enhance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SignatureAttribute.ObjectType;
import javassist.bytecode.SignatureAttribute.TypeArgument;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import net.lecousin.reactive.data.relational.annotations.ColumnDefinition;
import net.lecousin.reactive.data.relational.annotations.ForeignKey;
import net.lecousin.reactive.data.relational.annotations.ForeignTable;
import net.lecousin.reactive.data.relational.annotations.JoinTable;
import net.lecousin.reactive.data.relational.model.LcEntityTypeInfo;
import net.lecousin.reactive.data.relational.model.ModelException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

public final class Enhancer {
	
	private static final Log logger = LogFactory.getLog(Enhancer.class);
	
	public static final String STATE_FIELD_NAME = "_lcState";
	public static final String JOIN_TABLE_ATTRIBUTE_PREFIX = "entity";
	
	private static final String DEFAULT_VALUE_ANNOTATION_ATTRIBUTE = "value";
	
	private ClassPool classPool;
	private Map<String, CtClass> classes;
	private Map<CtClass, Map<String, JoinTableInfo>> joinTableFields = new HashMap<>();
	
	private Enhancer() {
		classPool = ClassPool.getDefault();
	}
	
	public static void enhance(Collection<String> entityClasses) throws ModelException {
		new Enhancer().enhanceClasses(entityClasses);
	}
	
	@SuppressWarnings("java:S1141")
	private void enhanceClasses(Collection<String> entityClasses) throws ModelException {
		logger.info("Enhancing " + entityClasses.size() + " entity classes");
		
		// read class files
		loadClasses(entityClasses);

		// process join table fields and create join classes
		processJoinTables();
		
		// add state attribute
		addStateAttribute();
		
		// persistent fields accessor
		enhancePersistentFields();
		
		// lazy methods
		enhanceLazyMethods();
		
		// process join table getter and setter
		for (Map.Entry<CtClass, Map<String, JoinTableInfo>> classEntry : joinTableFields.entrySet()) {
			for (Map.Entry<String, JoinTableInfo> propertyEntry : classEntry.getValue().entrySet()) {
				try {
					processJoinTableAccessors(classEntry.getKey(), propertyEntry.getKey(), propertyEntry.getValue());
				} catch (Exception e) {
					throw new ModelException("Error enhancing join table accessors for " + classEntry.getKey().getName() + "#" + propertyEntry.getKey(), e);
				}
			}
		}
		
		// load classes into JVM
		List<Class<?>> result = new ArrayList<>(classes.size());
		for (CtClass cl : classes.values()) {
			try {
	        	Class<?> neighbor = null;
	        	try {
	        		neighbor = Enhancer.class.getClassLoader().loadClass(cl.getPackageName() + ".package-info");
	        	} catch (Exception e) {
	        		// ignore
	        	}
		        Class<?> newClass = neighbor != null ? cl.toClass(neighbor) : cl.toClass();
		        result.add(newClass);
			} catch (Exception e) {
				throw new ModelException("Unable to load enhanced class " + cl.getName() + " into JVM", e);
			}
		}
		
		// set to cache
		LcEntityTypeInfo.setClasses(result);
	}
	
	private void loadClasses(Collection<String> entityClasses) throws ModelException {
		classes = new HashMap<>();
		for (String className : entityClasses) {
			try {
				CtClass cl = classPool.get(className);
				if (!cl.hasAnnotation(Table.class))
					throw new ModelException("Class is not an entity (no @Table annotation): " + className);
				if (hasField(cl, STATE_FIELD_NAME)) {
					logger.warn("Entity already enhanced: " + className);
					return;
				}
				cl.defrost();
				classes.put(className, cl);
			} catch (ModelException e) {
				throw e;
			} catch (Exception e) {
				throw new ModelException("Error loading class " + className, e);
			}
		}
	}
	
	private static boolean isPersistent(CtField field) {
		if (field.hasAnnotation(Transient.class) || field.hasAnnotation(Autowired.class) || field.hasAnnotation(Value.class))
			return false;
		return field.hasAnnotation(Id.class) ||
			field.hasAnnotation(Column.class) ||
    		field.hasAnnotation(ColumnDefinition.class) ||
    		field.hasAnnotation(Version.class) ||
    		field.hasAnnotation(ForeignKey.class);
	}
	
	private static boolean hasField(CtClass cl, String name) {
		try {
			cl.getDeclaredField(name);
			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}
	
	private void addStateAttribute() throws ModelException {
		for (CtClass cl : classes.values())
			try {
				addStateAttribute(cl);
			} catch (Exception e) {
				throw new ModelException("Unable to add state attribute to class " + cl.getName(), e);
			}
	}
	
	private void addStateAttribute(CtClass cl) throws CannotCompileException, NotFoundException {
		CtField f = new CtField(classPool.get("net.lecousin.reactive.data.relational.enhance.EntityState"), STATE_FIELD_NAME, cl);
		cl.addField(f);
		ConstPool cpool = cl.getClassFile().getConstPool();

        AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(Transient.class.getName(), cpool);
        attr.addAnnotation(annot);
        f.getFieldInfo().addAttribute(attr);
	}
	
	private void enhancePersistentFields() throws ModelException {
		for (CtClass cl : classes.values())
			try {
				enhancePersistentFields(cl);
			} catch (Exception e) {
				throw new ModelException("Error enhancing entity class " + cl.getName(), e);
			}
	}
	
	private static void enhancePersistentFields(CtClass cl) throws CannotCompileException {
		for (CtField field : cl.getDeclaredFields()) {
        	if (!isPersistent(field))
        		continue;
        	
        	String accessorSuffix = Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
        	try {
        		CtMethod accessor = cl.getDeclaredMethod("set" + accessorSuffix);
        		enhanceSetter(field, accessor);
        	} catch (NotFoundException e) {
        		// ignore
        	}
        }
	}
	
	private static void enhanceSetter(CtField field, CtMethod setter) throws CannotCompileException {
		setter.insertBefore("if ($0._lcState != null) { $0._lcState.fieldSet(\"" + field.getName() + "\", $1); }");
	}
	
	@SuppressWarnings("java:S3776")
	private void processJoinTables() throws ModelException {
		LinkedList<Tuple4<CtClass, CtField, JoinTable, CtClass>> joins = new LinkedList<>();
		CtClass clSet = null;
		try {
			clSet = classPool.get(Set.class.getName());
		} catch (Exception e) {
			// should never happen
		}
		// collect
		for (CtClass cl : classes.values()) {
			for (CtField field : cl.getDeclaredFields()) {
				if (!field.hasAnnotation(JoinTable.class))
					continue;
				try {
					JoinTable jt = (JoinTable) field.getAnnotation(JoinTable.class);
					CtClass type = field.getType();
					if (!type.subtypeOf(clSet))
						throw new ModelException("Attribute " + cl.getName() + "#" + field.getName() + " annotated with @JoinTable must be a Set");
					ObjectType ot = SignatureAttribute.toFieldSignature(field.getGenericSignature());
					if (!(ot instanceof SignatureAttribute.ClassType))
						throw new ModelException("Unexpected type " + ot + " for @JoinTable field, must be a Set with specified type: " + cl.getName() + '#' + field.getName());
					SignatureAttribute.ClassType ct = (SignatureAttribute.ClassType) ot;
					if (ct.getTypeArguments().length != 1)
						throw new ModelException("Unexpected type for @JoinTable field, must be a Set with 1 type argument: " + cl.getName() + '#' + field.getName());
					ot = ct.getTypeArguments()[0].getType();
					if (!(ot instanceof SignatureAttribute.ClassType))
						throw new ModelException("Unexpected collection element type " + ot + " for @JoinTable field: " + cl.getName() + '#' + field.getName());
					ct = (SignatureAttribute.ClassType) ot;
					CtClass target = classes.get(ct.getName());
					if (target == null)
						throw new ModelException("Unexpected collection element type " + ot + " for @JoinTable field: " + cl.getName() + '#' + field.getName());
					joins.add(Tuples.of(cl, field, jt, target));
				} catch (ModelException e) {
					throw e;
				} catch (Exception e) {
					throw new ModelException("Error getting @JoinTable field info for " + cl.getName() + '#' + field.getName(), e);
				}
			}
		}
		
		// create joins
		while (!joins.isEmpty()) {
			Tuple4<CtClass, CtField, JoinTable, CtClass> t = joins.removeFirst();
			try {
				createJoinTable(t, joins);
			} catch (ModelException e) {
				throw e;
			} catch (Exception e) {
				throw new ModelException("Error generating join table entity from " + t.getT1().getName() + '#' + t.getT2().getName(), e);
			}
		}
	}
	
	private static class JoinTableInfo {
		private String joinClassName;
		private int linkNumber;
	}
	
	@SuppressWarnings({"java:S3776", "java:S135"}) // complexity and several continue in loop
	private void createJoinTable(Tuple4<CtClass, CtField, JoinTable, CtClass> t, LinkedList<Tuple4<CtClass, CtField, JoinTable, CtClass>> joins) throws ModelException, ReflectiveOperationException, CannotCompileException, NotFoundException {
		List<Tuple4<CtClass, CtField, JoinTable, CtClass>> targetJoins = new LinkedList<>();
		for (Tuple4<CtClass, CtField, JoinTable, CtClass> tt : joins) {
			if (!tt.getT1().equals(t.getT4()))
				continue;
			if (!tt.getT3().tableName().equals(t.getT3().tableName()))
				continue;
			if (tt.getT3().joinProperty().length() > 0 && !tt.getT3().joinProperty().equals(t.getT2().getName()))
				continue;
			if (t.getT3().joinProperty().length() > 0 && !t.getT3().joinProperty().equals(tt.getT2().getName()))
				continue;
			targetJoins.add(tt);
		}

		if (targetJoins.size() > 1)
			throw new ModelException("@JoinTable on field " + t.getT1().getName() + '#' + t.getT2().getName() + " is ambiguous");

		CtClass class1;
		CtClass class2;
		CtField field1 = null;
		CtField field2 = null;
		String columnName1 = "";
		String columnName2 = "";
		if (t.getT1().getName().compareTo(t.getT4().getName()) < 0) {
			class1 = t.getT1();
			class2 = t.getT4();
			field1 = t.getT2();
			columnName1 = t.getT3().columnName();
		} else {
			class1 = t.getT4();
			class2 = t.getT1();
			field2 = t.getT2();
			columnName2 = t.getT3().columnName();
		}
		
		String tableName;
		
		if (targetJoins.isEmpty()) {
			if (t.getT3().joinProperty().length() > 0)
				throw new ModelException("@JoinTable on field " + t.getT1().getName() + '#' + t.getT2().getName() + " refers to a property (" + t.getT3().joinProperty() + ") that does not exist on " + t.getT4().getName());
			tableName = t.getT3().tableName();
		} else {
			Tuple4<CtClass, CtField, JoinTable, CtClass> tt = targetJoins.get(0);
			joins.remove(tt);
			tableName = tt.getT3().tableName();
			if (class1.equals(tt.getT1())) {
				field1 = tt.getT2();
				columnName1 = tt.getT3().columnName();
			} else {
				field2 = tt.getT2();
				columnName2 = tt.getT3().columnName();
			}
		}
		
		if (tableName.isEmpty()) {
			Table t1 = (Table) class1.getAnnotation(Table.class);
			Table t2 = (Table) class2.getAnnotation(Table.class);
			String name1 = t1.value();
			String name2 = t2.value();
			if (name1.isEmpty())
				name1 = class1.getSimpleName();
			if (name2.isEmpty())
				name2 = class2.getSimpleName();
			tableName = name1 + '_' + name2 + "_JOIN";
		}
		
		String joinClassName = class1.getPackageName();
		if (joinClassName != null)
			joinClassName = joinClassName + ".JoinEntity_" + class1.getSimpleName() + '_' + class2.getSimpleName();
		else
			joinClassName = "JoinEntity_" + class1.getSimpleName() + '_' + class2.getSimpleName();
		
		logger.info("Create join table class " + joinClassName + " with table name " + tableName);
		
		CtClass joinClass = classPool.makeClass(joinClassName);
		ClassFile joinClassFile = joinClass.getClassFile();
		ConstPool constPool = joinClassFile.getConstPool();
		
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(Table.class.getName(), constPool);
        annot.addMemberValue(DEFAULT_VALUE_ANNOTATION_ATTRIBUTE, new StringMemberValue(tableName, constPool));
        attr.addAnnotation(annot);
        joinClassFile.addAttribute(attr);
        
        createJoinTableField(joinClass, JOIN_TABLE_ATTRIBUTE_PREFIX + "1", class1, columnName1, constPool);
        createJoinTableField(joinClass, JOIN_TABLE_ATTRIBUTE_PREFIX + "2", class2, columnName2, constPool);
        
        if (field1 != null)
        	createJoinField(class1, field1, joinClassName, 1);
        if (field2 != null)
        	createJoinField(class2, field2, joinClassName, 2);
        
        classes.put(joinClassName, joinClass);
	}
	
	private static void createJoinTableField(CtClass joinClass, String fieldName, CtClass targetType, String columnName, ConstPool constPool) throws CannotCompileException {
		CtField field = new CtField(targetType, fieldName, joinClass);
		joinClass.addField(field);
		AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
		Annotation annot = new Annotation(ForeignKey.class.getName(), constPool);
        annot.addMemberValue("optional", new BooleanMemberValue(false, constPool));
        attr.addAnnotation(annot);
        if (columnName.length() > 0) {
        	annot = new Annotation(Column.class.getName(), constPool);
            annot.addMemberValue(DEFAULT_VALUE_ANNOTATION_ATTRIBUTE, new StringMemberValue(columnName, constPool));
            attr.addAnnotation(annot);
        }
        field.getFieldInfo().addAttribute(attr);
	}
	
	private void createJoinField(CtClass cl, CtField joinField, String joinClassName, int linkNumber) throws CannotCompileException, NotFoundException {
		ClassFile classFile = cl.getClassFile();
		ConstPool constPool = classFile.getConstPool();

		CtField field = new CtField(classPool.get(Collection.class.getName()), joinField.getName() + "_join", cl);
		field.setGenericSignature(new SignatureAttribute.ClassType(Collection.class.getName(), new TypeArgument[] { new TypeArgument(new SignatureAttribute.ClassType(joinClassName)) }).encode());
        cl.addField(field);
        AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation annot = new Annotation(ForeignTable.class.getName(), constPool);
        annot.addMemberValue("joinKey", new StringMemberValue(JOIN_TABLE_ATTRIBUTE_PREFIX + linkNumber, constPool));
        attr.addAnnotation(annot);
        field.getFieldInfo().addAttribute(attr);
        
        JoinTableInfo info = new JoinTableInfo();
        info.joinClassName = joinClassName;
        info.linkNumber = linkNumber;
        joinTableFields.computeIfAbsent(cl, c -> new HashMap<>()).put(joinField.getName(), info);
	}
	
	private void enhanceLazyMethods() throws ModelException {
		for (CtClass cl : classes.values())
			try {
				enhanceLazyMethods(cl);
			} catch (Exception e) {
				throw new ModelException("Error enhancing entity class " + cl.getName(), e);
			}
	}
	
	private void enhanceLazyMethods(CtClass cl) throws ReflectiveOperationException, CannotCompileException, NotFoundException {
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
		enhanceLazyGetMethods(cl);
	}
	
	@SuppressWarnings({"java:S3776", "java:S135"})
	private void enhanceLazyGetMethods(CtClass cl) throws ReflectiveOperationException, CannotCompileException, NotFoundException {
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
				JoinTable jt = (JoinTable) field.getAnnotation(JoinTable.class);
				if (jt != null) {
					JoinTableInfo info = joinTableFields.get(cl).get(propertyName);
					method.setBody("return $0._lcState.lazyGetJoinTableField($0, \"" + propertyName + "\", " + info.linkNumber + ");");
					continue;
				}
				ForeignKey fk = (ForeignKey) field.getAnnotation(ForeignKey.class);
				if (fk != null) {
					// if ForeignKey, ensure it is loaded
					method.setBody("return $0.get" + method.getName().substring(7) + "() != null ? $0.get" + method.getName().substring(7) + "().loadEntity() : reactor.core.publisher.Mono.empty();");
				} else {
					method.setBody("return $0.loadEntity().map($0._lcState.getFieldMapper($0, \"" + propertyName + "\"));");
				}
			}
		}
	}
	
	private static void processJoinTableAccessors(CtClass cl, String propertyName, JoinTableInfo joinTable) throws CannotCompileException {
    	String accessorSuffix = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    	try {
    		CtMethod accessor = cl.getDeclaredMethod("get" + accessorSuffix);
    		enhanceJoinTableGetter(cl.getDeclaredField(propertyName), accessor, joinTable);
    	} catch (NotFoundException e) {
    		// ignore
    	}
    	try {
    		CtMethod accessor = cl.getDeclaredMethod("set" + accessorSuffix);
    		enhanceJoinTableSetter(cl.getDeclaredField(propertyName), accessor, joinTable);
    	} catch (NotFoundException e) {
    		// ignore
    	}
	}
	
	private static void enhanceJoinTableGetter(CtField field, CtMethod getter, JoinTableInfo joinTable) throws CannotCompileException {
		StringBuilder body = new StringBuilder();
		body.append('{');
		body.append("if ($0.").append(field.getName()).append(" != null) return $0.").append(field.getName()).append(';');
		body.append("if ($0.").append(field.getName()).append("_join != null) return $0.").append(field.getName()).append(" = new net.lecousin.reactive.data.relational.model.JoinTableCollectionToTargetCollection($0, $0.").append(field.getName()).append("_join, \"").append(joinTable.joinClassName).append("\", ").append(joinTable.linkNumber).append(");");
		body.append("return null;");
		body.append('}');
		getter.setBody(body.toString());
	}
	
	private static void enhanceJoinTableSetter(CtField field, CtMethod setter, JoinTableInfo joinTable) throws CannotCompileException {
		StringBuilder body = new StringBuilder();
		body.append('{');
		body.append("$0.").append(field.getName()).append("_join = new net.lecousin.reactive.data.relational.model.JoinTableCollectionFromTargetCollection($0, $0.").append(field.getName()).append("_join, $1, \"").append(joinTable.joinClassName).append("\", ").append(joinTable.linkNumber).append(");");
		body.append("$0.").append(field.getName()).append(" = $1;");
		body.append('}');
		setter.setBody(body.toString());
	}
	
}
