package ru.enjoy.server.data.specificator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public class SpecificationAnnotationProcessor extends AbstractProcessor {

	private static final String ATTR_CHILD_CLASS = "childClass";
	private static final String ATTR_ID_FIELD = "idField";
	private static final String ATTR_REF_FIELD = "refField";
	private static final String ATTR_TYPE_CODE = "typeCode";
	private static final String ATTR_FIELDS_ORDER = "fieldsOrder";
	private static final String ATTR_CLS = "cls";
	private static final Set<String> ALLOWED_IDFIELD_TYPES = new LinkedHashSet<>(
			Arrays.asList("int", "long", "java.lang.String"));
	private static final Set<String> ALLOWED_FIELD_TYPES = new LinkedHashSet<>(
			Arrays.asList("int", "long", "double", "java.lang.String"));
	private static final String CHILDLIST_ANN_CLASSNAME = Child.class.getName();
	private static final String DATAOBJ_ANN_CLASSNAME = DataObject.class.getName();
	private static final String TABLELIST_ANN_CLASSNAME = TableList.class.getName();

	private Elements elementUtils;
	private Messager messager;

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return new LinkedHashSet<String>(
				Arrays.asList(CHILDLIST_ANN_CLASSNAME, DATAOBJ_ANN_CLASSNAME, TABLELIST_ANN_CLASSNAME));
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		elementUtils = processingEnv.getElementUtils();
		messager = processingEnv.getMessager();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		// Проверка аннотаций ChildList
		for (Element el : roundEnv.getElementsAnnotatedWith(Child.class))
			workChildList(el);
		for (Element el : roundEnv.getElementsAnnotatedWith(DataObject.class))
			workDataObject(el);
		for (Element el : roundEnv.getElementsAnnotatedWith(TableList.class))
			workTableList(el);
		return true;
	}

	private void workTableList(Element el) {
		TableList tableList = el.getAnnotation(TableList.class);
		String cls = tableList.cls();
		AnnMessager annTableListMessager = new AnnMessager(messager, el, TABLELIST_ANN_CLASSNAME, ATTR_CLS);
		testGetClass(cls, annTableListMessager);
	}

	private void workDataObject(Element el) {
		DataObject dataObject = el.getAnnotation(DataObject.class);
		String typeCode = dataObject.typeCode();
		if ((typeCode != null) && typeCode.isEmpty()) {
			(new AnnMessager(messager, el, DATAOBJ_ANN_CLASSNAME, ATTR_TYPE_CODE))
					.printMessage("Attribute '%s' can not be empty", ATTR_TYPE_CODE);
		}
		AnnMessager fieldsOrderMessager = new AnnMessager(messager, el, DATAOBJ_ANN_CLASSNAME, ATTR_FIELDS_ORDER);
		String[] fieldsOrder = dataObject.fieldsOrder();
		if (fieldsOrder != null) {
			for (String fldName : fieldsOrder) {
				if (fldName.isEmpty())
					continue;
				testGetField((TypeElement) el, fldName, ALLOWED_FIELD_TYPES, fieldsOrderMessager);
			}
		}
	}

	private void workChildList(Element el) {
		Child childList = el.getAnnotation(Child.class);

		String childClassName = childList.childClass();
		String[] idFieldNames = childList.idField();
		String[] refFieldNames = childList.refField();

		AnnMessager annChildClassMessager = new AnnMessager(messager, el, CHILDLIST_ANN_CLASSNAME, ATTR_CHILD_CLASS);
		TypeElement ChildClassEl = testGetClass(childClassName, annChildClassMessager);

		AnnMessager annIdFieldMessager = new AnnMessager(messager, el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD);
		VariableElement[] idFldEls = new VariableElement[idFieldNames.length];
		for (int i = 0; i < idFieldNames.length; i++) {
			idFldEls[i] = testGetField((TypeElement) el.getEnclosingElement(), idFieldNames[i], ALLOWED_IDFIELD_TYPES,
					annIdFieldMessager);
		}

		AnnMessager annRefFieldMessager = new AnnMessager(messager, el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD);
		VariableElement[] refFldEls = new VariableElement[refFieldNames.length];
		for (int i = 0; i < refFieldNames.length; i++) {
			refFldEls[i] = testGetField(ChildClassEl, refFieldNames[i], null, annRefFieldMessager);
		}
		if(idFieldNames.length!=refFieldNames.length){
			String msg = String.format("Number of parts in %s not equal number of parts in %s",
					ATTR_ID_FIELD, ATTR_REF_FIELD);
			annIdFieldMessager.printMessage(msg);
			annRefFieldMessager.printMessage(msg);
			return;
		}
		for (int i = 0; i < refFieldNames.length; i++) {
			if ((idFldEls[i] != null) && (refFldEls[i] != null)) {
				if (!idFldEls[i].asType().toString().equals(refFldEls[i].asType().toString())) {
					String msg = String.format("Type of field '%s' not equal type of field '%s' of class '%s'",
							idFieldNames[i], refFieldNames[i], childClassName);
					annIdFieldMessager.printMessage(msg);
					annRefFieldMessager.printMessage(msg);
				}
			}
		}
	}

	/**
	 * Проверка атрибута аннотации являющегося именем поля На то что оно: 1 -
	 * значение непустое 2 - в enclosingEl существует поле с таким именем 3 -
	 * публичное 4 - нестатичное 5 - тип поля входит в указанный список
	 */
	private VariableElement testGetField(TypeElement enclosingEl, String fieldName, Set<String> allowedTypes,
			AnnMessager annMessager) {
		if (fieldName.isEmpty()) {
			annMessager.printMessage("Value of attribute can not be empty");
			return null;
		}
		if (enclosingEl == null)
			return null;
		VariableElement fldElem = AnnUtils.findFieldElement(enclosingEl, fieldName);
		if (fldElem == null) {
			annMessager.printMessage("Field '%s' does not exist in class '%s'", fieldName, enclosingEl.getSimpleName());
			return null;
		}
		if (!fldElem.getModifiers().contains(Modifier.PUBLIC)) {
			annMessager.printMessage("Field '%s' does not public", fieldName);
		}
		if (fldElem.getModifiers().contains(Modifier.STATIC)) {
			annMessager.printMessage("Field '%s' is static", fieldName);
		}
		String typeName = fldElem.asType().toString();
		if ((allowedTypes != null) && (!allowedTypes.contains(typeName))) {
			annMessager.printMessage("Field '%s' is '%s', but allowed only types: %s", fieldName, typeName,
					allowedTypes.toString());
			;
		}
		return fldElem;
	}

	private TypeElement testGetClass(String className, AnnMessager annMessager) {
		if (className == null)
			return null;
		if (className.isEmpty()) {
			annMessager.printMessage("Value of attribute can not be empty.");
			return null;
		}
		TypeElement classElem = elementUtils.getTypeElement(className);
		if (classElem == null) {
			annMessager.printMessage("'%s' is not valid class name.", className);
			return null;
		}
		if (!classElem.getModifiers().contains(Modifier.PUBLIC)) {
			annMessager.printMessage("Class '%s' is not public", className);
		}
		if (AnnUtils.findAnnotationMirror(classElem, DATAOBJ_ANN_CLASSNAME) == null) {
			annMessager.printMessage("Class '%s' has not '@%s' annotation", className,
					DataObject.class.getSimpleName());
		}
		return classElem;
	}
}
