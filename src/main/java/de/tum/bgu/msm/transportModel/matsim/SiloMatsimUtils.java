package de.tum.bgu.msm.transportModel.matsim;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.Tuple;
import org.opengis.feature.simple.SimpleFeature;

import com.pb.common.matrix.Matrix;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import de.tum.bgu.msm.data.Household;
import de.tum.bgu.msm.data.HouseholdDataManager;
import de.tum.bgu.msm.data.Job;
import de.tum.bgu.msm.data.Person;

/**
 * @author dziemke
 */
public class SiloMatsimUtils {
	private final static Logger LOG = Logger.getLogger(SiloMatsimUtils.class);
	
	private final static GeometryFactory geometryFactory = new GeometryFactory();
	
	public static Config createMatsimConfig(Config config, String runId, double populationScalingFactor, double workerScalingFactor) {
		LOG.info("Stating creating a MATSim config.");
		double flowCapacityFactor = populationScalingFactor;
		config.qsim().setFlowCapFactor(flowCapacityFactor);
		
		// According to "NicolaiNagel2013HighResolutionAccessibility (citing Rieser on p.9):
		// Storage_Capacitiy_Factor = Sampling_Rate / ((Sampling_Rate) ^ (1/4))
		double storageCapacityFactor = Math.round((flowCapacityFactor / (Math.pow(flowCapacityFactor, 0.25)) * 100)) / 100.;
		config.qsim().setStorageCapFactor(storageCapacityFactor);
		
		String outputDirectoryRoot = config.controler().getOutputDirectory();
		String outputDirectory = outputDirectoryRoot + "/" + runId + "/";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setFirstIteration(0);
		config.controler().setMobsim("qsim");
		config.controler().setWritePlansInterval(config.controler().getLastIteration());
		config.controler().setWriteEventsInterval(config.controler().getLastIteration());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
	
		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles);
		config.vspExperimental().setWritingOutputEvents(true); // writes final events into toplevel directory
		
		{				
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ChangeExpBeta");
			strategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(strategySettings);
		}{
			StrategySettings strategySettings = new StrategySettings();
			strategySettings.setStrategyName("ReRoute");
			strategySettings.setWeight(0.2);
			config.strategy().addStrategySettings(strategySettings);
		}
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.strategy().setMaxAgentPlanMemorySize(4);
		
		ActivityParams homeActivity = new ActivityParams("home");
		homeActivity.setTypicalDuration(12*60*60);
		config.planCalcScore().addActivityParams(homeActivity);
		
		ActivityParams workActivity = new ActivityParams("work");
		workActivity.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(workActivity);
		
		config.qsim().setNumberOfThreads(1);
		config.global().setNumberOfThreads(1);
		config.parallelEventHandling().setNumberOfThreads(1);
		config.qsim().setUsingThreadpool(false);
		
		// TODO is this required?
		AccessibilityConfigGroup accessibilityConfigGroup = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		double timeOfDay = accessibilityConfigGroup.getTimeOfDay();
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
	
