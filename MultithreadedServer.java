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

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Cache[] allCaches, String trans) {
        caches = allCaches;
        transaction = trans;
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
        Account a = caches[accountNum].account;
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            accountNum = (caches[accountNum].account.peek() % numLetters);
            a = caches[accountNum].account;
        }
        Cache c = new Cache();
        c.account = a;
        if (side == 0) {
        	c.written = true;
        }else {
        	c.read = true;
        }
        return c;
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
        System.out.println("hi");
        
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
            try {
                lhs.account.open(true);//lhs.open(true);
            } catch (TransactionAbortException e) {
                // won't happen in sequential version
            }
            lhs.account.update(rhs);//lhs.update(rhs);
            lhs.account.close();//lhs.close();
        }
        System.out.println("commit: " + transaction);
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
