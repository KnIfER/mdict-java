# mdict-parsr-java
.now a library!  
.now supports lzo  
.no encryption other than ripemd128 key-info encryption.  

# 一、mdx parse && search
![image](https://github.com/KnIfER/mdict-parsr-java/raw/master/screenshot/Screenshot1.png)
in above example image,it takes 5.8ms to query/locate an key entry on average ,  
and in total it takes 6ms to query/locate an entry and then extract it's contents.

# 二、mdd parse && search
![image](https://github.com/KnIfER/mdict-parsr-java/raw/master/screenshot/Screenshot2.png)
in above example image,it takes 24ms to query/locate an entry and then extract it's contents.  
in better cases,it takes only 6~8ms.  

# 三、details
* use red-black tree and binary searching list to implement dict funcitons.  
* credits to wang's mdict python parser and Feng Dihai(@fengdh).  

# enjoy!~
