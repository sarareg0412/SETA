package seta;

import java.io.IOException;

public class StdInSetaThread extends Thread {
    @Override
    public void run() {
        System.out.println("> Press ENTER to stop.");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("> Quitting SETA.");
        System.exit(0);
    }
}
