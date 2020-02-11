import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {
  private Map<Position, Sensor> crossingSensors = new HashMap<>();
  private Set<Position> endSensors = new HashSet<>();
  private List<Semaphore> semaphores = new ArrayList<>();

  private class Pair<T1, T2> {
    private T1 first;
    private T2 second;

    public Pair(T1 first, T2 second) {
      this.first = first;
      this.second = second;
    }
  }

  private enum SwitchDirection {
    LEFT, RIGHT, NONE
  }

  private enum Region {
    A(0, true), B(-1, false), C(1, true), D(2, true), E(3, true), F(-1, false), G(4, true), H(5, true), I(-1, false),
    MAX(6, false);

    public final int value;
    public final boolean critical;

    private Region(int value, boolean critical) {
      this.value = value;
      this.critical = critical;
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

  private class Sensor {
    List<Pair<Region, SwitchDirection>> regions;
    Position switchPosition;

    public Sensor(Position switchPosition, List<Pair<Region, SwitchDirection>> regions) {
      this.switchPosition = switchPosition;
      this.regions = regions;
    }
  }

  private class Train implements Runnable {
    private int id;
    private int speed;
    private Deque<Region> regions = new ArrayDeque<>();
    private TSimInterface tsi = TSimInterface.getInstance();

    public Train(int id, int speed, Region region) {
      this.id = id;
      this.speed = speed;
      this.regions.add(region);
    }

    @Override
    public void run() {
      try {
        tsi.setDebug(false);
        tsi.setSpeed(id, speed);
        while (true) {
          SensorEvent event = tsi.getSensor(id);
          if (event.getStatus() != SensorEvent.ACTIVE) {
            continue;
          }

          Position position = new Position(event.getXpos(), event.getYpos());
          Sensor crossingSensor = crossingSensors.get(position);
          if (crossingSensor != null) {
            // Pop region if leaving
            boolean hasPopped = false;
            if (crossingSensor.regions.size() > 1) {
              for (Pair<Region, SwitchDirection> region : crossingSensor.regions) {
                if (region.first == regions.peek()) {
                  regions.poll();
                  if (region.first.critical) {
                    semaphores.get(region.first.value).release();
                  }
                  hasPopped = true;
                  break;
                }
              }
            } else if (crossingSensor.regions.size() == 1) {
              Region region = crossingSensor.regions.get(0).first;
              if (region == regions.peek()) {
                regions.poll();
                if (region.critical) {
                  semaphores.get(region.value).release();
                }
                hasPopped = true;
              }
            } else {
              throw new IllegalStateException("Invalid number of regions");
            }

            // If a region has been popped, we can't add one from the same event
            if (hasPopped) {
              continue;
            }

            // Push region if entering
            if (crossingSensor.regions.size() > 1) {
              if (crossingSensor.regions.size() > 1) {
                for (Pair<Region, SwitchDirection> region : crossingSensor.regions) {
                  if (region.first.critical) {
                    Semaphore semaphore = semaphores.get(region.first.value);
                    if (semaphore.tryAcquire()) {
                      if (region.second == SwitchDirection.NONE) {
                        regions.addFirst(region.first);
                      } else {
                        regions.add(region.first);
                        setSwitch(crossingSensor.switchPosition, region.second);
                      }
                      break;
                    }
                  } else {
                    if (region.second == SwitchDirection.NONE) {
                      regions.addFirst(region.first);
                    } else {
                      regions.add(region.first);
                      setSwitch(crossingSensor.switchPosition, region.second);
                    }
                    break;
                  }
                }
              }
            } else if (crossingSensor.regions.size() == 1) {
              Pair<Region, SwitchDirection> region = crossingSensor.regions.get(0);
              if (region.first.critical) {
                Semaphore semaphore = semaphores.get(region.first.value);
                if (!semaphore.tryAcquire()) {
                  tsi.setSpeed(id, 0);
                  semaphore.acquire();
                }
                tsi.setSpeed(id, speed);
              }
              if (region.second == SwitchDirection.NONE) {
                regions.addFirst(region.first);
              } else {
                regions.add(region.first);
                setSwitch(crossingSensor.switchPosition, region.second);
              }
            } else {
              throw new IllegalStateException("Invalid number of regions");
            }
          }

          if (endSensors.contains(position)) {
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

    private void setSwitch(Position position, SwitchDirection direction) throws CommandException {
      switch (direction) {
      case RIGHT:
        tsi.setSwitch(position.x, position.y, TSimInterface.SWITCH_RIGHT);
        break;
      case LEFT:
        tsi.setSwitch(position.x, position.y, TSimInterface.SWITCH_LEFT);
        break;
      case NONE:
        return;
      }
    }
  }

  public Lab1(int speed1, int speed2) {
    try {
      initSemaphores();
      initSensors();

      semaphores.get(Region.A.value).acquire();
      semaphores.get(Region.H.value).acquire();
      new Thread(new Train(1, speed1, Region.A)).start();
      new Thread(new Train(2, speed2, Region.H)).start();
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
    addCrossingSensor(new Position(6, 6), null, new Pair<>(Region.C, SwitchDirection.NONE));
    addCrossingSensor(new Position(11, 7), null, new Pair<>(Region.C, SwitchDirection.NONE));
    addCrossingSensor(new Position(14, 7), new Position(17, 7), new Pair<>(Region.D, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(19, 8), new Position(17, 7), new Pair<>(Region.A, SwitchDirection.RIGHT),
        new Pair<>(Region.B, SwitchDirection.LEFT));
    addCrossingSensor(new Position(18, 9), new Position(15, 9), new Pair<>(Region.E, SwitchDirection.RIGHT),
        new Pair<>(Region.F, SwitchDirection.LEFT));
    addCrossingSensor(new Position(12, 9), new Position(15, 9), new Pair<>(Region.D, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(7, 9), new Position(4, 9), new Pair<>(Region.G, SwitchDirection.LEFT));
    addCrossingSensor(new Position(1, 9), new Position(4, 9), new Pair<>(Region.E, SwitchDirection.LEFT),
        new Pair<>(Region.F, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(1, 10), new Position(3, 11), new Pair<>(Region.H, SwitchDirection.LEFT),
        new Pair<>(Region.I, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(6, 11), new Position(3, 11), new Pair<>(Region.G, SwitchDirection.LEFT));
    addCrossingSensor(new Position(4, 13), new Position(3, 11), new Pair<>(Region.G, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(6, 10), new Position(4, 9), new Pair<>(Region.G, SwitchDirection.RIGHT));
    addCrossingSensor(new Position(13, 10), new Position(15, 9), new Pair<>(Region.D, SwitchDirection.LEFT));
    addCrossingSensor(new Position(15, 8), new Position(17, 7), new Pair<>(Region.D, SwitchDirection.LEFT));
    addCrossingSensor(new Position(10, 8), null, new Pair<>(Region.C, SwitchDirection.NONE));
    addCrossingSensor(new Position(9, 5), null, new Pair<>(Region.C, SwitchDirection.NONE));

    endSensors.add(new Position(15, 3));
    endSensors.add(new Position(15, 5));
    endSensors.add(new Position(15, 11));
    endSensors.add(new Position(15, 13));
  }

  private void addCrossingSensor(Position sensorPosition, Position switchPosition,
      Pair<Region, SwitchDirection>... regions) {
    crossingSensors.put(sensorPosition, new Sensor(switchPosition, new ArrayList<>(Arrays.asList(regions))));
  }
}
