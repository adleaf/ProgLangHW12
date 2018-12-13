package hw12;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Cache {
	Account account; //reference to the Account object it caches
	int initialValue; //initial value read when account peaked
	int currentValue; //value resulting from processing transaction
	boolean read = false; //was this account read from in cache?
	boolean written = false; //was this account written to the cache?
	int accountNum;
}

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable{
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Cache[] caches;
    private String transaction;
    private Account[] accounts; 
    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
    	caches = new Cache[allAccounts.length];
    	
    	for (int i = 0; i < allAccounts.length; i += 1)
    	{
    		caches[i] = new Cache();
    		
    	}
    	accounts = allAccounts;    	
        transaction = trans;
    }
    
    private Account parseAccount(String name) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (accounts[accountNum].peek() % numLetters);
            a = accounts[accountNum];
        }
        return a;
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //side will be 0 if it's on the left hand side (write) or 1 if it's on the right hand side (read)
    //
    private Cache parseAccount(String name, int side) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
        	//System.out.println("HERE ");
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            
            int peekedVal = accounts[accountNum].peek();
            accountNum = (peekedVal % numLetters);
            
            int val = accounts[accountNum].peek();
            //System.out.println("HERE " + accountNum);
            caches[accountNum].account = accounts[accountNum];
            caches[accountNum].initialValue = val;
            caches[accountNum].currentValue = val;
            caches[accountNum].read = true; 
            caches[accountNum].accountNum = accountNum; 
            //System.out.println(caches[accountNum].read);
           
            a = accounts[accountNum];
        }
        
        caches[accountNum].account = a;
        caches[accountNum].initialValue = accounts[accountNum].peek();
        caches[accountNum].currentValue = caches[accountNum].initialValue;
        caches[accountNum].accountNum = accountNum;
        if (side == 0) {
        	caches[accountNum].written = true;
        }else {
        	caches[accountNum].read = true;
        }        
        
        return caches[accountNum];
    }

    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name, 1).currentValue;//parseAccount(name).peek();
        }
        return rtn;
    }

    public void run() {
        // tokenize transaction
        String[] commands = transaction.split(";");

        for (int i = 0; i < commands.length; i++) {
        	System.out.print(commands[i] + " ");
        }
        //System.out.println("hi");
        for (int i = 0; i < commands.length; i++) {
            String[] words = commands[i].trim().split("\\s");
            if (words.length < 3)
                throw new InvalidTransactionError();
            Cache lhs = parseAccount(words[0], 0);//parseAccount(words[0]);
            if (!words[1].equals("="))
                throw new InvalidTransactionError();
            int rhs = parseAccountOrNum(words[2]);
            for (int j = 3; j < words.length; j+=2) {
                if (words[j].equals("+"))
                    rhs += parseAccountOrNum(words[j+1]);
                else if (words[j].equals("-"))
                    rhs -= parseAccountOrNum(words[j+1]);
                else
                    throw new InvalidTransactionError();
            }
            lhs.currentValue = rhs;
        }
        
        boolean failure = false;
        for (int i = 0; i < caches.length && failure == false; i +=1) { //opens all accounts
        	//System.out.print(i);
        	//System.out.println(caches[i].read);
        	if (caches[i].read || caches[i].written) {
        		try {
        			if (caches[i].read) { //This line is for andrew
        				System.out.println("Trying to open in read mode: "+ caches[i].accountNum);
        				caches[i].account.open(false);
        				System.out.println("Sucessfully opened in read mode: "+ caches[i].accountNum);
        				
        			}
        			if (caches[i].written) {
        				System.out.println("Trying to open in write mode: "+ caches[i].accountNum);
        				caches[i].account.open(true); //opens for writing
        				System.out.println("Sucessfully to opened in write mode: "+ caches[i].accountNum);
        			}
        		} catch (TransactionAbortException e) {
        			for (int j = i-1; j >= 0; j -= 1) {  //once we hit a already opened account, it closes all 
        											   //previously opened accounts
        				if (caches[j].read || caches[j].written) {
        					caches[j].account.close(); 
        					System.out.println("Closing: " + j);
        				}
        			}      			
        			failure = true; //a flag for the for loop, so it wont keep opening after failure
        			System.out.println("Failure in opening: "+i);
        			run(); //not sure if this is correct
        		}
        	}
        }
        //We now have all accounts, we need to verify that each account holds the expected value. 
        for (int i = 0; i < caches.length && failure == false; i +=1) {
        	if (caches[i].read) {
        		try {
        			int expected = caches[i].initialValue; 
        			System.out.println("Trying to read " + i);
        			caches[i].account.verify(expected);
        			
        		}
        		catch (TransactionAbortException e) { //something has been modified. We need to close all accounts and 
        			                                  //retry again.
        			for (int j = 0; j < caches.length; j += 1) {  //close all accounts we opened.
        				if (caches[j].read || caches[j].written)
        					caches[j].account.close(); 
        			}
        			System.out.println("FAILURE IN VERIFING");
        			failure = true; //a flag for the for loop, so it wont keep opening after failure
        			run(); //retry not sure if correct      			
        		}
        	}
        }
        
        //We now have verified all variables. Let us update all
        for (int i = 0; i < caches.length && failure == false; i +=1) {
        	if (caches[i].written) {
        		int finalValue = caches[i].currentValue;
        		System.out.println(finalValue);
        		caches[i].account.update(finalValue);
        		System.out.println("commit: " + transaction);
        	}
        }
        //We now can close all.
        for (int i = 0; i < caches.length && failure == false; i +=1) {
        	if (caches[i].read || caches[i].written) {
        		System.out.println("Closing "+i);
        		caches[i].account.close();
        	}
        }
        
    }
}

public class MultithreadedServer {
	
	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  
        
        ExecutorService executor = Executors.newFixedThreadPool(3); 
        
        while ((line = input.readLine()) != null) {
            Task t = new Task(accounts, line);
            executor.execute(t);
        }
        try {
        	executor.shutdown();
        	executor.awaitTermination(20, TimeUnit.MINUTES);
        }
        catch(InterruptedException ex) {
        	ex.getCause().printStackTrace();
        }
        
        input.close();

    }
}
