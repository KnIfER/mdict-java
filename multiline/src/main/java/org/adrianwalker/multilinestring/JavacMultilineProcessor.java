package org.adrianwalker.multilinestring;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.squareup.javapoet.TypeName;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;


@SupportedAnnotationTypes({"org.adrianwalker.multilinestring.Multiline"})
public final class JavacMultilineProcessor extends AbstractProcessor {
	
	private JavacElements elementUtils;
	private TreeMaker maker;
	private Context context;
	
	@Override
	public void init(final ProcessingEnvironment procEnv) {
		super.init(procEnv);
		JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment) procEnv;
		this.elementUtils = javacProcessingEnv.getElementUtils();
		this.maker = TreeMaker.instance(context=javacProcessingEnv.getContext());
	}
	
	@Override public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
	
	public static Pattern comments=Pattern.compile("((?<![:/])//.*)?(\r)?\n");
	
	@Override
	public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
		Set<? extends Element> fields = roundEnv.getElementsAnnotatedWith(Multiline.class);
		for (Element field : fields) {
			Multiline annotation = field.getAnnotation(Multiline.class);
			ElementKind KIND = field.getKind();
			if(KIND == ElementKind.FIELD||KIND==ElementKind.LOCAL_VARIABLE) {
				String docComment = elementUtils.getDocComment(field);
				String file = annotation.file();
				boolean fromFile = file.length()>0;
				boolean jsFile = fromFile&&file.endsWith(".js");
				String charset = "UTF-8";
				JCVariableDecl varDcl = (JCVariableDecl) elementUtils.getTree(field);
				TypeName typeName = TypeName.get(field.asType());
				String name = typeName.toString();
				CMN.Log("processing...", System.getProperty("java.class.path"));
				if(fromFile) {
					File path = null;
					if(!file.contains(":")||!file.startsWith("/")) {
						try {
							File project_path = new File("");
							path = new File(project_path.getCanonicalFile(), file);
						} catch (IOException ignored) { }
					}
					if(path==null) {
						path = new File(file);
					}
					//String to = annotation.to();
//					if(!StringUtils.isEmpty(to)) {
//						try {
//							File toDir = new File("");
//							toDir = new File(toDir.getCanonicalPath(), to);
//							if(toDir.getParentFile().exists()&&(path.lastModified()>toDir.lastModified()||annotation.debug()==1)) {
//								FileInputStream fin = new FileInputStream(path);
//								FileOutputStream fout = new FileOutputStream(toDir);
//								byte[] buffer = new byte[1024];
//								int len;
//								while((len=fin.read(buffer))>0) {
//									fout.write(buffer,0,len);
//								}
//								fin.close();
//								fout.close();
//							}
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
					if(name.equals("int")) {
						varDcl.init = maker.Literal((int)path.length());
						if(!path.exists()) {
							throw new IllegalArgumentException("File Not Exist : "+path);
						}
						continue;
					}
					if(docComment==null) { //only for string. for bytes read them from file.
						byte[] bytes = new byte[(int) path.length()];
						try {
							FileInputStream fin = new FileInputStream(path);
							fin.read(bytes);
							fin.close();
							docComment = new String(bytes, StandardCharsets.UTF_8);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				if (docComment!=null) {
					if(annotation.trim()){
						docComment=comments.matcher(docComment).replaceAll(" ");
						docComment=docComment.replaceAll("\\s+"," ");
						docComment=docComment.replaceAll(" ?([={}<>;,+\\-]) ?","$1");
					}
					if(annotation.compile()) {
						SourceFile source = SourceFile.fromCode("input.js", docComment);
						SourceFile extern = SourceFile.fromCode("extern.js","");
						CompilerOptions opt = new CompilerOptions();
						//opt.set;
						Compiler compiler = new Compiler();
						CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(opt);
						Result res = compiler.compile(extern, source, opt);
						opt.setOutputCharset(StandardCharsets.UTF_8);
						CMN.Log("编译JS", field.getSimpleName(), res.success);
						if(res.success) {
							//CMN.Log("-->成功", compiler.toSource());
							docComment = compiler.toSource();
						} else {
							CMN.Log("-->失败", res.errors.toString());
						}
					}
					JCLiteral doclet = maker.Literal(docComment);
					if(name.equals(String.class.getName())) {
						varDcl.init = doclet;
						CMN.Log("string...", varDcl.init);
					} else if(name.equals("byte[]")) {
						if(false) {
							byte[] data = docComment.getBytes();
							JCExpression[] dataExp = new JCExpression[data.length];
							for (int i = 0; i < data.length; i++) {
								dataExp[i] = maker.Literal((int)data[i]);
							}
							varDcl.init = maker.NewArray(maker.TypeIdent(TypeTag.BYTE), List.nil(), List.from(dataExp));
						} else {
							Names names = Names.instance(context);
							JCFieldAccess select = maker.Select(doclet, names.fromString("getBytes"));
							JCFieldAccess charAccess = maker.Select(maker.Ident(names.fromString("java.nio.charset")), names.fromString("Charset"));
							charAccess = maker.Select(charAccess, names.fromString("forName"));
							List<JCExpression> charArgs = List.of(maker.Apply(List.nil(), charAccess, List.of(maker.Literal(charset))));
							varDcl.init = maker.Apply(List.nil(), select, charArgs);
						}
						//CMN.Log( "processed_len:",data.length, varDcl.init);
					}
				}
			}
			else if(KIND == ElementKind.METHOD){
				JCMethodDecl metDcl = (JCMethodDecl) elementUtils.getTree(field);
				//TypeName typeName = TypeName.get();
				//String name = typeName.toString();
				//metDcl.body = maker.Block()
				CMN.Log(field, metDcl, metDcl.body.flags);
				List<JCStatement> statements = metDcl.body.stats;
				//CMN.Log(statements.toArray());
				JCStatement _1st = statements.get(0);
				CMN.Log("_1st", _1st);
				
				JCTree retType = metDcl.getReturnType();
				if(retType instanceof JCPrimitiveTypeTree) {
					JCPrimitiveTypeTree RETType = (JCPrimitiveTypeTree) retType;
					if(statements.length()==2&&statements.get(1) instanceof JCThrow) {
						if(_1st instanceof JCExpressionStatement) {
							JCExpressionStatement stat = (JCExpressionStatement) _1st;
							JCAssign assgn = (JCAssign) stat.expr;
							JCExpression flag = assgn.getVariable(); //JCIdent
							int flagPos = annotation.flagPos();
							int mask = (1<<annotation.flagSize())-1;
							long maskVal = ((long)mask)<<annotation.flagPos();
							int max = annotation.max();
							int shift = annotation.shift();
							if(max==0) {
								max = mask;
							}
							int elevation = annotation.elevation();
							List<JCVariableDecl> parms = metDcl.getParameters();
							
							boolean TogglePosFlag=false;
							try {
								TogglePosFlag=((JCIdent)((JCNewClass)((JCThrow)statements.get(1)).expr).clazz).name.toString().equals("IllegalArgumentException");
							} catch (Exception ignored) { }
							//CMN.Log("TogglePosFlag", TogglePosFlag);
							
							if(RETType.typetag==TypeTag.INT) {
								//CMN.Log("mask", mask);
								JCBinary core = maker.Binary(Tag.SR, flag, maker.Literal(flagPos));
								JCBinary basic = maker.Binary(Tag.BITAND, maker.Parens(core), maker.Literal(mask));
								JCExpression finalExpr = basic;
								if(shift!=0||max<mask) {
									if(shift!=0) {
										finalExpr = maker.Binary(Tag.PLUS, maker.Parens(basic), maker.Literal(shift));
									}
									finalExpr = maker.Binary(Tag.MOD, maker.Parens(finalExpr), maker.Literal(max));
								}
								if(elevation>0) {
									finalExpr = maker.Binary(Tag.PLUS, finalExpr, maker.Literal(annotation.elevation()));
								}
								finalExpr = maker.TypeCast(maker.TypeIdent(TypeTag.INT), finalExpr);
								CMN.Log(121,finalExpr);
								metDcl.body = maker.Block(0, List.from(new JCStatement[]{maker.Return(finalExpr)}));
							}
							else if(RETType.typetag==TypeTag.BOOLEAN) {//get boolean
								int debugVal = annotation.debug();
								int size = parms.size();
								if(size==1) {
									JCVariableDecl val=parms.get(0);
									JCPrimitiveTypeTree type = (JCPrimitiveTypeTree) val.getType();
									if(type.typetag==TypeTag.LONG||type.typetag==TypeTag.INT) {
										flag = maker.Ident(val);
									}
								}
								if(TogglePosFlag) {//toggle boolean
									CMN.Log("TogglePosFlag");
									JCExpression fetVal = maker.Binary(Tag.EQ, maker.Binary(Tag.BITAND, flag, maker.Literal(maskVal)), maker.Literal(0));
									
									Names names = Names.instance(context);
									Name valName = names.fromString("b");
									
									JCVariableDecl bEmptyVal = maker.VarDef(maker.Modifiers(0), valName, maker.TypeIdent(TypeTag.BOOLEAN), fetVal);
									JCExpression bEmptyEval = maker.Ident(valName);
									
									JCExpression core = maker.Assignop(Tag.BITAND_ASG, flag, maker.Literal(~maskVal));
									
									JCExpression FlagMaskPosPutOne = maker.Binary(Tag.SL, maker.Literal(1), maker.Literal(flagPos));
									
									FlagMaskPosPutOne = maker.Assignop(Tag.BITOR_ASG, flag, FlagMaskPosPutOne);
									
									JCExpression finalExpr = bEmptyEval;
									if(shift!=0) {//If defaul to zero, return inverted value.
										finalExpr = maker.Unary(Tag.NOT, bEmptyEval);
									}
									
									metDcl.body = maker.Block(0, List.from(new JCStatement[]{
											bEmptyVal,
											maker.Exec(core),
											maker.If(maker.Parens(bEmptyEval), maker.Exec(FlagMaskPosPutOne), null), //如果原来为空，现在不为空
											maker.Return(finalExpr)
									}));
									//CMN.Log("TogglePosFlag2", metDcl.body.toString());
								} else {
									JCBinary core = maker.Binary(Tag.BITAND, flag, maker.Literal(maskVal));
									JCExpression finalExpr = maker.Binary(shift==0?Tag.NE:Tag.EQ, maker.Parens(core), maker.Literal(0));
									if(debugVal>=0) {
										finalExpr = maker.Literal(debugVal==1);
									}
									metDcl.body = maker.Block(0, List.from(new JCStatement[]{maker.Return(finalExpr)}));
								}
							}
							else if(RETType.typetag==TypeTag.VOID) {
								int size = parms.size();
								JCVariableDecl val=null;
								if(size==1) {
									val = parms.get(0);
								} else if(size==2) {
									val = parms.get(1);
								}
								JCExpression core = maker.Binary(Tag.BITAND, flag, maker.Literal(~maskVal));
								JCExpression basic = maker.Ident(val);
								JCPrimitiveTypeTree type = (JCPrimitiveTypeTree) val.getType();
								if(type.typetag==TypeTag.BOOLEAN) {
									core = maker.Assign(flag, core);
									JCStatement[] stats = new JCStatement[2];
									stats[0]=maker.Exec(core);
									JCAssign modify = maker.Assign(flag, maker.Binary(Tag.BITOR, flag, maker.Literal(maskVal)));
									JCExpressionStatement exec = maker.Exec(modify);
									if(shift!=0) {
										basic = maker.Unary(Tag.NOT, basic);
									}
									JCIf finalExpr = maker.If(maker.Parens(basic), exec, null);
									stats[1]=finalExpr;
									metDcl.body = maker.Block(0, List.from(stats));
								}
								else if(type.typetag==TypeTag.INT) {
									JCExpression finalExpr = null;
									JCExpression var = maker.Ident(val);
									if(shift!=0||max<mask) {
										shift=-shift-elevation+max;
										finalExpr = maker.Binary(Tag.PLUS, var, maker.Literal(shift));
										finalExpr = maker.Binary(Tag.MOD, maker.Parens(finalExpr), maker.Literal(max));
									}
									finalExpr = maker.Binary(Tag.BITAND, finalExpr==null?var:maker.Parens(finalExpr), maker.Literal(mask));
									finalExpr = maker.Binary(Tag.SL, maker.Parens(finalExpr), maker.Literal(flagPos));
									finalExpr = maker.Binary(Tag.BITOR, maker.Parens(core), maker.Parens(finalExpr));
									metDcl.body = maker.Block(0, List.from(new JCStatement[]{maker.Exec(maker.Assign(flag, finalExpr))}));
									CMN.Log("111", maker.Parens(core), metDcl.body);
								}
							}
						}
						else {
							if(_1st instanceof JCIf) {
								//CMN.Log(((JCParens)((JCIf)_1st).cond).expr);
								JCStatement thenExp = ((JCIf) _1st).thenpart;
								if(thenExp instanceof JCExpressionStatement) {
									CMN.Log(((JCExpressionStatement)thenExp).expr);
								}
								if(thenExp instanceof JCBlock) {
									CMN.Log(((JCBlock)thenExp).stats.get(0));
								}
								
							}
						}
					}
					
				}


//				JCExpressionStatement statement = (JCExpressionStatement) metDcl.body.stats.get(0);
//				CMN.Log(statement);
//				CMN.Log(statement.expr);
//				CMN.Log(statement.expr.getClass());
			
			}
		}
		
		return true;
	}
}
