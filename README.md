# MDict Library in java 
It supports lzo,and no encryption other than ripemd128 key-info encryption has been imple..ed now.  
The Builder is ready to use now. An example has been uploaded in the folder /src/test/.  

# Usage:
### 1.Basic query:
```
String key = "happy";
mdict md = new mdict(path);
int search_result = md.lookUp(key);
if(search_result!=-1){
  String html_contents = md.getRecordAt(search_result);
  String entry_name_at_pos = md.getEntryAt(search_result);
  //TODO handle html_contents and entry_name_at_pos
}
```
### 2.Search in a bunch of dicts:
```
String key = "happy";
ArrayList<mdict> mdxs = new ArrayList<mdict>();
mdxs.add(path1);
mdxs.add(path2);
RBTree_additive combining_search_tree = new RBTree_additive();
for(int i=0;i<mdxs.size();i++)
{
  mdxs.get(i).size_confined_lookUp(key,combining_search_tree,i,30);
}  	
combining_search_tree.inOrder();//print results stored in the RBTree

/*printed results looks like 【happy____@398825@0@16905@1】...【other results】...
how to handle:
String html_contents0 = mdxs.get(0).getRecordAt(398825),
html_contents1 = mdxs.get(1).getRecordAt(16905);
*/
```

# Sample Console output:

```
——————————————————————Dict Info——————————————————————
|CreationDate:2011-8-12
|Description:&lt;html&gt;&lt;h1 align=&quot;center&quot;&gt;&lt;font size=7 color=red&gt;简明英汉汉英词典&lt;/font&gt;&lt;/h1&gt;&lt;p&gt;
&lt;h1 align=&quot;center&quot;&gt;&lt;font size=3 color=red&gt;Latest update:Aug,10,2011&lt;/font&gt;&lt;/h1&gt;
&lt;p&gt;&lt;h1 align=&quot;center&quot;&gt;&lt;font size=3 color=red&gt;1155424 entries&lt;/font&gt;&lt;/h1&gt;&lt;p&gt;
&lt;h1 align=&quot;center&quot;&gt;&lt;font size=3 color=blue&gt;&lt;/font&gt;&lt;/h1&gt;&lt;h1 align=&quot;center&quot;&gt;&lt;font size=3 color=blue&gt;&lt;em&gt;Converted by Superfan&lt;/em&gt;&lt;/font&gt;&lt;/h1&gt;&lt;/html&gt;      
|Encrypted:2
|Compat:Yes
|KeyCaseSensitive:No
|Title:简明英汉汉英词典
|Encoding:UTF-8
|Left2Right:Yes
|Format:Html
|StyleSheet:
|GeneratedByEngineVersion:2.0
|RequiredEngineVersion:2.0
|Compact:Yes
|DataSourceFormat:106
|StripKey:Yes
|编码: UTF-8
|_num_entries: 1155424
|_num_key_blocks: 877
|_num_record_blocks: 6755
|maxComRecSize: 10090
|maxDecompressedSize: 65536
——————————————————————Info of Dict ——————————————————————



——————————————————————basic query——————————————————————
结果html contents of happy ： <link rel="stylesheet" type="text/css" href="sf_cb.css"/>
<span class="CK"><span class="DC">happy</span><span class="JS"><span class="CY"><span class="CX"><span class="YX">happy</span><span class="YB"><span class="CB">D.J.:[ˈhæpi]</span></span><span class="YB"><span class="CB">K.K.:[ˈhæpi]</span></span><span class="DX">adj.</span><span class="JX"><span class="entryNum">1.</span><span class="entryDot">■</span>幸福的, 愉快的, 高兴的</span><span class="LJ"><span class="LY">We had a happy reunion after many years.</span><span class="LS">我们在分别多年之后又愉快地团聚在一起。</span></span><span class="LJ"><span class="LY">She was happy sitting among her students.</span><span class="LS">她坐在学生中间很高兴。</span></span><span class="LJ"><span class="LY">I am so happy that you could visit us.</span><span class="LS">你能来看望我们, 我真高兴。</span></span><span class="JX"><span class="entryNum">2.</span><span class="entryDot">■</span>对…感到满意的; 认为…是对的[好的]</span><span class="LJ"><span class="LY">Are you happy with his work?</span><span class="LS">你对他的工作满意吗?</span></span><span class="JX"><span class="entryNum">3.</span><span class="entryDot">■</span>乐意的, 没有困难的</span><span class="LJ"><span class="LY">He is happy in doing good.</span><span class="LS">他乐于行善。</span></span><span class="LJ"><span class="LY">We'll be happy to help if you need us.</span><span class="LS">如果你需要的话, 我们将乐意帮助。</span></span><span class="JX"><span class="entryNum">4.</span><span class="entryDot">■</span>幸运的, 运气好的</span></span></span></span></span>


——————————————————————searching in a bunch of dicts——————————————————————
时耗 Time used： 32ms
联合搜索结果 results： 
【happy____@398825@0@16905@1】
【happy anniversary____@398826@0】
【happy as a clam____@398827@0】
【happy as the day is long____@398828@0】
【happy birthday____@398829@0】
【happy-clappy____@16906@1】
【happy dispatch____@398830@0】
【happy event____@398831@0】
【happy families____@16907@1】
【happy family____@398832@0】
【happy-go-lucky____@398833@0@16908@1】
【happy hour____@398834@0@16909@1】
【happy hunting ground____@398835@0】
【happy medium____@398836@0】
【happy time____@398837@0】
```

# details
* This project was initially converted from xiaoqiangWang's [python analyzer](https://bitbucket.org/xwang/mdict-analysis). 
* Use [red-black tree](http://www.cnblogs.com/skywang12345/p/3245399.html) and binary searching list to implement dict funcitons.  
* Thanks to Feng Dihai(@[fengdh](https://github.com/fengdh/mdict-js)),although I cant read tough javascript,his help have encouraged me.  

<img src="https://github.com/KnIfER/mdict-parsr-java/raw/master/doc/MDX.svg">

# an android demo,based on [anki-helper](https://github.com/mmjang/ankihelper),is under development