		LOG.info("Finished creating a MATSim config.");
		return config;
	}

	public static Population createMatsimPopulation(Config config, HouseholdDataManager householdDataManager, int year,
			Map<Integer,SimpleFeature> zoneFeatureMap, double scalingFactor, Random random) {
		LOG.info("Starting creating a MATSim population.");
    	Collection<Person> siloPersons = householdDataManager.getPersons();
    	
    	Population matsimPopulation = PopulationUtils.createPopulation(config);
    	PopulationFactory matsimPopulationFactory = matsimPopulation.getFactory();
    	    	
    	for (Person siloPerson : siloPersons) {
    		if (random.nextDouble() > scalingFactor) {
    			// e.g. if scalingFactor = 0.01, there will be a 1% chance that the loop is not
    			// continued in the next step, i.e. that the person is added to the population
    			continue;
    		}

    		if (siloPerson.getOccupation() != 1) { // i.e. person does not work
    			continue;
    		}

    		int siloWorkplaceId = siloPerson.getWorkplace();
    		if (siloWorkplaceId == -2) { // i.e. person has workplace outside study area
    			continue;
    		}

    		int householdId = siloPerson.getHhId();
    		Household household = Household.getHouseholdFromId(householdId);
    		int numberOfWorkers = household.getNumberOfWorkers();
    		int numberOfAutos = household.getAutos();
    		if (numberOfWorkers == 0) {
    			throw new RuntimeException("If there are no workers in the household, the loop must already"
    					+ " have been continued by finding that the given person is not employed!");
    		}
    		if ((double) numberOfAutos/numberOfWorkers < 1.) {
    			if (random.nextDouble() > (double) numberOfAutos/numberOfWorkers) {
    				continue;
    			}
    		}

    		int siloPersonId = siloPerson.getId();
    		int siloHomeTazId = siloPerson.getHomeTaz();
    		Job job = Job.getJobFromId(siloWorkplaceId);
    		int workZoneId = job.getZone();

    		// Note: Do not confuse the SILO Person class with the MATSim Person class here
    		org.matsim.api.core.v01.population.Person matsimPerson = 
    				matsimPopulationFactory.createPerson(Id.create(siloPersonId, org.matsim.api.core.v01.population.Person.class));
    		matsimPopulation.addPerson(matsimPerson);

    		Plan matsimPlan = matsimPopulationFactory.createPlan();
    		matsimPerson.addPlan(matsimPlan);

    		SimpleFeature homeFeature = zoneFeatureMap.get(siloHomeTazId);
    		Coord homeCoordinates = SiloMatsimUtils.getRandomCoordinateInGeometry(homeFeature, random);
    		Activity activity1 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
    		activity1.setEndTime(6 * 3600 + 3 * random.nextDouble() * 3600); // TODO Potentially change later
    		matsimPlan.addActivity(activity1);
    		matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car)); // TODO Potentially change later

    		SimpleFeature workFeature = zoneFeatureMap.get(workZoneId);
    		Coord workCoordinates = SiloMatsimUtils.getRandomCoordinateInGeometry(workFeature, random);
    		Activity activity2 = matsimPopulationFactory.createActivityFromCoord("work", workCoordinates);
    		activity2.setEndTime(15 * 3600 + 3 * random.nextDouble() * 3600); // TODO Potentially change later
    		matsimPlan.addActivity(activity2);
    		matsimPlan.addLeg(matsimPopulationFactory.createLeg(TransportMode.car)); // TODO Potentially change later

    		Activity activity3 = matsimPopulationFactory.createActivityFromCoord("home", homeCoordinates);
    		matsimPlan.addActivity(activity3);
    	}
    	LOG.info("Finished creating a MATSim population.");
    	return matsimPopulation;
    }
	
	public static final Coord getRandomCoordinateInGeometry(SimpleFeature feature, Random random) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();
		while (true) {
			Point point = getRandomPointInEnvelope(envelope, random);
			if (point.within(geometry)) {
				return new Coord(point.getX(), point.getY());
			}
		}
	}
	
	public static final Point getRandomPointInEnvelope(Envelope envelope, Random random) {
		double x = envelope.getMinX() + random.nextDouble() * envelope.getWidth();
		double y = envelope.getMinY() + random.nextDouble() * envelope.getHeight();
		return geometryFactory.createPoint(new Coordinate(x,y));
	}
	
	public static final Matrix convertTravelTimesToImpedanceMatrix(
			Map<Tuple<Integer, Integer>, Float> travelTimesMap, int rowCount, int columnCount, int year) {
		LOG.info("Converting MATSim travel times to impedance matrix for " + year + ".");
		String name = "travelTimeMatrix";
		String description = name;
		
		Matrix matrix = new Matrix(name, description, rowCount, columnCount);

		// Do not just increment by 1! Some values are missing. So, do not confuse the array index with the array entry!
		for (int i = 1; i <= rowCount; i++) {
			for (int j = 1; j <= columnCount; j++) {
//				Tuple<Integer, Integer> zone2Zone = new Tuple<Integer, Integer>(zones[i], zones[j]);
//				matrix.setValueAt(zones[i], zones[j], travelTimesMap.get(zone2Zone));
				
				Tuple<Integer, Integer> zone2Zone = new Tuple<Integer, Integer>(i, j);
				if (travelTimesMap.containsKey(zone2Zone)) {
					matrix.setValueAt(i, j, travelTimesMap.get(zone2Zone));
				} else {
					matrix.setValueAt(i, j, 0.f);
				}
			}
		}	
		return matrix;
	}
}