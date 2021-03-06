package ru.enjoy.server.data.specificator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface DataObject {
	String typeCode();
	String[] fieldsOrder();
}
