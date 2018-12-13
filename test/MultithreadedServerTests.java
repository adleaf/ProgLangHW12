package hw12.test;

import hw12.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;
import static org.junit.Assert.assertTrue;

import junit.framework.TestCase;

import org.junit.Test;

public class MultithreadedServerTests extends TestCase {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;
            
    protected static void dumpAccounts() {
	    // output values:
	    for (int i = A; i <= Z; i++) {
	       System.out.print("    ");
	       if (i < 10) System.out.print("0");
	       System.out.print(i + " ");
	       System.out.print(new Character((char) (i + 'A')) + ": ");
	       accounts[i].print();
	       System.out.print(" (");
	       accounts[i].printMod();
	       System.out.print(")\n");
	    }
	 }    
     
        
     @Test
	 public void testIncrement() throws IOException {
	
		// initialize accounts 
		accounts = new Account[numLetters];
		for (int i = A; i <= Z; i++) {
			accounts[i] = new Account(Z-i);
		}			 
		
		MultithreadedServer.runServer("src/hw12/data/increment", accounts);
			
		// assert correct account values
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
		}		

	 }  
 
    
     @Test
     public void testStar() throws IOException {
    	// initialize accounts 
 		accounts = new Account[numLetters];
 		for (int i = A; i <= Z; i++) {
 			accounts[i] = new Account(Z-i);
 		}
 		
 		MultithreadedServer.runServer("src/hw12/data/starTest", accounts);		
		dumpAccounts();
 		assertEquals("Account C differs",24,accounts[2].getValue());
 		assertEquals("Account A differs",1,accounts[0].getValue());
 		     }

     
     @Test
     public void testChange() throws IOException {
    	 //This test also tests to avoid deadlock in concurrent version
    	 
    	// initialize accounts 
 		accounts = new Account[numLetters];
 		for (int i = A; i <= Z; i++) {
 			accounts[i] = new Account(Z-i);
 		}
 		
 		MultithreadedServer.runServer("src/hw12/data/testChanges", accounts);
		
 		//assertEquals("Account A differs",(26),accounts[0].getValue());
 		assertTrue(accounts[0].getValue() == 26 || accounts[0].getValue() == 28);
 		//assertEquals("Account B differs",27,accounts[1].getValue());
 		assertTrue(accounts[1].getValue() == 27 || accounts[1].getValue() == 26);
 		//assertEquals("Account C differs",53,accounts[2].getValue());
 		assertTrue(accounts[2].getValue() == 53 || accounts[2].getValue() == 50 || accounts[2].getValue() == 54);
 		
     }
  
 
     @Test
     public void testSubtract() throws IOException {    	 
    	// initialize accounts 
 		accounts = new Account[numLetters];
 		for (int i = A; i <= Z; i++) {
 			accounts[i] = new Account(Z-i);
 		}
 		
 		MultithreadedServer.runServer("src/hw12/data/testSubtract", accounts);
		
 		assertEquals("Account A differs",-1,accounts[0].getValue());
 		assertEquals("Account B differs",19,accounts[1].getValue());
 		assertEquals("Account C differs",20,accounts[2].getValue());
 		assertEquals("Account D differs",17,accounts[3].getValue());
 		
     }
     
 /*  
     @Test
     public void testRotate() throws IOException {
    	// initialize accounts 
 		accounts = new Account[numLetters];
 		for (int i = A; i <= Z; i++) {
 			accounts[i] = new Account(Z-i);
 		}			 
 		
 		MultithreadedServer.runServer("src/hw12/data/rotate", accounts);
 			
 		assertEquals("Account A differs",47,accounts[0].getValue());
 		assertEquals("Account B differs",45,accounts[1].getValue());
 		assertEquals("Account C differs",43,accounts[2].getValue());
 		assertEquals("Account D differs",41,accounts[3].getValue());
 		assertEquals("Account E differs",39,accounts[4].getValue());
 		assertEquals("Account F differs",37,accounts[5].getValue());
 		assertEquals("Account G differs",35,accounts[6].getValue());
 		assertEquals("Account H differs",33,accounts[7].getValue());
 		assertEquals("Account I differs",31,accounts[8].getValue());
 		assertEquals("Account J differs",29,accounts[9].getValue());
 		assertEquals("Account K differs",27,accounts[10].getValue());
 		assertEquals("Account L differs",25,accounts[11].getValue());
 		assertEquals("Account M differs",23,accounts[12].getValue());
 		assertEquals("Account N differs",21,accounts[13].getValue());
 		assertEquals("Account O differs",19,accounts[14].getValue());
 		assertEquals("Account P differs",17,accounts[15].getValue());
 		assertEquals("Account Q differs",15,accounts[16].getValue());
 		assertEquals("Account R differs",13,accounts[17].getValue());
 		assertEquals("Account S differs",11,accounts[18].getValue());
 		assertEquals("Account T differs",9,accounts[19].getValue());
 		assertEquals("Account U differs",7,accounts[20].getValue());
 		assertEquals("Account V differs",5,accounts[21].getValue());
 		assertEquals("Account W differs",3,accounts[22].getValue());
 		assertEquals("Account X differs",1,accounts[23].getValue());
 		assertEquals("Account Y differs",47,accounts[24].getValue());
 		assertEquals("Account Z differs",92,accounts[25].getValue());


     }
	 	  	 
*/  
}