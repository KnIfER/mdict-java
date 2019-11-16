package org.adrianwalker.multilinestring;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;

import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes({"org.adrianwalker.multilinestring.Multiline"})
public final class JavacMultilineProcessor extends AbstractProcessor {

	private JavacElements elementUtils;
	private TreeMaker maker;
  
	@Override
	public void init(final ProcessingEnvironment procEnv) {
		super.init(procEnv);
		JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
		this.elementUtils = javacProcessingEnv.getElementUtils();
		this.maker = TreeMaker.instance(javacProcessingEnv.getContext());
	}

	@Override public SourceVersion getSupportedSourceVersion() {
               return SourceVersion.latest();
	}

	public static Pattern comments=Pattern.compile("((?<!:)//.*)?(\r)?\n");

	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		Set<? extends Element> fields = roundEnv.getElementsAnnotatedWith(Multiline.class);
		for (Element field : fields) {
			String docComment = elementUtils.getDocComment(field);
			if (null != docComment) {
				Multiline annotation = field.getAnnotation(Multiline.class);
				if(annotation.trim()){
					docComment=comments.matcher(docComment).replaceAll(" ");
					docComment=docComment.replaceAll("\\s+"," ");
					docComment=docComment.replaceAll(" ?([={}<>;,+\\-]) ?","$1");
				}

				JCVariableDecl fieldNode = (JCVariableDecl) elementUtils.getTree(field);
				fieldNode.init = maker.Literal(docComment);
			}
		}
		return true;
	}
}
