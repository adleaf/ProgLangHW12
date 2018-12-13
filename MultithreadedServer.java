package hw12;

import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Cache {
	Account account; // reference to the Account object it caches
	int initialValue; // initial value read when account peaked
	int currentValue; // value resulting from processing transaction
	boolean read = false; // was this account read from in cache?
	boolean written = false; // was this account written to the cache?
	int accountNum;
	boolean notSet;

}

// TO DO: Task is currently an ordinary class.
// You will need to modify it to make it a task,
// so it can be given to an Executor thread pool.
//
class Task implements Runnable {
	private static final int A = constants.A;
	private static final int Z = constants.Z;
	private static final int numLetters = constants.numLetters;

	//an array to hold all caches
	private Cache[] caches;
	
	//string to hold the current transaction
	private String transaction;
	
	//array to hold all the accounts
	private Account[] accounts;
	
	// TO DO: The sequential version of Task peeks at accounts
	// whenever it needs to get a value, and opens, updates, and closes
	// an account whenever it needs to set a value. This won't work in
	// the parallel version. Instead, you'll need to cache values
	// you've read and written, and then, after figuring out everything
	// you want to do, (1) open all accounts you need, for reading,
	// writing, or both, (2) verify all previously peeked-at values,
	// (3) perform all updates, and (4) close all opened accounts.
	
	// requires: Valid account array and transaction string
	// modifies: transaction, caches, accounts
	// effects: creates a Task object and initializes accounts, caches, and transaction
	public Task(Account[] allAccounts, String trans) {
		//reset caches
		caches = new Cache[allAccounts.length];

		for (int i = 0; i < allAccounts.length; i += 1) {
			caches[i] = new Cache();

		}
		
		//reset all accounts
		accounts = allAccounts;
		
		//set the transaction
		transaction = trans;
	}

	// TO DO: parseAccount currently returns a reference to an account.
	// You probably want to change it to return a reference to an
	// account cache instead.
	// side will be 0 if it's on the left hand side (write) or 1 if it's on the
	// right hand side (read)
	
	// requires: name != null, side is a valid int
	// modifies: accounts, caches
	// effects: accounts change according to their values and the value of side
	private Cache parseAccount(String name, int side) {
		//get the account number from the letter
		int accountNum = (int) (name.charAt(0)) - (int) 'A';
		
		//make sure it's a valid account
		if (accountNum < A || accountNum > Z)
			throw new InvalidTransactionError();
		
		//get the account from the array
		Account a = accounts[accountNum];
		
		//go through the stars (*) in the name
		for (int i = 1; i < name.length(); i++) {
			//make sure it's valid
			if (name.charAt(i) != '*')
				throw new InvalidTransactionError();

			//peek at the value of the account
			int peekedVal = accounts[accountNum].peek();
			
			//if the current value isn't equal to the peeked value, update the peeked value
			if (caches[accountNum].currentValue != peekedVal) {
				peekedVal = caches[accountNum].currentValue;
			}
			
			//get the account number from the value
			accountNum = (peekedVal % numLetters);
			
			//if the caches doesn't exists, set it up with the current values
			if (caches[accountNum].account == null) {
				int val = accounts[accountNum].peek();
				caches[accountNum].account = accounts[accountNum];
				caches[accountNum].initialValue = val;
				caches[accountNum].currentValue = val;
				caches[accountNum].read = true;
				caches[accountNum].accountNum = accountNum;
			}
			a = accounts[accountNum];
		}

		// set cache again just in case there were no stars
		if(caches[accountNum].account == null) {
			caches[accountNum].account = a;
			caches[accountNum].initialValue = accounts[accountNum].peek();
			caches[accountNum].currentValue = caches[accountNum].initialValue;
			caches[accountNum].notSet = true;
			caches[accountNum].accountNum = accountNum;
		}
		
		//if it's the left hand side, it's going to be written
		if (side == 0) {
			caches[accountNum].written = true;
		//else it's on the right hand side and will be read
		} else {
			caches[accountNum].read = true;
		}

		//return the cache
		return caches[accountNum];
	}

