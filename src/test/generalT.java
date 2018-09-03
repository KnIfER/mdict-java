package test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.InflaterOutputStream;

import com.knziha.plod.dictionary.CMN;
import com.knziha.plod.dictionary.mdict;
import com.knziha.plod.dictionary.BU;
import com.knziha.plod.dictionaryBuilder.mdictBuilder;

/**
 * TEsts
 * @author KnIfER
 * @date 2018/05/31a
 */
public class generalT {
	
	
    public static void main(String[] args) throws UnsupportedEncodingException{
    	
    	ByteBuffer sf = ByteBuffer.wrap(new byte[5*8]);
    	try {
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putLong(10l);
			sf.putInt(10);
			
			sf.putLong(10l);
		} catch (BufferOverflowException e) {
			e.printStackTrace();
			CMN.show(sf.position()+"");
		}
    	CMN.show(sf.position()+"end");
    	
    	
    	Deflater df = new Deflater();
    	byte[] data = "asdfghvd".getBytes();
    	BU.printBytes(data);
    	byte[] out = new byte[1024];
    	df.setInput(data, 0, data.length);
    	df.finish();
    	int ln = df.deflate(out);//压缩进去
    	BU.printBytes(out, 0, ln);
    	byte[] de = zlib_decompress(out,0,ln);
    	BU.printBytes(de);
    	
    	BU.printBytes("哈".getBytes("GBK"));
    	
    	BU.printBytes2("哈".getBytes("GBK"));
    	
    	CMN.show(""+("A".getBytes()[0]-"a".getBytes()[0]));
    	CMN.show(""+("Ж".getBytes()[0]-"ж".getBytes()[0]));
    	
    	BU.printBytes2("Ж".getBytes("utf8"));
    	BU.printBytes2("ж".getBytes("utf8"));

    	BU.printBytes2("Ж".getBytes("utf8"));
    	BU.printBytes2("ж".getBytes("utf8"));
    	BU.printBytes2(" ".getBytes("utf8"));
    	
    	CMN.show(new String(new byte[] {(byte) 208,(byte)182,0,(byte) 208,(byte) 150},"utf8"));
    	
    	BU.printBytes2("Ж".getBytes("utf16"));
    	BU.printBytes2("ж".getBytes("utf16"));
    	BU.printBytes2(" ".getBytes("utf16"));
    	String utf16str = new String(new byte[] {(byte)254,(byte)255,4,22,0,0,(byte)254,(byte)255,4,54,},"utf16");
    	CMN.show(utf16str.toLowerCase()+":"+utf16str.toLowerCase().indexOf(new String(new byte[] {0,0},"utf16")));
    	BU.printBytes2(utf16str.getBytes("utf16"));
    	
    	
    	BU.printBytes2("别树一帜".getBytes("UTF-16"));
    	BU.printBytes2("别树一帜".getBytes("UTF-16LE"));
    	BU.printBytes2("h".getBytes("UTF-16LE"));
    	
    	CMN.show(""+"\\js\\aa".compareTo("\\js\\b"));

    	
    	CMN.show(""+"\\js\\renderers\\CanvasRenderer.js".compareTo("\\js\\Three.js"));
    	CMN.show(""+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	
    	
    	CMN.show("asdasdasd"+"\\js\\renderers\\Projector.js".compareTo("\\js\\Three.js"));
    	
    	
    	String testData = "hello world word haWa垃圾 katsuki naku 和 乐 君 卿 寝室是真实的";
    	byte[] testDataArr = testData.getBytes(_encoding);
    	String key = "hellO".toLowerCase();
    	byte[][][] matcher = new byte[2][][];
    	matcher[0] = SanLieZhi(key);
    	String upperKey = key.toUpperCase();
    	if(!upperKey.equals(key))
    		matcher[1] = SanLieZhi(upperKey);
		for(byte[] xxx:matcher[0])
    		BU.printBytes(xxx);
		BU.printBytes(key.getBytes(_encoding));
    	
		CMN.show("bingStartWith"+bingStartWith(testDataArr,0,"haWa".getBytes(_encoding),0,-1,0));
    
		CMN.show("EntryStartWith"+EntryStartWith(testDataArr, 0, testDataArr.length, matcher));

		
	CMN.show("bbb"+binary_find_closest(new int[] {1,2,3,4,5,6,7,8,9,10,11,12,14,14},14,-1));
	
		String Fuzzykey = "haWa";
		//CMN.show("Fuzzykey"+EntryStartWith(testDataArr, 0, testDataArr.length, matcher));
		
		
		BU.printBytes("guppy".getBytes("utf8"));
		BU.printBytes("h".getBytes("utf8"));
		
		BU.printBytes("简".getBytes("utf8"));
		BU.printBytes("极简".getBytes("utf8"));
		try {
			mdict m = new mdict("H:\\antiquafortuna\\MDictPC\\doc\\简明英汉汉英词典.mdx");
			m.flowerFindAllContents("简", 0, 0);
			//BU.printBytes(m.getRecordAt(m.lookUp("简")).getBytes(s));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String i=null;
		
		
		CMN.show(new  StringBuilder().append(i).toString());
		
		
		BU.printBytes("<div class=titleNote><span class=comment>《下武》，继文也。武王有圣德，复受天命，能昭先人之功焉。</span></div><div class=content><p>下武</a>维周，世有哲王。<!---->三后</a>在天，王配于京。<span class=charType>（一章）</span></p><div class=small></div><p>王配于京，世德</a>作求</a>。<!---->永言配命</a>，成王之孚</a>。<span class=charType>（二章）</span></p><div class=small></div><p>成王之孚</a>，下土</a>之式。<!---->永言孝思</a>，孝思</a>维则。<span class=charType>（三章）</span></p><div class=small></div><p>媚</a>兹一人</a>，应侯顺德。<!---->永言孝思</a>，昭哉嗣服</a>。<span class=charType>（四章）</span></p><div class=small></div><p>昭兹来许</a>，绳其祖武</a>。<!---->于万斯年，受</a>天之祜。<span class=charType>（五章）</span></p><div class=small></div><p>受</a>天之祜，四方</a>来贺</a>。<!---->于万斯年，不遐有佐。<span class=charType>（六章）</span></p><div class=small></div></div><div id=comment_351 class=comment><span class=label>《毛诗注疏》</span>：<br><div class=pageContent><p>《<span class=book>下武</span>》，<span class=example1>继文也。</span><span class=example1>武王有圣德，</span><span class=example1>复受天命，</span><span class=example1>能昭先人之功焉。</span>继文者，继文王之王业而成之。昭，明也。<span class=label>○</span>复，扶又反。王业，于况反。</p><p><span class=label><span class=highlighted1>[疏]</span></span>“《<span class=book>下武</span>》六章，章四句”至“功焉”。<span class=label>○</span>正义曰：经六章，皆言武王益有明智，配先人之道，成其孝思，继嗣祖考之迹，皆是继文能昭先人之功焉。经云“<span class=example1>三后在天，</span><span class=example1>王配于京”</span>，则武王所继，自大王、王季皆是矣。而序独云“继文”者，作者以周道积基，故本之于三后，言“<span class=example1>世有哲王”</span>，见积德之深远，其实美武王能继，唯在文王也。大王、王季虽脩德创业，为后世所因，而未有天命，非开基之主，不足使武王圣人继之。又此篇在《<span class=book>文王</span>》诗后，故诗言“继文”，著其功之大，且见篇之次也。文王已受天命，故言“复受”，为亚前之辞。武王之受天命，白鱼入舟是也。</p><p><span class=example1>下武维周，</span><span class=example1>世有哲王。</span>武，继也。笺云：下，犹后也。哲，知也。后人能继先祖者，维有周家最大，世世益有明知之王，谓大王、王季、文王稍就盛也。<span class=label>○</span>哲，张列反，本又作“悊”，又作“哲”，皆同。知音智。下同。</p><p><span class=label><span class=highlighted1>[疏]</span></span>传“武，继”。<span class=label>○</span>正义曰：《<span class=book>释诂</span>》文。<span class=label>○</span>笺“下犹”至“就盛”。<span class=label>○</span>正义曰：居下世，即是在后，故云“下，犹后也”。“哲，智”，《<span class=book>释言</span>》文。言后人能继祖者，维周家最大，谓大王、王季、文王稍稍就盛者也。王季为西伯，文王又受命，是稍盛也。不通数武王者，此言哲王，即是下文“三后”、“王配”之文，别在于下，故知世有之中，不兼武王也。</p><p><span class=example1>三后在天，</span><span class=example1>王配于京。</span>三后，大王、王季、文王也。王，武王也。笺云：此三后既没登遐，精气在天矣。武王又能配行其道于京，谓镐京也。<span class=label>○</span>假音遐，已也。本或作“遐”。</p><p><span class=label><span class=highlighted1>[疏]</span></span>笺“此三后”至“镐京”。<span class=label>○</span>正义曰：《<span class=book>曲礼下</span>》云：“天子崩，告丧曰：‘天王登遐。’”注云：“登，上也。遐，已也。”上已者，若仙去云耳。以三后皆号为王，故以天子之礼言之。武王居镐，故知配行其道于京，谓镐京也。</p><p><span class=example1>王配于京，</span><span class=example1>世德作求。</span>笺云：作，为。求，终也。武王配行三后之道于镐京者，以其世世积德，庶为终成其大功。</p><p><span class=label><span class=highlighted1>[疏]</span></span>笺“作为”至“大功”。<span class=label>○</span>正义曰：“作，为”，《<span class=book>释言</span>》文。“求，终”，《<span class=book>释诂</span>》文。世积厚德，是当王天下。文王未及诛纣，即是王事未终。武王乃终之，故云终成其大功。</p><p><span class=example1>永言配命，</span><span class=example1>成王之孚。</span>笺云：永，长。言，我也。命，犹教令也。孚，信也。此为武王言也。今长我之配行三后之教令者，欲成我周家王道之信也。王德之道成于信，《<span class=book>论语</span>》曰：“民无信不立。”<span class=label>○</span>成王，如字，又于况反。此为如字。</p><p><span class=label><span class=highlighted1>[疏]</span></span>笺“命犹”至“不立”。<span class=label>○</span>正义曰：此承“<span class=example1>王配于京”</span>，是配三后，不配天，故以命为教令。此篇是武王之诗，于此独云“此为武王言”者，馀文是作者以已之心论武王之事，此则称武王口自所言，故辨之也。又解欲成王道，所为多矣，独以信为言者，由王德之道成于信，欲使民信王道，然后天下顺从，必伐纣，功成然始得耳。以民无信不立，故引《<span class=book>论语</span>》以證之。</p><p><span class=example1>成王之孚，</span><span class=example1>下土之式。</span>式，法也。笺云：王道尚信，则天下以为法，勤行之。<span class=example1>永言孝思，</span><span class=example1>孝思维则。</span>则其先人也。笺云：长我孝心之所思。所思者，其维则三后之所行。子孙以顺祖考为孝。</p><p><span class=example1>媚兹一人，</span><span class=example1>应侯顺德。</span>一人，天子也。应，当。侯，维也。笺云：媚，爱。兹，此也。可爱乎武王，能当此顺德。谓能成其祖考之功也。《<span class=book>易</span>》曰：“君子以顺德，积小以高大。”</p><p><span class=example1>永言孝思，</span><span class=example1>昭哉嗣服。</span>笺云：服，事也。明哉，武王之嗣行祖考之事。谓伐纣定天下。</p><p><span class=label><span class=highlighted1>[疏]</span></span>“媚兹”至“嗣服”。<span class=label>○</span>正义曰：既言武王能法则三后之道，故于此叹而美之。可爱乎，此一人之武王。所以可爱者，以其能当此维顺之德。祖考欲定天下，武王能顺而定之，是能当顺德。又述武王所言而叹美之。武王自言，长我孝心之所思者，此事显明哉。武王实能嗣行祖考之事，伐纣定天下，是能嗣祖考也。<span class=label>○</span>传“一人”至“侯维”。<span class=label>○</span>正义曰：《<span class=book>曲礼下</span>》云：“天子自称曰予一人。”言其天下之贵，唯一人而已，谓天子为一人。“应，当”，《<span class=book>释诂</span>》文。又云：“维，侯也。”是侯得为维也。<span class=label>○</span>笺“可爱”至“高大”。<span class=label>○</span>正义曰：序言“继文”，此云“顺德”，故知是顺其先人之心，成其祖考之德。所引《<span class=book>易</span>》者，《<span class=book>升卦·象辞</span>》。升卦巽下坤上，故言木生地中。木渐而顺长以成树，犹人顺德以成功。彼谓一人之身，渐积以成，此则顺父祖而成事，亦相类，故引以为證。定本作“慎德”。准约此诗上下及《<span class=book>易</span>》，宜为顺字。又《<span class=book>集注</span>》亦作“顺”，疑定本误。<span class=label>○</span>笺“服事”至“天下”。<span class=label>○</span>正曰：“服，事”，《<span class=book>释诂</span>》文。《<span class=book>礼记·大传</span>》曰：“牧之野，武王之大事。”故知嗣行祖考之事，唯谓伐纣定天下也。上言“<span class=example1>永言配命”</span>、“<span class=example1>永言孝思”</span>，其下句云“<span class=example1>成王之孚”</span>、“<span class=example1>孝思维则”</span>，亦是武王自言。此云“<span class=example1>昭哉嗣服”</span>，是作者美武王之辞，所以亦与“孝思”相连者，上云“<span class=example1>永言孝思”</span>，是武王自言，此又述武王之言，叹而美之，并此“孝思”之句，亦非武王自言，得与嗣服相连也。</p><p><span class=example1>昭兹来许，</span><span class=example1>绳其祖武。</span>许，进。绳，戒。武，迹也。笺云：兹，此。来，勤也。武王能明此勤行，进于善道，戒慎其祖考所履践之迹，美其终成之。<span class=label>○</span>来，王如字，郑音赉。下篇“来孝”同。</p><p><span class=example1>于万斯年，</span><span class=example1>受天之祜。</span>笺云：祜，福也。天下乐仰武王之德，欲其寿考之言也。<span class=label>○</span>祜音户。下同。</p><p><span class=label><span class=highlighted1>[疏]</span></span>“昭兹”至“之祜”。<span class=label>○</span>正义曰：既言武王能嗣行祖事，又美其为民所乐仰。言武王能明此勤行，进于善道，戒慎其祖考所行之迹而践行之，犹行善不倦，故为天下乐仰，皆欲令武王得于万年之寿，且又多受天之福禄。言武王行善之故，为民爱之如此。<span class=label>○</span>传“许进”至“武迹”。<span class=label>○</span>正义曰：以礼法既许，而后得进，故以许为进。“绳，戒。武，迹”，皆《<span class=book>释训</span>》文。<span class=label>○</span>笺“兹此”至“成之”。<span class=label>○</span>正义曰：“兹，此。来，勤”，皆《<span class=book>释诂</span>》文。戒慎祖考践履之迹，谓谨慎奉行，故美其终成之。<span class=label>○</span>笺“祜福”至“之言”。<span class=label>○</span>正义曰：“祜，福”，《<span class=book>释诂</span>》文。以万年受福，是祝庆之辞，故知武王为天下所乐仰，此是欲其得福之言也。</p><p><span class=example1>受天之祜，</span><span class=example1>四方来贺。</span><span class=example1>于万斯年，</span><span class=example1>不遐有佐！</span>远夷来佐也。笺云：武王受此万年之寿，不远有佐。言其辅佐之臣，亦宜蒙其馀福也。《<span class=book>书</span>》曰“公其以子万亿年”，亦君臣同福禄也。</p><p><span class=label><span class=highlighted1>[疏]</span></span>“受天”至“有佐”。<span class=label>○</span>毛以为，民欲王受福，即实言其受福之事。武王既受得天之祜福，故四方诸侯之国皆贡献庆之。又得于此万年之寿，岂不远有佐助之乎！言有远方夷狄来佐助之也。此乘上章之文，故先言所受天之祜，因则为远近之次，故先言四方，后言远夷。四方，谓中国诸侯也。<span class=label>○</span>郑唯以下句为异。言武王得于此万年之寿，不远其有辅佐之臣。言王亲近其臣，与之同福。<span class=label>○</span>传“远夷来佐”。<span class=label>○</span>正义曰：言不远有佐，是远有佐。远人佐天子，唯夷狄耳，故知远夷来佐之。《<span class=book>书叙</span>》言：“武王既胜殷，西旅献獒，巢伯来朝。”《<span class=book>鲁语</span>》曰：“武王克商，遂通道于九夷八蛮，萧慎来贺。”是远夷来佐之事。“<span class=example1>不遐有佐”</span>为远夷，则“<span class=example1>四方来贺”</span>为诸夏。《<span class=book>民劳</span>》传曰：“四方，诸夏。”是也。<span class=label>○</span>笺“武王”至“福禄”。<span class=label>○</span>正义曰：笺以“<span class=example1>不遐有佐”</span>顺文自通，不当反其言，故易之。武王既有万年之寿，不远有辅佐之臣，共蒙其福。其封为诸侯，则与周升降；其仕于王朝，则继世在位，是其不与远之。引《<span class=book>书</span>》曰“公其以予万亿年”者，《<span class=book>洛诰</span>》文。成王告周公，言公与我身，皆得万亿之年。既引其文，乃申其意，言彼亦君臣同福禄，故知此亦武王君臣同受福矣。</p><p>《<span class=book>下武</span>》六章，章四句。</p></div><span class=label>《诗经通论》</span>：<br><div class=pageContent><p>下武</p><p><span class=example1>下武维周，世有哲王。</span><span class=emphase1>三后在天</span><span class=example1>，王配于京。</span><span class=label><span class=small>本韵。</span>○赋也。下同。</span><span class=emphase1>王配于京</span><span class=example1>，世德作求。永言配命，成王之孚。</span><span class=label><span class=small>本韵。</span></span><span class=emphase1>成王之孚</span><span class=example1>，下土之式。永言孝思，孝思维则。</span><span class=label><span class=small>本韵。</span></span><span class=example1>媚兹一人，应侯顺德。</span><span class=emphase1>永言孝思</span><span class=example1>，</span><span class=label><span class=highlighted1>[评]</span>第三句应上第三句变。</span><span class=example1>昭哉嗣服。</span><span class=label><span class=small>本韵。</span></span><span class=emphase1>昭兹来许</span><span class=example1>，绳其祖武。于万斯年，受天之祜。</span><span class=label><span class=small>本韵。</span></span><span class=emphase1>受天之祜</span><span class=example1>，四方来贺。于万斯年，不遐有佐。</span><span class=label><span class=small>本韵。</span></span></p><p>小序谓「继文」，是，盖咏武王也。<br>[一章]「下」，后也；「武」，继也，迹也，即下「绳其祖武」之武。谓下世而能步武乎前人者维周也，以其世世有哲王也。传、笺解此亦皆明。集传忽云「『下』义未详；或曰<span class=rhymeComment>「曰」，原误作「亦」，今校改。</span>『字当作「文」』」。无论诗多拗字，处处皆然。且下言「三后」中有文王，岂有下言「三后」而上又言文、武者乎？此不通于文义也。其云「『下』义未详」，吾患其「武」义未详，岂止「下」义而已！此不通于字义也。自集传为此猜疑之说，故严氏因为之解曰「武王之心上文不上武」。嗟乎，既不上武，何以谥为武，而其乐亦名武乎？武王取天下以武，故谥以武，乐亦名武，初未尝讳也。下篇云，「文王受命，有此武功」。文王伐崇伐密且以武名，况武王乎！伪传、说又因以「下」作「大」，尤谬。此皆「『下』义未详」之说害之，故如此。<br>[五章]「绳其祖武」，兼祖、考言。集传曰「或疑此诗有『成王』字，当为康王以后之诗；然考寻文义」云云。按，此等不通稚论直当远屏，不必载之篇简。乃有鲰生者拾其所吐弃，方奉为至宝，又不足嗤已！<br>【下武六章，章四句。】</p></div></div>\n".getBytes("utf8"));
		BU.printBytes("七".getBytes("utf8"));
		
		
		BU.printBytes("七".getBytes("utf8"));
		BU.printBytes("七".getBytes());
    }
    
    

    public static int  binary_find_closest(int[] array,int val,int iLen){
    	int middle = 0;
    	if(iLen==-1||iLen>array.length)
    		iLen = array.length;
    	int low=0,high=iLen-1;
    	if(array==null || iLen<1){
    		return -1;
    	}
    	if(iLen==1){
    		return 0;
    	}
    	if(val-array[0]<=0){
			return 0;
    	}else if(val-array[iLen-1]>0){
    		return iLen-1;
    	}
    	int counter=0;
    	long cprRes1,cprRes0;
    	while(low<high){
    		//CMN.show(low+"~"+high);
    		counter+=1;
    		System.out.println(low+":"+high);
    		middle = (low+high)/2;
    		cprRes1=array[middle+1]-val;
        	cprRes0=array[middle  ]-val;
        	if(cprRes0>=0){
        		high=middle;
        	}else if(cprRes1<=0){
        		//System.out.println("cprRes1<=0 && cprRes0<0");
        		//System.out.println(houXuan1);
        		//System.out.println(houXuan0);
        		low=middle+1;
        	}else{
        		//System.out.println("asd");
        		//high=middle;
        		low=middle+1;//here
        	}
    	}
		return low;
    }
    
    
    //static String _encoding = "UTF-16";//fe,ff开头
    static String _encoding = "UTF-16LE";//无开头
    //static String _encoding = "GBK";//无开头
    
    private static byte[][] SanLieZhi(String str) throws UnsupportedEncodingException {
    	byte[][] res = new byte[str.length()][];
    	for(int i=0;i<str.length();i++){
    		String c = str.substring(i, i+1);
    		res[i] = c.getBytes(_encoding);
		}
		return res;
	}
    
    static boolean EntryStartWith(byte[] source, int sourceOffset, int sourceCount, byte[][][] matchers) {
		boolean Matched = false;
		int fromIndex=0;
		CMN.show("matching!");
    	for(int lexiPartIdx=0;lexiPartIdx<matchers[0].length;lexiPartIdx++) {
    		Matched = false;
    		for(byte[][] marchLet:matchers) {
    			if(marchLet==null) break;
    			if(bingStartWith(source,sourceOffset,marchLet[lexiPartIdx],0,-1,fromIndex)) {
    				Matched=true;
    			}
    		}
    		if(!Matched)
    			return false;
    		fromIndex+=matchers[0][lexiPartIdx].length;
    	}
    	return true;
    }


    static boolean bingStartWith(byte[] source, int sourceOffset,byte[] target, int targetOffset, int targetCount, int fromIndex) {
    	if (fromIndex >= source.length) {
    		return false;
        }
    	if(targetCount<=-1)
    		targetCount=target.length;
    	if(sourceOffset+targetCount>=source.length)
        	return false;
    	for (int i = sourceOffset + fromIndex; i <= sourceOffset+fromIndex+targetCount-1; i++) {
    		if (source[i] != target[targetOffset+i-sourceOffset-fromIndex]) 
    			return false;
    	}
    	return true;
    }


    static int bingIndexOf(byte[] source, int sourceOffset, int sourceCount, byte[][][] matcher) {
    	for(byte[][] marcherLet:matcher) {
    		
    		
    	}
    	return -1;
    }
    /*
     * https://stackoverflow.com/questions/21341027/find-indexof-a-byte-array-within-another-byte-array
     * Gustavo Mendoza's Answer*/
    static int indexOf(byte[] source, int sourceOffset, int sourceCount, byte[] target, int targetOffset, int targetCount, int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first)
                    ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++)
                    ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }

    
	public static byte[] zlib_decompress(byte[] encdata,int offset,int ln) {
	    try {
			    ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			    InflaterOutputStream inf = new InflaterOutputStream(out); 
			    inf.write(encdata,offset, ln); 
			    inf.close(); 
			    return out.toByteArray(); 
		    } catch (Exception ex) {
		    	ex.printStackTrace(); 
		    	return "ERR".getBytes(); 
		    }
    }
    

    
}


