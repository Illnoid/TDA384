import TSim.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static java.util.Arrays.asList;

/**
 * the class responsible for all the operations related to the running of a train instance
 */
public class Lab1 {

  /**
   * is used to identify the different switches
   */
  enum SwitchName {
    NORTH_STATION_SWITCH, SOUTH_STATION_SWITCH, MIDDLE_SWITCH_EAST, MIDDLE_SWITCH_WEST
  }

  /**
   * is used to identify the different sensors
   */
  enum SensorName {
    SOUTH_OF_NORTH_STATION,NORTH_OF_NORTH_STATION, WEST_OF_CROSSING, NORTH_OF_CROSSING, EAST_OF_CROSSING, SOUTH_OF_CROSSING,
    SOUTHWEST_OF_NORTH_STATION_SWITCH, EAST_OF_NORTH_STATION_SWITCH, WEST_OF_NORTH_STATION_SWITCH, SOUTHEAST_OF_SOUTH_STATION_SWITCH,
    EAST_OF_SOUTH_STATION_SWITCH, WEST_OF_SOUTH_STATION_SWITCH, NORTH_OF_SOUTH_STATION, SOUTH_OF_SOUTH_STATION, WEST_OF_MIDDLE_SWITCH_EAST,
    EAST_OF_MIDDLE_SWITCH_EAST, SOUTHWEST_OF_MIDDLE_SWITCH_EAST, WEST_OF_MIDDLE_SWITCH_WEST, EAST_OF_MIDDLE_SWITCH_WEST,
    SOUTHEAST_OF_MIDDLE_SWITCH_WEST
  }

  /**
   * is used to identify the different semaphores
   */
  enum SemaphoreName {
    NORTH_STATION, SINGLETRACK_EAST, SINGLETRACK_WEST, SOUTH_STATION, MIDDLE_DUBBLE_TRACK,CROSSING
  }

  private Map<SemaphoreName, Semaphore> semaphoreMap = new HashMap<>();
  private Map<List<Integer>, SensorName> sensorMap = new HashMap<>();
  private Map<SwitchName, List<Integer>> switchMap = new HashMap<>();

  private TSimInterface tsi;

  public Lab1(int speed1, int speed2) {
    tsi = TSimInterface.getInstance();

    //All the switches.
    switchMap.put(SwitchName.NORTH_STATION_SWITCH, asList(17,7));
    switchMap.put(SwitchName.SOUTH_STATION_SWITCH,asList(3,11));
    switchMap.put(SwitchName.MIDDLE_SWITCH_EAST,asList(15,9));
    switchMap.put(SwitchName.MIDDLE_SWITCH_WEST,asList(4,9));

    //All the stations.
    sensorMap.put(asList(16,3), SensorName.NORTH_OF_NORTH_STATION);
    sensorMap.put(asList(16,5), SensorName.SOUTH_OF_NORTH_STATION);
    sensorMap.put(asList(16,11), SensorName.NORTH_OF_SOUTH_STATION);
    sensorMap.put(asList(16,13), SensorName.SOUTH_OF_SOUTH_STATION);

    //All the sensors surrounding the crossing.
    sensorMap.put(asList(8,5), SensorName.NORTH_OF_CROSSING);
    sensorMap.put(asList(6,7), SensorName.WEST_OF_CROSSING);
    sensorMap.put(asList(9,8), SensorName.SOUTH_OF_CROSSING);
    sensorMap.put(asList(10,7), SensorName.EAST_OF_CROSSING);

    //All the sensors surrounding the north station switch.
    sensorMap.put(asList(14,7), SensorName.WEST_OF_NORTH_STATION_SWITCH);
    sensorMap.put(asList(15,8), SensorName.SOUTHWEST_OF_NORTH_STATION_SWITCH);
    sensorMap.put(asList(18,7), SensorName.EAST_OF_NORTH_STATION_SWITCH);

    //All the sensors surrounding the south station switch.
    sensorMap.put(asList(2,11), SensorName.WEST_OF_SOUTH_STATION_SWITCH);
    sensorMap.put(asList(5,11), SensorName.EAST_OF_SOUTH_STATION_SWITCH);
    sensorMap.put(asList(4,13), SensorName.SOUTHEAST_OF_SOUTH_STATION_SWITCH);

    //All the sensors surrounding the switch east to the middle of the map.
    sensorMap.put(asList(16,9), SensorName.EAST_OF_MIDDLE_SWITCH_EAST);
    sensorMap.put(asList(12,9), SensorName.WEST_OF_MIDDLE_SWITCH_EAST);
    sensorMap.put(asList(13,10), SensorName.SOUTHWEST_OF_MIDDLE_SWITCH_EAST);

    //All the sensors surrounding the switch west to the middle of the map.
    sensorMap.put(asList(3,9), SensorName.WEST_OF_MIDDLE_SWITCH_WEST);
    sensorMap.put(asList(7,9), SensorName.EAST_OF_MIDDLE_SWITCH_WEST);
    sensorMap.put(asList(6,10), SensorName.SOUTHEAST_OF_MIDDLE_SWITCH_WEST);

    //All the semaphores.
    semaphoreMap.put(SemaphoreName.CROSSING, new Semaphore(1));
    semaphoreMap.put(SemaphoreName.NORTH_STATION, new Semaphore(1));
    semaphoreMap.put(SemaphoreName.SOUTH_STATION, new Semaphore(1));
    semaphoreMap.put(SemaphoreName.MIDDLE_DUBBLE_TRACK, new Semaphore(1));
    semaphoreMap.put(SemaphoreName.SINGLETRACK_EAST, new Semaphore(1));
    semaphoreMap.put(SemaphoreName.SINGLETRACK_WEST, new Semaphore(1));

    //The two trains to run on the map.
    Train train1 = new Train(1,speed1,SensorName.NORTH_OF_NORTH_STATION,tsi,null);
    Train train2 = new Train(2,speed2,SensorName.NORTH_OF_SOUTH_STATION,tsi,SemaphoreName.SOUTH_STATION);

    semaphoreMap.get(SemaphoreName.SOUTH_STATION).tryAcquire();

    train1.start();
    train2.start();

  }

