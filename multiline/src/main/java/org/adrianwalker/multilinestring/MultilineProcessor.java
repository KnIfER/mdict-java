package org.adrianwalker.multilinestring;


import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({"org.adrianwalker.multilinestring.Multiline"})
public final class MultilineProcessor extends AbstractProcessor {
  private Processor delegator = null;
  
  @Override
  public void init(final ProcessingEnvironment procEnv) {
	  super.init(procEnv);
	  delegator = new JavacMultilineProcessor();
	  delegator.init(procEnv);
  }
  
  @Override 
	public SourceVersion getSupportedSourceVersion() 
	{
      return SourceVersion.latest();
	}

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
	  return delegator.process(annotations, roundEnv);
  }
}
