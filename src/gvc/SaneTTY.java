/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2023 colun ( Yasunobu Imamura )
 * 
 */
package gvc;

import java.io.IOException;

public class SaneTTY implements Runnable {
	public SaneTTY() {
	}
	public void run() {
		try {
			Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "stty sane < /dev/tty" }).waitFor();
		}
		catch(InterruptedException e) {
			System.out.println("Please run: stty sane");
		}
		catch(IOException e) {
		}
	}
}