  /**
   * Class containing all the behavior and logic necessary for the function of a given train.
   */
  // TODO: Implement a representation of the train
  class Train extends Thread {
    boolean forward;
    private int velocity;
    int id;
    TSimInterface tsi;
    int maxVelocity;
    SemaphoreName lastSemaphore;
    SemaphoreName currentSemaphore;
    SensorName lastSensor;

    Train(int id, int startVelocity, SensorName startSensor, TSimInterface tsi, SemaphoreName semaphoreName) {
      this.id = id;
      this.forward = true;
      this.maxVelocity = 50;
      setVelocity(startVelocity);
      this.tsi = tsi;
      this.lastSensor = startSensor;
      this.currentSemaphore = semaphoreName;

      try {
        tsi.setSpeed(id,this.velocity);
      } catch (CommandException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    private void setVelocity(int velocity) {
      if (velocity <= maxVelocity) {
        this.velocity = velocity;
      }
      else {
        this.velocity = maxVelocity;
      }
    }

    private void activateBreak() throws CommandException {
      tsi.setSpeed(id,0);
    }

    private void goForward() throws CommandException {
      if (forward) {
        tsi.setSpeed(id, velocity);
      }
      else {
        tsi.setSpeed(id, -velocity);
      }
    }

    private void changeDirection() {
      forward = !forward;
    }

    private void waitAtStation() {
      try {
        activateBreak();
        sleep(1000 + (20 * velocity));
        changeDirection();
        goForward();
      }
      catch (CommandException e) {
        e.printStackTrace();
        System.exit(1);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    private void setSwitch(SwitchName switchName, int direction) throws CommandException {
      tsi.setSwitch(switchMap.get(switchName).get(0),switchMap.get(switchName).get(1),direction);
    }

    /**
     * Stops the train until it is able to acquire the corresponding semaphore.
     *
     * @param semaphoreName         The name corresponding to the semaphore that is to be acquired.
     * @throws CommandException
     * @throws InterruptedException
     */
    private void stopUntilPass(SemaphoreName semaphoreName) throws CommandException, InterruptedException {
      Semaphore semaphore = semaphoreMap.get(semaphoreName);
      activateBreak();
      semaphore.acquire();
      updateSemaphores(semaphoreName);
      goForward();
    }

    /**
     * Releases the permit of a given semaphore.
     *
     * @param semaphoreName   The name of the semaphore who's permit is to be released.
     */
    private void releasePermit(SemaphoreName semaphoreName) {
      Semaphore semaphore = semaphoreMap.get(semaphoreName);
      if (semaphore.availablePermits() == 0) {
        semaphore.release();
        lastSemaphore = currentSemaphore;
      }
    }

    private boolean semaphoreHasPermits(SemaphoreName semaphoreName) {
      Semaphore semaphore = semaphoreMap.get(semaphoreName);
      boolean hasPermit = semaphore.tryAcquire();
      updateSemaphores(semaphoreName);
      return hasPermit;
    }

    /**
     * Sets the acquired semaphore to the current one and saves the old one as the last semaphore.
     *
     * @param semaphoreName   The newly acquired semaphore.
     */
    private void updateSemaphores(SemaphoreName semaphoreName) {
        lastSemaphore = currentSemaphore;
        currentSemaphore = semaphoreName;
    }

    /**
     * Handels sensor events for the given train depending what sensor has be triggered.
     *
     * @param event   The given sensor event to handel.
     * @throws CommandException
     * @throws InterruptedException
     */
    private void manageSensorEvent(SensorEvent event) throws CommandException, InterruptedException {
      boolean isActive = event.getStatus() == SensorEvent.ACTIVE;
      SensorName sensorName = sensorMap.get(asList(event.getXpos(),event.getYpos()));
      if (isActive) {
        switch (sensorName) {
          case NORTH_OF_NORTH_STATION:
            if (lastSensor == SensorName.WEST_OF_CROSSING) {
              waitAtStation();
            }
            break;
          case SOUTH_OF_NORTH_STATION:
            if (lastSensor == SensorName.NORTH_OF_CROSSING) {
              waitAtStation();
            }
            break;
          case NORTH_OF_SOUTH_STATION:
            if (lastSensor == SensorName.EAST_OF_SOUTH_STATION_SWITCH) {
              waitAtStation();
            }
            break;
          case SOUTH_OF_SOUTH_STATION:
            if (lastSensor == SensorName.SOUTHEAST_OF_SOUTH_STATION_SWITCH) {
              waitAtStation();
            }
            break;

          case WEST_OF_CROSSING:
            if (lastSensor == SensorName.EAST_OF_CROSSING) {
              releasePermit(SemaphoreName.CROSSING);
            }
            else {
              stopUntilPass(SemaphoreName.CROSSING);
            }
            break;
          case EAST_OF_CROSSING:
            if (lastSensor == SensorName.WEST_OF_NORTH_STATION_SWITCH) {
              stopUntilPass(SemaphoreName.CROSSING);
            }
            else {
              releasePermit(SemaphoreName.CROSSING);
            }
            break;
          case NORTH_OF_CROSSING:
            if (lastSensor == SensorName.SOUTH_OF_CROSSING) {
              releasePermit(SemaphoreName.CROSSING);
            }
            else {
              stopUntilPass(SemaphoreName.CROSSING);
            }
            break;
          case SOUTH_OF_CROSSING:
            if (lastSensor == SensorName.SOUTHWEST_OF_NORTH_STATION_SWITCH) {
              stopUntilPass(SemaphoreName.CROSSING);
            }
            else {
              releasePermit(SemaphoreName.CROSSING);
            }
            break;

          case EAST_OF_NORTH_STATION_SWITCH:
            if (lastSensor == SensorName.SOUTHWEST_OF_NORTH_STATION_SWITCH) {
              releasePermit(SemaphoreName.NORTH_STATION);
            }
            else if (lastSensor == SensorName.EAST_OF_MIDDLE_SWITCH_EAST) {
              if (semaphoreHasPermits(SemaphoreName.NORTH_STATION)) {
                setSwitch(SwitchName.NORTH_STATION_SWITCH, TSimInterface.SWITCH_LEFT);
                goForward();
              }
              else {
                setSwitch(SwitchName.NORTH_STATION_SWITCH, TSimInterface.SWITCH_RIGHT);
              }
            }
            break;
          case WEST_OF_NORTH_STATION_SWITCH:
            if (lastSensor == SensorName.EAST_OF_CROSSING) {
              stopUntilPass(SemaphoreName.SINGLETRACK_EAST);
              setSwitch(SwitchName.NORTH_STATION_SWITCH, TSimInterface.SWITCH_RIGHT);
            }
            else {
              releasePermit(SemaphoreName.SINGLETRACK_EAST);
            }
            break;
          case SOUTHWEST_OF_NORTH_STATION_SWITCH:
            if (lastSensor == SensorName.SOUTH_OF_CROSSING) {
              stopUntilPass(SemaphoreName.SINGLETRACK_EAST);
              setSwitch(SwitchName.NORTH_STATION_SWITCH, TSimInterface.SWITCH_LEFT);
            }
            else {
              releasePermit(SemaphoreName.SINGLETRACK_EAST);
            }

          case WEST_OF_SOUTH_STATION_SWITCH:
            if (lastSensor == SensorName.WEST_OF_MIDDLE_SWITCH_WEST) {
              if (semaphoreHasPermits(SemaphoreName.SOUTH_STATION)) {
                setSwitch(SwitchName.SOUTH_STATION_SWITCH, TSimInterface.SWITCH_LEFT);
              }
              else {
                setSwitch(SwitchName.SOUTH_STATION_SWITCH, TSimInterface.SWITCH_RIGHT);
              }
            }
            else if (lastSensor == SensorName.EAST_OF_SOUTH_STATION_SWITCH) {
              releasePermit(SemaphoreName.SOUTH_STATION);
            }
            break;
          case EAST_OF_SOUTH_STATION_SWITCH:
            if (lastSensor == SensorName.WEST_OF_SOUTH_STATION_SWITCH) {
              releasePermit(SemaphoreName.SINGLETRACK_WEST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_WEST);
              setSwitch(SwitchName.SOUTH_STATION_SWITCH, TSimInterface.SWITCH_LEFT);
            }
            break;
          case SOUTHEAST_OF_SOUTH_STATION_SWITCH:
            if (lastSensor == SensorName.WEST_OF_SOUTH_STATION_SWITCH) {
              releasePermit(SemaphoreName.SINGLETRACK_WEST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_WEST);
              setSwitch(SwitchName.SOUTH_STATION_SWITCH, TSimInterface.SWITCH_RIGHT);
            }
            break;

          case WEST_OF_MIDDLE_SWITCH_WEST:
            if (lastSensor == SensorName.WEST_OF_SOUTH_STATION_SWITCH) {
              if (semaphoreHasPermits(SemaphoreName.MIDDLE_DUBBLE_TRACK)) {
                setSwitch(SwitchName.MIDDLE_SWITCH_WEST, TSimInterface.SWITCH_LEFT);
                goForward();
              }
              else {
                setSwitch(SwitchName.MIDDLE_SWITCH_WEST, TSimInterface.SWITCH_RIGHT);
              }
            }
            else {
              releasePermit(SemaphoreName.MIDDLE_DUBBLE_TRACK);
            }
            break;
          case EAST_OF_MIDDLE_SWITCH_WEST:
            if (lastSensor == SensorName.WEST_OF_MIDDLE_SWITCH_WEST) {
              releasePermit(SemaphoreName.SINGLETRACK_WEST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_WEST);
              setSwitch(SwitchName.MIDDLE_SWITCH_WEST, TSimInterface.SWITCH_LEFT);
            }
            break;
          case SOUTHEAST_OF_MIDDLE_SWITCH_WEST:
            if (lastSensor == SensorName.WEST_OF_MIDDLE_SWITCH_WEST) {
              releasePermit(SemaphoreName.SINGLETRACK_WEST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_WEST);
              setSwitch(SwitchName.MIDDLE_SWITCH_WEST, TSimInterface.SWITCH_RIGHT);
            }
            break;

          case EAST_OF_MIDDLE_SWITCH_EAST:
            if (lastSensor == SensorName.EAST_OF_NORTH_STATION_SWITCH) {
              if (semaphoreHasPermits(SemaphoreName.MIDDLE_DUBBLE_TRACK)) {
                setSwitch(SwitchName.MIDDLE_SWITCH_EAST, TSimInterface.SWITCH_RIGHT);
                goForward();
              }
              else {
                setSwitch(SwitchName.MIDDLE_SWITCH_EAST, TSimInterface.SWITCH_LEFT);
              }
            }
            else {
              releasePermit(SemaphoreName.MIDDLE_DUBBLE_TRACK);
            }
            break;
          case WEST_OF_MIDDLE_SWITCH_EAST:
            if (lastSensor == SensorName.EAST_OF_MIDDLE_SWITCH_EAST) {
              releasePermit(SemaphoreName.SINGLETRACK_EAST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_EAST);
              setSwitch(SwitchName.MIDDLE_SWITCH_EAST, TSimInterface.SWITCH_RIGHT);
            }
            break;
          case SOUTHWEST_OF_MIDDLE_SWITCH_EAST:
            if (lastSensor == SensorName.EAST_OF_MIDDLE_SWITCH_EAST) {
              releasePermit(SemaphoreName.SINGLETRACK_EAST);
            }
            else {
              stopUntilPass(SemaphoreName.SINGLETRACK_EAST);
              setSwitch(SwitchName.MIDDLE_SWITCH_EAST, TSimInterface.SWITCH_LEFT);
            }
            break;
        }
        lastSensor = sensorName;
      }
    }

    @Override
    public void run() {
      while (true) {
        try {
          tsi.setSpeed(id, this.velocity);

        } catch (CommandException e) {
          e.printStackTrace();    // or only e.getMessage() for the error
          System.exit(1);
        }
        while (!this.isInterrupted()) {
          try {
            manageSensorEvent(tsi.getSensor(id));
          } catch (CommandException e) {
            e.printStackTrace();
            System.exit(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
          }
        }

      }
    }
  }

}