	// requires: name is a valid string
	// modifies: accounts, caches
	// effects: accounts and caches change according to the given value
	private int parseAccountOrNum(String name) {
		int rtn;
		//if the char is a number, return its value
		if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
			rtn = new Integer(name).intValue();
		//else return the value in the account
		} else {
			rtn = parseAccount(name, 1).currentValue;
		}
		return rtn;
	}

	// requires: transaction != null
	// modifies: accounts, caches
	// effects: accounts and caches change according to transactions in inputFile
	public void run() {
		// tokenize transaction
		String[] commands = transaction.split(";");
		for (int i = 0; i < commands.length; i++) {
			String[] words = commands[i].trim().split("\\s");
			if (words.length < 3)
				throw new InvalidTransactionError();
			Cache lhs = parseAccount(words[0], 0);// parseAccount(words[0]);
			if (!words[1].equals("="))
				throw new InvalidTransactionError();
			int rhs = parseAccountOrNum(words[2]);
			for (int j = 3; j < words.length; j += 2) {
				if (words[j].equals("+"))
					rhs += parseAccountOrNum(words[j + 1]);
				else if (words[j].equals("-"))
					rhs -= parseAccountOrNum(words[j + 1]);
				else
					throw new InvalidTransactionError();
			}
			lhs.currentValue = rhs;
		}
		boolean failure = false;
		for (int i = 0; i < caches.length && failure == false; i += 1) { // opens
			if (caches[i].read || caches[i].written) {
				try {
					if (caches[i].read) {
						caches[i].account.open(false);
					}
					if (caches[i].written) {
						caches[i].account.open(true);
					}
				} catch (TransactionAbortException e) {
					for (int j = i - 1; j >= 0; j -= 1) { // once we hit a
						if (caches[j].read || caches[j].written) {
							caches[j].account.close();
						}
					}
					failure = true;
				}
			}
		}
		for (int i = 0; i < caches.length && failure == false; i += 1) {
			if (caches[i].read) {
				try {
					int expected = caches[i].initialValue;
					caches[i].account.verify(expected);

				} catch (TransactionAbortException e) { // something has been
					for (int j = 0; j < caches.length; j += 1) { // close all
						if (caches[j].read || caches[j].written)
							caches[j].account.close();
					}
					failure = true; // a flag for the for loop, so it wont keep
				}
			}
		}

		// We now have verified all variables. Let us update all
		for (int i = 0; i < caches.length && failure == false; i += 1) {
			if (caches[i].written) {
				int finalValue = caches[i].currentValue;
				caches[i].account.update(finalValue);
			}
		}
		// We now can close all.
		for (int i = 0; i < caches.length && failure == false; i += 1) {
			if (caches[i].read || caches[i].written) {
				caches[i].account.close();
				
			}
		}
		if (failure == true) { // if failure is true, reset the cache
			for (int i = 0; i < caches.length; i += 1) {
				caches[i] = new Cache();
			}
			
			run();
		}
	
	}
}

public class MultithreadedServer {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are
	// properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
	public static void runServer(String inputFile, Account accounts[]) throws IOException {

		// read transactions from input file
		String line;
		BufferedReader input = new BufferedReader(new FileReader(inputFile));

		// TO DO: you will need to create an Executor and then modify the
		// following loop to feed tasks to the executor instead of running them
		// directly.

		//Executor to run the program with up to 10 threads
		ExecutorService executor = Executors.newFixedThreadPool(10);

		//go through each input line and execute the command
		while ((line = input.readLine()) != null) {
			Task t = new Task(accounts, line);
			executor.execute(t);
		}
		//wait for all the threads to terminate
		try {
			executor.shutdown();
			executor.awaitTermination(20, TimeUnit.MINUTES);
		} catch (InterruptedException ex) {
			ex.getCause().printStackTrace();
		}

		input.close();

	}
}