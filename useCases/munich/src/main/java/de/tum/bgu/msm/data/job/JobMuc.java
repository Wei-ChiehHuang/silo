package de.tum.bgu.msm.data.job;

import de.tum.bgu.msm.data.MicroLocation;
import org.locationtech.jts.geom.Coordinate;

import java.util.Optional;

public class JobMuc implements Job, MicroLocation {

    private final int id;
    private int workerId;
    private final String type;
    private int startTimeInSeconds;
    private int workingTimeInSeconds;
    private Coordinate coordinate;
    private int zoneId;

    JobMuc(int id, int zoneId, Coordinate coordinate, int workerId, String type) {
        this.id = id;
        this.coordinate = coordinate;
        this.workerId = workerId;
        this.type = type;
        this.zoneId = zoneId;
    }

    public int getId () {
        return id;
    }

    public int getWorkerId() {
        return workerId;
    }

    public String getType() {
        return type;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public void setWorkerID(int personID) {
        this.workerId = personID;
    }

    public void setJobWorkingTime(int startTimeInSeconds, int workingTimeInSeconds){
        this.startTimeInSeconds = startTimeInSeconds;
        this.workingTimeInSeconds = workingTimeInSeconds;
    }

    public Optional<Integer> getStartTimeInSeconds() {
        return Optional.of(startTimeInSeconds);
    }

    public Optional<Integer> getWorkingTimeInSeconds() {
        return Optional.of(workingTimeInSeconds);
    }

    @Override
    public String toString() {
        return "Attributes of job       " + id
//                + "\nLocated in zone         " + zone
                + "\nLocated at         " + coordinate.toString() // TODO implement toString methods
                + "\nFilled by person        " + workerId
                + "\nJob type                " + type;
    }

    @Override
    public int getZoneId() {
        return this.zoneId;
    }

    @Override
    public Coordinate getCoordinate() {
        return coordinate;
    }
}
