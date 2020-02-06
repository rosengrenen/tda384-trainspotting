import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {
  private Map<Position, CrossingSensor> crossingSensors = new HashMap<>();
  private Map<Position, Void> endSensors = new HashMap<>();
  private List<Semaphore> semaphores = new ArrayList<>();

  private enum Region {
    A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), MAX(8);

    public final int value;

    private Region(int value) {
      this.value = value;
    }
  }

  private class Position {
    public int x;
    public int y;

    public Position(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  private class CrossingSensor {
    List<Region> regions;

    public CrossingSensor(List<Region> regions) {
      this.regions = regions;
    }
  }

  private class Train implements Runnable {
    private int id;
    private int speed;
    private Queue<Region> regions = new ArrayDeque<>();
    private TSimInterface tsi = TSimInterface.getInstance();

    public Train(int id, int speed, Region region) {
      this.id = id;
      this.speed = speed;
      this.regions.add(region);
    }

    @Override
    public void run() {
      // try {
      // tsi.setSpeed(id, speed);
      // SensorEvent event = tsi.getSensor(id);
      // if (event.getStatus() == SensorEvent.ACTIVE) {
      // if (event.getXpos() == 16 && event.getYpos() == 7) {
      // if (semaphoreC.tryAcquire()) {
      // tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
      // semaphoreA.release();
      // } else {
      // tsi.setSpeed(id, 0);
      // while (!semaphoreC.tryAcquire(10, TimeUnit.SECONDS))
      // ;
      // tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
      // semaphoreA.release();
      // tsi.setSpeed(id, speed);
      // }
      // }
      // }
      // while (true) {
      // SensorEvent event = tsi.getSensor(id);
      // if (event.getStatus() == SensorEvent.ACTIVE) {
      // tsi.setSpeed(id, 0);
      // }
      // System.out.println("Status " + (event.getStatus() == SensorEvent.ACTIVE));
      // }
      // } catch (InterruptedException | CommandException e) {
      // e.printStackTrace();
      // }
    }
  }

  public Lab1(int speed1, int speed2) {
    try {
      // Initialise semaphores
      for (int i = 0; i < Region.MAX.value; ++i) {
        semaphores.add(new Semaphore(1));
      }
      semaphores.get(Region.A.value).acquire();
      semaphores.get(Region.C.value).acquire();
      semaphores.get(Region.G.value).acquire();

      // Initialise sensors
      crossingSensors.put(new Position(2, 11),
          new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.G, Region.H))));
      crossingSensors.put(new Position(3, 9),
          new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.D, Region.E))));
      crossingSensors.put(new Position(3, 12), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.F))));
      crossingSensors.put(new Position(4, 10), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.F))));
      crossingSensors.put(new Position(4, 11), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.F))));
      crossingSensors.put(new Position(5, 9), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.F))));
      crossingSensors.put(new Position(14, 9), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.C))));
      crossingSensors.put(new Position(15, 10), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.C))));
      crossingSensors.put(new Position(16, 7), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.C))));
      crossingSensors.put(new Position(16, 9),
          new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.D, Region.E))));
      crossingSensors.put(new Position(17, 8), new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.C))));
      crossingSensors.put(new Position(18, 7),
          new CrossingSensor(new ArrayList<Region>(Arrays.asList(Region.A, Region.B))));

      endSensors.put(new Position(17, 3), null);
      endSensors.put(new Position(17, 5), null);
      endSensors.put(new Position(17, 11), null);
      endSensors.put(new Position(17, 13), null);

      new Thread(new Train(1, speed1, Region.A)).start();
      new Thread(new Train(2, speed2, Region.G)).start();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
