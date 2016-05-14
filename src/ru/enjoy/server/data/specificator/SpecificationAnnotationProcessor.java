package ru.enjoy.server.data.specificator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

public class SpecificationAnnotationProcessor extends AbstractProcessor {

	private static final String ATTR_CHILD_CLASS = "childClass";
	private static final String ATTR_ID_FIELD = "idField";
	private static final String ATTR_REF_FIELD = "refField";
	private static final String ATTR_TYPE_CODE = "typeCode";
	private static final String ATTR_FIELDS_ORDER = "fieldsOrder";
	private static final Set<String> ALLOWED_IDFIELD_TYPES = new LinkedHashSet<>(
			Arrays.asList("int", "long", "java.lang.String"));
	private static final Set<String> ALLOWED_FIELD_TYPES = new LinkedHashSet<>(
			Arrays.asList("int", "long", "double", "java.lang.String"));
	private static final String CHILDLIST_ANN_CLASSNAME = ChildList.class.getCanonicalName();
	private static final String DATAOBJ_ANN_CLASSNAME = DataObject.class.getCanonicalName();

	private Elements elementUtils;
	private Messager messager;

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return new LinkedHashSet<String>(Arrays.asList(CHILDLIST_ANN_CLASSNAME, DATAOBJ_ANN_CLASSNAME));
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
		for (Element el : roundEnv.getElementsAnnotatedWith(ChildList.class))
			workChildList(el);
		for (Element el : roundEnv.getElementsAnnotatedWith(DataObject.class))
			workDataObject(el);
		return true;
	}

	private void workDataObject(Element el) {
		DataObject dataObject = el.getAnnotation(DataObject.class);
		String typeCode = dataObject.typeCode();
		String[] fieldsOrder = dataObject.fieldsOrder();
		if (typeCode.isEmpty()) {
			printMessage(el, DATAOBJ_ANN_CLASSNAME, ATTR_TYPE_CODE, "Attribute '%s' can not be empty", ATTR_TYPE_CODE);
		}
		for (String fldName : fieldsOrder) {
			if (fldName.isEmpty())
				continue;
			VariableElement fldEl = findFieldElement((TypeElement) el, fldName);
			if (fldEl == null) {
				printMessage(el, DATAOBJ_ANN_CLASSNAME, ATTR_FIELDS_ORDER, "Field '%s' does not exist", fldName);
				continue;
			}
			if(!fldEl.getModifiers().contains(Modifier.PUBLIC)){
				printMessage(el, DATAOBJ_ANN_CLASSNAME, ATTR_FIELDS_ORDER, "Field '%s' does not public.", fldName);
			}
			if(fldEl.getModifiers().contains(Modifier.STATIC)){
				printMessage(el, DATAOBJ_ANN_CLASSNAME, ATTR_FIELDS_ORDER, "Field '%s' is static.", fldName);
			}
			String type = fldEl.asType().toString();
			if (!ALLOWED_FIELD_TYPES.contains(type)){
				printMessage(el, DATAOBJ_ANN_CLASSNAME, ATTR_FIELDS_ORDER, "Field '%s' has type '%s', but type must be in list: %s.", fldName, type, ALLOWED_FIELD_TYPES.toString());
			}
		}
	}

	private AnnotationMirror findAnnotationMirror(Element annotatedElement, String annotationClassName) {
		List<? extends AnnotationMirror> annotationMirrors = annotatedElement.getAnnotationMirrors();

		for (AnnotationMirror am : annotationMirrors) {
			if (am.getAnnotationType().toString().equals(annotationClassName)) {
				return am;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AnnotationValue findAnnotationValue(AnnotationMirror am, String attrName) {
		Map anVals = am.getElementValues();
		Set<ExecutableElement> ks = anVals.keySet();
		for (ExecutableElement k : ks) {
			AnnotationValue av = (AnnotationValue) anVals.get(k);
			if (k.getSimpleName().toString().equals(attrName)) {
				return av;
			}
		}
		return null;
	}

	private VariableElement findFieldElement(TypeElement clsElem, String fieldName) {
		for (VariableElement fld : ElementFilter.fieldsIn(clsElem.getEnclosedElements())) {
			if (fld.getSimpleName().toString().equals(fieldName))
				return fld;
		}
		return null;
	}

	private void printMessage(Element e, String annClass, String annAttr, String msg, Object... args) {
		AnnotationMirror am = findAnnotationMirror(e, annClass);
		if (am != null) {
			AnnotationValue av = findAnnotationValue(am, annAttr);
			if (av != null) {
				messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e, am, av);
			}
		}
	}

	private void workChildList(Element el) {
		ChildList childList = el.getAnnotation(ChildList.class);

		String childClassName = childList.childClass();
		String idFieldName = childList.idField();
		VariableElement idFldEl = null;
		String refFieldName = childList.refField();
		TypeElement childClassElem = null;
		String idFieldTypeName = null;

		// Проверка атрибута childClass
		// Имя класса не может быть пустым
		if (childClassName.isEmpty()) {
			printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_CHILD_CLASS, "Attribute '%s' can not be empty.",
					ATTR_CHILD_CLASS);
		} else {
			childClassElem = elementUtils.getTypeElement(childClassName);
			// Класс должен существовать
			if (childClassElem == null) {
				printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_CHILD_CLASS, "Class '%s' does not exist.",
						childClassName);
			} else {
				// У класса на который ссылаемся должна быть аннотация
				// DataObject
				if (findAnnotationMirror(childClassElem, DATAOBJ_ANN_CLASSNAME) == null) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_CHILD_CLASS, "Class '%s' has not '%s' annotation.",
							childClassName, DataObject.class.getName());
				}
			}
		}

		if (idFieldName.isEmpty() != refFieldName.isEmpty()) {
			String msg = String.format("Attributes '%s' and '%s' must be both empty, or both not empty.",
					ATTR_REF_FIELD, ATTR_ID_FIELD);
			printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD, msg);
			printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD, msg);

		}

		// Проверка атрибута idField
		// idField может быть пустым, что значит в список нужно включить все
		// объекты а не только те, у которых значение refField совпадает с
		// idField
		if (!idFieldName.isEmpty()) {
			// Но если idField не пусто, то должно существовать поле с этим
			// именем
			idFldEl = findFieldElement((TypeElement) el.getEnclosingElement(), idFieldName);
			if (idFldEl == null) {
				printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD, "Field '%s' does not exist in current class.",
						idFieldName);
			} else {
				// поле должно быть публичным
				if (!idFldEl.getModifiers().contains(Modifier.PUBLIC)) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD,
							"Field '%s' does not public. Private field can not be used as 'idField'.", idFieldName);
				}
				// Не должно быть статичным
				if (idFldEl.getModifiers().contains(Modifier.STATIC)) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD,
							"Field '%s' is static. Static field can not be used as 'idField'.", idFieldName);
				}
				// тип поля должен быть из множества ALLOWED_IDFIELD_TYPES
				idFieldTypeName = idFldEl.asType().toString();
				if (!ALLOWED_IDFIELD_TYPES.contains(idFieldTypeName)) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD,
							"Field '%s' has type = '%s'. But it is allowed to use as key only %s.", idFieldName,
							idFieldTypeName, ALLOWED_IDFIELD_TYPES.toString());
				}
			}
		}
		if ((!refFieldName.isEmpty()) && (childClassElem != null)) {
			VariableElement refFldEl = findFieldElement(childClassElem, refFieldName);
			if (refFldEl == null) {
				printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD, "Field '%s' does not exist in class '%s'.",
						refFieldName, childClassName);
			} else {
				// поле должно быть публичным
				if (!refFldEl.getModifiers().contains(Modifier.PUBLIC)) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD,
							"Field '%s' does not public. Private field can not be used as '%s'.", refFieldName,
							ATTR_REF_FIELD);
				}
				// Не должно быть статичным
				if (refFldEl.getModifiers().contains(Modifier.STATIC)) {
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD,
							"Field '%s' is static. Static field can not be used as '%s'.", refFieldName,
							ATTR_REF_FIELD);
				}
				// Тип поля ссылки должен совпадать с типом поля ключа
				String refFieldTypeName = refFldEl.asType().toString();
				if ((idFldEl != null) && (!refFieldTypeName.equals(idFieldTypeName))) {
					String msg = String.format(
							"Field '%s' of class '%s' has type '%s', but field '%s' has type '%s'. Both fields must have same type",
							refFieldName, childClassName, refFieldTypeName, idFieldName, idFieldTypeName);
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_REF_FIELD, msg);
					printMessage(el, CHILDLIST_ANN_CLASSNAME, ATTR_ID_FIELD, msg);
				}
			}

		}
	}
}
