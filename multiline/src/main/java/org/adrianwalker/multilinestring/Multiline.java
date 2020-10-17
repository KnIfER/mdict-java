package org.adrianwalker.multilinestring;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface Multiline {
	boolean trim() default true;
	
	int flagPos() default 0;
	int flagSize() default 1;
	int shift() default 0;
	int elevation() default 0;
	int max() default 0;
	int debug() default -1;
	
	String file() default "";
	String to() default "";
	String charset() default "";
	
	boolean compile() default false;
}
