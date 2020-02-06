import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import TSim.TSimInterface;

public class Main {

    private static final String TSIM_PATH_ON_LAB_COMPUTERS = "/chalmers/groups/tda381/bin/tsim";
    /**
     * The main method expects 3-4 arguments, e.g.:
     * - command line: java -cp bin Main "Lab1.map" 5 10 20
     * - in Eclipse: add them from Run Configurations -> Arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Main method expects 3-4 arguments: Lab1.map <Train1Speed> <Train2Speed> [SimulatorSpeed]");
            System.exit(1);
        }

        String map = args[0];
        int train1_speed = Integer.parseInt(args[1]);
        int train2_speed = Integer.parseInt(args[2]);
        int tsim_speed = (args.length >= 4) ? Integer.parseInt(args[3]) : 20;

        String tsim;
        if (Files.exists(Paths.get(TSIM_PATH_ON_LAB_COMPUTERS))) {
            tsim = TSIM_PATH_ON_LAB_COMPUTERS;
        } else {
            // Otherwise tsim must be in your $PATH
            tsim = "tsim";
        }

        String tsimCommand = String.format("%s --speed=%d %s", tsim, tsim_speed, map);
        Process p = Runtime.getRuntime().exec(tsimCommand);
        TSimInterface.init(p.getInputStream(), p.getOutputStream());
        TSimInterface.getInstance().setDebug(true);
        new Lab1(train1_speed, train2_speed);
        // new Lab1Extra(train1_speed, train2_speed);
        p.waitFor();
    }
}
