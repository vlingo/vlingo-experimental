package io.vlingo.xoom.experimental.fst;

import org.nustaq.serialization.FSTConfiguration;

public class BootstrapApp {
    public static void main(String... args) {
        final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        
        System.out.println("Hello Demo...");
    }
}
