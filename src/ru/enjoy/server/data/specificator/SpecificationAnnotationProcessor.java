package ru.enjoy.server.data.specificator;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class SpecificationAnnotationProcessor extends AbstractProcessor {

	private static final String ATTR_CHILD_CLASS = "childClass";
	private static final String ATTR_ID_FIELD = "idField";

	private Types typeUtils;
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotataions = new LinkedHashSet<String>();
		annotataions.add(ChildList.class.getCanonicalName());
		annotataions.add(DataObject.class.getCanonicalName());
		return annotataions;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<Element> DataObjectClassSet = new LinkedHashSet<>();
		for (Element el : roundEnv.getElementsAnnotatedWith(DataObject.class)) {
			DataObjectClassSet.add(el);
		}
		// Проверка аннотаций ChildList
		for (Element el : roundEnv.getElementsAnnotatedWith(ChildList.class)) {
			ChildList childList = el.getAnnotation(ChildList.class);

			// Проверка атрибута childClass
			String childClassName = childList.childClass();
			// Имя класса не может быть пустым
			if (childClassName.isEmpty()) {
				printMessage("Attribute '" + ATTR_CHILD_CLASS + "' can not be empty.", el,
						ChildList.class.getCanonicalName(), ATTR_CHILD_CLASS);
			} else {
				// Класс должен существовать
				TypeElement childClass = elementUtils.getTypeElement(childClassName);
				if (childClass == null) {
					printMessage("Class '" + childClassName + "' does not exist.", el,
							ChildList.class.getCanonicalName(), ATTR_CHILD_CLASS);
				} else {
					// У класса на который ссылаемся должна быть аннотация
					// DataObject
					if (findAnnotationMirror(childClass,DataObject.class.getCanonicalName())==null) {
						printMessage("Class '" + childClassName + "' has not '" + DataObject.class.getName()
								+ "' annotation.", el, ChildList.class.getCanonicalName(), ATTR_CHILD_CLASS);
					}
				}
			}
			
			// Проверка атрибута idField
			String idFieldName = childList.idField();
			
			if (!idFieldName.isEmpty()) {
				if (findFieldElement((TypeElement)el.getEnclosingElement(), idFieldName)==null){
					printMessage("Field '" + idFieldName + "' does not exist in current class.", el,
							ChildList.class.getCanonicalName(), ATTR_ID_FIELD);
				}
			}
		}
		messager.printMessage(Diagnostic.Kind.ERROR, "STACK2");
		return true;
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
	
	private VariableElement findFieldElement(TypeElement clsElem, String fieldName){
		for (VariableElement fld : ElementFilter.fieldsIn(clsElem.getEnclosingElement().getEnclosedElements())) {
			if (fld.getSimpleName().toString().equals(fieldName))
				return fld;
		}
		return null;
	}

	private void printMessage(CharSequence msg, Element e, String annClass, String annAttr) {
		AnnotationMirror am = findAnnotationMirror(e, annClass);
		if (am != null) {
			AnnotationValue av = findAnnotationValue(am, annAttr);
			if (av != null) {
				messager.printMessage(Diagnostic.Kind.ERROR, msg, e, am, av);
			}
		}
	}
}
