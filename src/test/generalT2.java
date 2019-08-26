package test;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.knziha.rbtree.ParralelListTree;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * 2019
 * @author KnIfER
 * @date 2019/05/16
 */
public class generalT2 {
	
	

    public static void main(String[] args){
    	ParralelListTree<Long,Integer> subtitle = new ParralelListTree<>(12);

    	subtitle.insert((long) 0, 100);
    	subtitle.insert((long) 18, 100);
    	subtitle.insert((long) 2, 100);
    	subtitle.insert((long) 1, 100);
    	subtitle.insert((long) 28, 100);
    	subtitle.insert((long) 8, 100);
    	subtitle.insert((long) 2, 100);
    	subtitle.insert((long) -1, 100);

    	//Arrays.binarySearch(subtitle.data.toArray(), 12);
    	ArrayList<Integer> data = new ArrayList<>(12);
    	//data.get(-1);
    	
		System.out.println(subtitle.data);
    	if(false) {
			System.out.println(binarySearch(subtitle.data,0,subtitle.data.size(),Long.valueOf(-1)));
			System.out.println(binarySearch(subtitle.data,0,subtitle.data.size(),Long.valueOf(0)));
			System.out.println(binarySearch(subtitle.data,0,subtitle.data.size(), Long.valueOf(18)));;
			System.out.println(binarySearch(subtitle.data,0,subtitle.data.size(),Long.valueOf(19l)));
			System.out.println(binarySearch(subtitle.data,0,subtitle.data.size(),Long.valueOf(29l)));
    	}

		System.out.println("?: "+subtitle.lookUpKey(-1l,false));
		System.out.println("?: "+subtitle.lookUpKey(1l,false));
		System.out.println("?: "+subtitle.lookUpKey(3l,false));
		
		int val=(int) (2+1)%3+2;
		CMN.Log(val);
		CMN.Log((val)%3);



    }

    
	@SuppressWarnings("rawtypes")
    private static int binarySearch(ArrayList a, int fromIndex, int toIndex,Object key) {
			int low = fromIndex;
			int high = toIndex - 1;
			
			while (low <= high) {
			int mid = (low + high) >>> 1;
			Comparable midVal = (Comparable)a.get(mid);
			@SuppressWarnings("unchecked")
			int cmp = midVal.compareTo(key);
			
			if (cmp < 0)
				low = mid + 1;
				else if (cmp > 0)
				high = mid - 1;
				else
				return mid; // key found
			}
			return -(low);  // key not found.
	}
		    
    
}


