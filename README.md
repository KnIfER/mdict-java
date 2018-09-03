# MDict Library in pure java ！
It supports:
   I.lzo(via lzo-core-1.0.5.jar)
  II.Ripemd128 key-info encryption.
 III.Mdx add Mdd Builders.

and is able to do:
   I.Basic query.
  II.Conjuction search.
 III.Fast Fuzzy search (with wild cards).
  IV.Fast Fulltext retrieval (with wild cards).

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



# details
* This project was initially converted from xiaoqiangWang's [python analyzer](https://bitbucket.org/xwang/mdict-analysis). 
* Use [red-black tree](http://www.cnblogs.com/skywang12345/p/3245399.html) and binary-list-searching(mainly) to implement dict funcitons.  
* Feng Dihai(@[fengdh](https://github.com/fengdh/mdict-js))'s mdict-js is of help too, I just switch to use the same elegant binary list searching mathod——reduce().Somehow, this function will always returns the first occurance of the entry >= keyword, maybe some mathematician could told me why, but I've tested over 100000 times without any expectation.
```
/*via mdict-js
 *note at first time we feed in 0 as start and array.length as end. it's must not be array.length-1. 
*/
public static int reduce(int phrase, int[] array,int start,int end) {
	int len = end-start;
	if (len > 1) {
	  len = len >> 1;
	  return phrase > array[start + len - 1]
				? reduce(phrase,array,start+len,end)
				: reduce(phrase,array,start,start+len);
	} else {
	  return start;
	}
}
```
	

<img src="https://github.com/KnIfER/mdict-parsr-java/raw/master/doc/MDX.svg">

# an android demo,based on [anki-helper](https://github.com/mmjang/ankihelper),is under development
