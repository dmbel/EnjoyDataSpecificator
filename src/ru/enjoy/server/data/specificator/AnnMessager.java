package ru.enjoy.server.data.specificator;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public class AnnMessager {
	private Element element;
	private String annClass;
	private String annAttr;
	private Messager messager;

	public AnnMessager(Messager messager, Element element, String annClass, String annAttr) {
		this.element = element;
		this.annClass = annClass;
		this.annAttr = annAttr;
		this.messager = messager;
	}
	
	public void printMessage(String msg, Object... args){
		String msgRes = String.format(msg, args);
		AnnotationMirror am = AnnUtils.findAnnotationMirror(element, annClass);
		if (am != null) {
			AnnotationValue av = AnnUtils.findAnnotationValue(am, annAttr);
			if (av != null) {
				messager.printMessage(Diagnostic.Kind.ERROR, msgRes, element, am, av);
			} else {
				messager.printMessage(Diagnostic.Kind.ERROR, msgRes, element, am);
			}
		} else {
			messager.printMessage(Diagnostic.Kind.ERROR, msgRes, element);
		}
	}
	
}
