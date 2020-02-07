import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {
  private Map<Position, CrossingSensor> crossingSensors = new HashMap<>();
  private Set<Position> endSensors = new HashSet<>();
  private List<Semaphore> semaphores = new ArrayList<>();

  private class SensorConfig {
    private Region region;
    private Direction direction;

    public SensorConfig(Region region, Direction direction) {
      this.region = region;
      this.direction = direction;
    }
  }

  private enum Direction {
    LEFT, RIGHT
  }

  private enum Region {
    A(0), B(1), C(2), D(3), E(4), F(5), G(6), H(7), MAX(8);

    public final int value;

    private Region(int value) {
      this.value = value;
    }
  }

  private class Position {
    public final int x;
    public final int y;

    public Position(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Position position = (Position) o;
      if (x != position.x)
        return false;
      if (y != position.y)
        return false;
      return true;
    }

    @Override
    public int hashCode() {
      int hash = 17;
      hash = ((hash + x) << 5) - (hash + x);
      hash = ((hash + y) << 5) - (hash + y);
      return hash;
    }
  }

  private class CrossingSensor {
    List<SensorConfig> regions;
    Position switchPosition;

    public CrossingSensor(Position switchPosition, List<SensorConfig> regions) {
      this.switchPosition = switchPosition;
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
      try {
        tsi.setSpeed(id, speed);
        while (true) {
          SensorEvent event = tsi.getSensor(id);
          Position position = new Position(event.getXpos(), event.getYpos());
          CrossingSensor crossingSensor = crossingSensors.get(position);
          if (crossingSensor != null) {
            if (regions.size() == 2) {
              if (event.getStatus() == SensorEvent.INACTIVE) {
                if (crossingSensor.regions.size() > 1) {
                  for (SensorConfig region : crossingSensor.regions) {
                    if (region.region == regions.peek()) {
                      semaphores.get(regions.poll().value).release();
                      break;
                    }
                  }
                } else if (crossingSensor.regions.size() == 1) {
                  if (crossingSensor.regions.get(0).region == regions.peek()) {
                    semaphores.get(regions.poll().value).release();
                  }
                } else {
                  throw new IllegalStateException("Invalid number of regions");
                }
              }
            } else if (regions.size() == 1) {
              if (event.getStatus() == SensorEvent.ACTIVE) {
                if (crossingSensor.regions.size() > 1) {
                  for (SensorConfig region : crossingSensor.regions) {
                    Semaphore semaphore = semaphores.get(region.region.value);
                    if (semaphore.tryAcquire()) {
                      regions.add(region.region);
                      setSwitch(crossingSensor.switchPosition, region.direction);
                      break;
                    }
                  }
                } else if (crossingSensor.regions.size() == 1) {
                  SensorConfig region = crossingSensor.regions.get(0);
                  Semaphore semaphore = semaphores.get(region.region.value);
                  if (!semaphore.tryAcquire()) {
                    tsi.setSpeed(id, 0);
                    semaphore.acquire();
                  }
                  tsi.setSpeed(id, speed);
                  regions.add(region.region);
                  setSwitch(crossingSensor.switchPosition, region.direction);
                } else {
                  throw new IllegalStateException("Each region must have at least one outgoing edge");
                }
              }
            } else {
              throw new IllegalStateException("Invalid number of regions");
            }
          }
          if (event.getStatus() == SensorEvent.ACTIVE && endSensors.contains(position)) {
            tsi.setSpeed(id, 0);
            Thread.sleep(1000 + 20 * Math.abs(speed), 0);
            speed *= -1;
            tsi.setSpeed(id, speed);
          }
        }
      } catch (InterruptedException | CommandException e) {
        e.printStackTrace();
      }
    }

    private void setSwitch(Position position, Direction direction) throws CommandException {
      tsi.setSwitch(position.x, position.y,
          direction == Direction.RIGHT ? TSimInterface.SWITCH_RIGHT : TSimInterface.SWITCH_LEFT);
    }
  }

  public Lab1(int speed1, int speed2) {
    try {
      initSemaphores();
      initSensors();

      semaphores.get(Region.A.value).acquire();
      semaphores.get(Region.G.value).acquire();
      new Thread(new Train(1, speed1, Region.A)).start();
      new Thread(new Train(2, speed2, Region.G)).start();
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void initSemaphores() {
    for (int i = 0; i < Region.MAX.value; ++i) {
      semaphores.add(new Semaphore(1));
    }
  }

  private void initSensors() {
    addCrossingSensor(new Position(1, 11), new Position(3, 11), new SensorConfig(Region.G, Direction.LEFT),
        new SensorConfig(Region.H, Direction.RIGHT));
    addCrossingSensor(new Position(2, 9), new Position(4, 9), new SensorConfig(Region.D, Direction.LEFT),
        new SensorConfig(Region.E, Direction.RIGHT));
    addCrossingSensor(new Position(3, 13), new Position(3, 11), new SensorConfig(Region.F, Direction.RIGHT));
    addCrossingSensor(new Position(5, 10), new Position(4, 9), new SensorConfig(Region.F, Direction.RIGHT));
    addCrossingSensor(new Position(5, 11), new Position(3, 11), new SensorConfig(Region.F, Direction.LEFT));
    addCrossingSensor(new Position(6, 9), new Position(4, 9), new SensorConfig(Region.F, Direction.LEFT));
    addCrossingSensor(new Position(13, 9), new Position(15, 9), new SensorConfig(Region.C, Direction.RIGHT));
    addCrossingSensor(new Position(14, 10), new Position(15, 9), new SensorConfig(Region.C, Direction.LEFT));
    addCrossingSensor(new Position(15, 7), new Position(17, 7), new SensorConfig(Region.C, Direction.RIGHT));
    addCrossingSensor(new Position(16, 8), new Position(17, 7), new SensorConfig(Region.C, Direction.LEFT));
    addCrossingSensor(new Position(17, 9), new Position(15, 9), new SensorConfig(Region.D, Direction.RIGHT),
        new SensorConfig(Region.E, Direction.LEFT));
    addCrossingSensor(new Position(19, 7), new Position(17, 7), new SensorConfig(Region.A, Direction.RIGHT),
        new SensorConfig(Region.B, Direction.LEFT));

    endSensors.add(new Position(16, 3));
    endSensors.add(new Position(16, 5));
    endSensors.add(new Position(16, 11));
    endSensors.add(new Position(16, 13));
  }

  private void addCrossingSensor(Position sensorPosition, Position switchPosition, SensorConfig... regions) {
    crossingSensors.put(sensorPosition, new CrossingSensor(switchPosition, new ArrayList<>(Arrays.asList(regions))));
  }
}
