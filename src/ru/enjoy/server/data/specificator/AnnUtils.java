package ru.enjoy.server.data.specificator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

public class AnnUtils {
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static AnnotationValue findAnnotationValue(AnnotationMirror am, String attrName) {
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
	
	public static AnnotationMirror findAnnotationMirror(Element annotatedElement, String annotationClassName) {
		List<? extends AnnotationMirror> annotationMirrors = annotatedElement.getAnnotationMirrors();

		for (AnnotationMirror am : annotationMirrors) {
			if (am.getAnnotationType().toString().equals(annotationClassName)) {
				return am;
			}
		}
		return null;
	}
	
	public static VariableElement findFieldElement(TypeElement clsElem, String fieldName) {
		for (VariableElement fld : ElementFilter.fieldsIn(clsElem.getEnclosedElements())) {
			if (fld.getSimpleName().toString().equals(fieldName))
				return fld;
		}
		return null;
	}
}
