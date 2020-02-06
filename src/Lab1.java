import TSim.*;

public class Lab1 {
  private class Train implements Runnable {
    private int id;
    private int speed;
    private TSimInterface tsi = TSimInterface.getInstance();

    public Train(int id, int speed) {
      this.id = id;
      this.speed = speed;
    }

    @Override
    public void run() {
      try {
        tsi.setSpeed(id, speed);
      } catch (CommandException e) {
        e.printStackTrace();
      }
    }
  }

  public Lab1(int speed1, int speed2) {
    new Thread(new Train(1, speed1)).start();
    new Thread(new Train(2, speed2)).start();
  }
}
