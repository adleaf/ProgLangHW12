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

public class MultithreadedServerTest extends TestCase {
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
     
     //Test incrememnting all accounts by 1
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
     
     //test a few transactions
     @Test
     public void testChange() throws IOException {
    	 //This test also tests to avoid deadlock in concurrent version
    	 
    	// initialize accounts 
 		accounts = new Account[numLetters];
 		for (int i = A; i <= Z; i++) {
 			accounts[i] = new Account(Z-i);
 		}
 		
 		MultithreadedServer.runServer("src/hw12/data/testChanges", accounts);
 		
 		boolean testWorks = (
 				(accounts[0].getValue() == 28 && accounts[1].getValue() == 26 && (accounts[2].getValue() == 50 || accounts[2].getValue() == 54))
 				|| (accounts[0].getValue() == 26 && accounts[1].getValue() == 27 && (accounts[2].getValue() == 50 || accounts[2].getValue() == 53))
 				);
		
 		assertTrue(testWorks);
 		
     } 
       

}