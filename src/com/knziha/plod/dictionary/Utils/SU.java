/*  Copyright 2018 KnIfER Zenjio-Kang

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at
	
	    http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
	
	Mdict-Java Query Library
*/

package com.knziha.plod.dictionary.Utils;

//StringUtils
public class  SU{
    public static boolean debug;

	public static String trimStart(String input) {
		int len = input.length();
        int st = 0;

        while ((st < len) && (input.charAt(st) <= ' ')) {
            st++;
        }
        
        return ((st > 0) || (len < input.length())) ? input.substring(st, len) : input;
    }
	
    public static int compareTo(String strA,String strB,int start, int lim) {
        int len1 = strA.length();
        int len2 = strB.length();
        int _lim = Math.min(Math.min(len1-start, len2-start),lim);

        int k = 0;
        while (k < _lim) {
            char c1 = strA.charAt(k+start);
            char c2 = strB.charAt(k+start);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return _lim==lim?0:len1 - len2;
    }
	
}
	


