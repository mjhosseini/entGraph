package data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import entailment.Util;
import instr.MyAgent;

public class Test {
	
	static void testTime(){
		
		long t = 0; 
		long t0 = System.currentTimeMillis();
		int x = 0;
		for (int i=0; i<100000; i++){
			if (i%3==0){
				x++;
			}
			else{
				x--;
				x-=1;
			}
			t += (System.currentTimeMillis()-t0);
		}
		System.out.println(x);
		System.out.println(t);
	}
	
	static void splitTest(){
		String s = "hi. I'm Javad. How are you?";
		String[] ss= StringUtils.split(s,".");
		System.out.println(ss.length);
	}
	
	static void testLemma(){
		String s = "hi. I'm Javad. How have you been living these days?";
		System.out.println(Util.getLemma(s));
	}
	
	static void testInstr(){
		System.out.println(MyAgent.getObjectSize(new Integer(2)));
	}
	
	//resided in, moved to
	static void checkIntersectionGBools(String f1, String f2) throws IOException{
		HashSet<String> apairs1 = new HashSet<>();
		HashSet<String> apairs2 = new HashSet<>();
		BufferedReader br1 = new BufferedReader(new FileReader(f1));
		BufferedReader br2 = new BufferedReader(new FileReader(f2));
		
		String line = null;
		while ((line=br1.readLine())!=null){
			String[] ss = line.split("\t");
			apairs1.add(ss[0]+"#"+ss[1]);
		}
		
		while ((line=br2.readLine())!=null){
			String[] ss = line.split("\t");
			apairs2.add(ss[0]+"#"+ss[1]);
		}
		
		System.out.println("intersection");
		for (String s:apairs1){
			if (apairs2.contains(s)){
				System.out.println(s);
			}
		}
	}
	
	static void test1(){
		String x = "hi";
		String y = "hi";
		System.out.println(x==y);
	}
	
	public static void main(String[] args) throws IOException {
//		testTime();
//		splitTest();
//		testLemma();
//		testInstr();
//		checkIntersectionGBools("x1.txt", "x2.txt");
		test1();
	}
}
