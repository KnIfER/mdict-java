package test.Examples;

import com.icafe4j.image.ImageParam;
import com.icafe4j.image.ImageType;
import com.icafe4j.image.options.PNGOptions;
import com.icafe4j.image.writer.ImageWriter;
import com.knziha.plod.PlainDict.utils.JAIConverter;
import com.knziha.plod.PlainDict.utils.TiffTerminator;
import com.knziha.plod.dictionary.Utils.BU;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionary.mdictRes;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;
import com.knziha.plod.dictionaryBuilder.mdictResBuilder;
import test.CMN;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.LinkedList;

/** tiff is small but unsupported by most browsers. */
public class TiffMddConverter {
	public static void main(String[] args) throws IOException {
		mdictRes mddRaw = new mdictRes("F:\\assets\\mdicts\\汉语\\文言快易通.mdd");

		if(false){/* true false */
			mdictResBuilder builder = new mdictResBuilder("","tiff pics to png");
			/* JAIConverter or ICAFEConverter */
			TiffTerminator hey = new JAIConverter();
			long len = mddRaw.getNumberEntries();
			for (int i = 0; i < len; i++) {
				String key = mddRaw.getEntryAt(i);
				byte[] data = mddRaw.getRecordData(i);

				try {
					builder.insert(key.replace(".tif",".png"), hey.terminateTiff(data));
					CMN.Log(i, " / ", len);
				} catch (Exception e) {
					e.printStackTrace();
					builder.insert(key, data);
				}
			}
			builder.write("F:\\assets\\mdicts\\汉语\\文言快易通.converted.mdd");
		}

		if(true){/* true false */
			mdict mdxRaw = new mdict("F:\\assets\\mdicts\\汉语\\文言快易通.mdx");
			mdictBuilder builder = new mdictBuilder("文言快易通","", mdxRaw.getEncoding());
			builder.setCompressionType(0,2);
			for (int i = 0; i < mdxRaw.getNumberEntries(); i++) {
				builder.insert(mdxRaw.getEntryAt(i), mdxRaw.getRecordAt(i).replace(".tif", ".png"));
			}
			builder.write("F:\\assets\\mdicts\\汉语\\文言快易通.converted.mdx");
		}

		if(false){/* true false */
			mdict mdxRaw = new mdict("D:\\Code\\Fillin\\writemdict-master\\example_output\\no_compression.mdx");
			mdictBuilder builder = new mdictBuilder("ABOUT--ABOUT--","", mdxRaw.getEncoding());
			builder.setCompressionType(0,0);
			for (int i = 0; i < mdxRaw.getNumberEntries(); i++) {
				builder.insert(mdxRaw.getEntryAt(i), mdxRaw.getRecordAt(i));
			}
			builder.write("D:\\Code\\Fillin\\writemdict-master\\example_output\\no_compression.converted.mdx");
		}





		if(false){/* true false */
			UnpackMdd("F:\\assets\\mdicts\\汉语\\文言快易通\\", mddRaw);
		}

		if(false){/* true false */
			mdictResBuilder builder = new mdictResBuilder("","");
			File startPath = new File("F:\\assets\\mdicts\\汉语\\文言快易通\\");
			int basePathLen = startPath.getAbsolutePath().length();
			ProcessAllFiles(startPath,
					fI -> builder.insert(fI.getAbsolutePath().substring(basePathLen), fI));
			//builder.insert("\\happy.mp3",new File("F:\\Music\\虾米音乐 - Heartbreak Hotel.mp3"));
			builder.write("F:\\assets\\mdicts\\汉语\\文言快易通.converted2.mdd");
		}

		if(false){/* true false */
			File startPath = new File("F:\\assets\\mdicts\\汉语\\文言快易通\\");
			File toPath = new File("F:\\assets\\mdicts\\汉语\\文言快易通Con\\");
			int basePathLen = startPath.getAbsolutePath().length();
			TiffTerminator hey = new JAIConverter();
			ProcessAllFiles(startPath,
					fI -> {
						try {
							FileInputStream fin = new FileInputStream(fI);
							byte[] data = new byte[(int) fI.length()];
							fin.read(data);
							fin.close();
							FileOutputStream fout = new FileOutputStream(new File(toPath, fI.getAbsolutePath().substring(basePathLen)));
							fout.write(hey.terminateTiff(data));
							fout.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					});

		}
	}


	static void UnpackMdd(String startPath, mdictRes mdd) throws IOException {
		final String SepWindows = "\\";
		File spf=new File(startPath);
		spf.mkdirs();
		if(!spf.isDirectory())
			throw new FileNotFoundException(startPath);
		for (int i = 0; i < mdd.getNumberEntries(); i++) {
			String key = mdd.getEntryAt(i);
			BU.printFile(mdd.getRecordData(i), new File(startPath, key.replace(SepWindows, File.separator)).getAbsolutePath());
		}
	}

	interface ProcessAllFilesLogicLayer{
		void process(File fI);
	}

	static void ProcessAllFiles(File startPath, ProcessAllFilesLogicLayer processor) throws IOException {
		int foldeNum = 0;
		int fileNum = 0, folderNum = 0;
		if (startPath.exists()) {
			LinkedList<File> list = new LinkedList<>();
			File[] files = startPath.listFiles();
			for (File fI : files) {
				if (fI.isDirectory()) {
					list.add(fI);
					foldeNum++;
				} else {
					//处理文件
					processor.process(fI);
					fileNum++;
				}
			}
			File temp_file;
			while (!list.isEmpty()) {
				temp_file = list.removeFirst();
				files = temp_file.listFiles();
				for (File fI : files) {
					if (fI.isDirectory()) {
						list.add(fI);
						folderNum++;
					} else {
						//处理文件
						processor.process(fI);
						fileNum++;
					}
				}
			}
		} else {
			throw new FileNotFoundException(startPath.getAbsolutePath());
		}
		CMN.Log("文件夹 =", folderNum, ", 文件 =", fileNum);
	}


	@Deprecated
	public static class ICAFEConverter implements TiffTerminator {
		@Override
		public byte[] terminateTiff(byte[] data) throws Exception {
			return terminateTiff(new ByteArrayInputStream(data));
		}

		@Override
		public byte[] terminateTiff(InputStream data) throws Exception {
			BufferedImage img = javax.imageio.ImageIO.read(data);
			PNGOptions pngOptions = new PNGOptions();
			pngOptions.setApplyAdaptiveFilter(false);
			pngOptions.setCompressionLevel(9);
			ImageWriter writer = com.icafe4j.image.ImageIO.getWriter(ImageType.PNG);
			writer.setImageParam(ImageParam.getBuilder().imageOptions(pngOptions).build());
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			writer.write(img, bos);
			return bos.toByteArray();
		}
	}
}
