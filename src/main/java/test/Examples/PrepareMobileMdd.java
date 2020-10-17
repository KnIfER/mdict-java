package test.Examples;

import com.knziha.plod.dictionaryBuilder.mdictResBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import static test.Examples.TiffMddConverter.ProcessAllFiles;

public class PrepareMobileMdd {


	public static void main(String[] args) throws IOException {



		HashSet<String> exemption = new HashSet<>();
		exemption.add(".idea");
		exemption.add("README.md");
		
		mdictResBuilder builder = new mdictResBuilder("", "");
		String InputPath = "D:\\Code\\tests\\recover_wrkst\\mdict-java\\src\\main\\java\\com\\knziha\\plod\\PlainDict\\Mdict-browser";
		int basePathLen = InputPath.length();
		File startPath = new File(InputPath);
		ProcessAllFiles(startPath, exemption,
				fI -> {
					if (!exemption.contains(fI.getAbsolutePath().substring(basePathLen + 1)))
						builder.insert(fI.getAbsolutePath().substring(basePathLen), fI);
				});
		builder.setCompressionType(2);
		builder.write("D:\\MdbR.mdd");
		
		
		
	}
	
	
	
	
	
	
	
	
}
