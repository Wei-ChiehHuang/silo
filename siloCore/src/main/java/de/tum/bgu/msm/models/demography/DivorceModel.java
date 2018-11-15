package de.tum.bgu.msm.models.demography;

import de.tum.bgu.msm.Implementation;
import de.tum.bgu.msm.data.person.Gender;
import de.tum.bgu.msm.utils.SiloUtil;
import de.tum.bgu.msm.container.SiloDataContainer;
import de.tum.bgu.msm.data.HouseholdDataManager;
import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdFactory;
import de.tum.bgu.msm.data.household.HouseholdUtil;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.data.person.PersonRole;
import de.tum.bgu.msm.events.IssueCounter;
import de.tum.bgu.msm.events.MicroEventModel;
import de.tum.bgu.msm.events.impls.person.DivorceEvent;
import de.tum.bgu.msm.models.AbstractModel;
import de.tum.bgu.msm.models.autoOwnership.munich.CreateCarOwnershipModel;
import de.tum.bgu.msm.models.relocation.MovesModelI;
import de.tum.bgu.msm.properties.Properties;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class DivorceModel extends AbstractModel implements MicroEventModel<DivorceEvent> {

    private final MovesModelI movesModel;
    private final CreateCarOwnershipModel carOwnership;
    private final HouseholdFactory hhFactory;

    private MarryDivorceJSCalculator calculator;
    private HashMap<Gender, double[]> divorceProbabilities;

    public DivorceModel(SiloDataContainer dataContainer, MovesModelI movesModel, CreateCarOwnershipModel carOwnership, HouseholdFactory hhFactory) {
        super(dataContainer);
        this.hhFactory = hhFactory;
        //setupModel();
        setupModelDistribution();
        this.movesModel = movesModel;
        this.carOwnership = carOwnership;
    }

    private void setupModel() {
        Reader reader;
        if (Properties.get().main.implementation == Implementation.MUNICH) {
            reader = new InputStreamReader(this.getClass().getResourceAsStream("MarryDivorceCalcMuc"));
        } else {
            reader = new InputStreamReader(this.getClass().getResourceAsStream("MarryDivorceCalcMstm"));
        }
        calculator = new MarryDivorceJSCalculator(reader, 0);
    }


    private void setupModelDistribution(){
        LogNormalDistribution femaleNormalDistribution = new LogNormalDistribution(3.739433, 0.25);
        LogNormalDistribution maleNormalDistribution = new LogNormalDistribution(3.7451,0.2459);
        GammaDistribution femaleGammaDistribution = new GammaDistribution(27.2130, 0.903879);
        GammaDistribution maleGammaDistribution = new GammaDistribution(25.4355, 0.9712);
        double scaleFemaleNormal = 0.4446;
        double scaleMaleNormal = 0.4357;
        double scaleFemaleGamma = 0.2364;
        double scaleMaleGamma = 0.2476;
        double[] probFemale = new double[101];
        double[] probMale = new double[101];
        for (int age = 15; age <= 100; age++){
            probFemale[age] = scaleFemaleNormal * femaleNormalDistribution.density((double) age) +
                    scaleFemaleGamma * femaleGammaDistribution.density((double) age);
            probMale[age] = scaleMaleNormal * maleNormalDistribution.density((double) age) +
                    scaleMaleGamma * maleGammaDistribution.density((double) age);
        }
        divorceProbabilities = new HashMap<>();
        divorceProbabilities.put(Gender.FEMALE,probFemale);
        divorceProbabilities.put(Gender.MALE, probMale);
    }

    @Override
    public Collection<DivorceEvent> prepareYear(int year) {
        final List<DivorceEvent> events = new ArrayList<>();
        for(Person person: dataContainer.getHouseholdData().getPersons()) {
            if (person.getRole() == PersonRole.MARRIED) {
                events.add(new DivorceEvent(person.getId()));
            }
        }
        return events;
    }

    @Override
    public boolean handleEvent(DivorceEvent event) {
        return chooseDivorce(event.getPersonId());    }

    @Override
    public void finishYear(int year) {

    }

    private boolean chooseDivorce(int perId) {
        // select if person gets divorced/leaves joint dwelling

        final HouseholdDataManager householdData = dataContainer.getHouseholdData();
        Person per = householdData.getPersonFromId(perId);
        if (per != null && per.getRole() == PersonRole.MARRIED) {
            //final double probability = calculator.calculateDivorceProbability(per.getType().ordinal()) / 2;
            final double probability = divorceProbabilities.get(per.getGender())[per.getAge()] / 2;
            if (SiloUtil.getRandomNumberAsDouble() < probability) {
                // check if vacant dwelling is available

                Household fakeHypotheticalHousehold = hhFactory.createHousehold(-1,-1,0);
                fakeHypotheticalHousehold.addPerson(per);
                int newDwellingId = movesModel.searchForNewDwelling(fakeHypotheticalHousehold);
                if (newDwellingId < 0) {
                    if (perId == SiloUtil.trackPp || per.getHousehold().getId() == SiloUtil.trackHh) {
                        SiloUtil.trackWriter.println(
                                "Person " + perId + " wanted to but could not divorce from household "
                                        + per.getHousehold().getId() + " because no appropriate vacant dwelling was found.");
                    }
                    IssueCounter.countLackOfDwellingFailedDivorce();
                    return false;
                }

                // divorce
                Household oldHh = householdData.getHouseholdFromId(per.getHousehold().getId());
                Person divorcedPerson = HouseholdUtil.findMostLikelyPartner(per, oldHh);
                divorcedPerson.setRole(PersonRole.SINGLE);
                per.setRole(PersonRole.SINGLE);
                householdData.removePersonFromHousehold(per);

                int newHhId = householdData.getNextHouseholdId();
                Household newHh = hhFactory.createHousehold(newHhId, -1, 0);
                householdData.addHousehold(newHh);
                householdData.addPersonToHousehold(per, newHh);

                // move divorced person into new dwelling
                movesModel.moveHousehold(newHh, -1, newDwellingId);
                if (perId == SiloUtil.trackPp || newHh.getId() == SiloUtil.trackHh ||
                        oldHh.getId() == SiloUtil.trackHh) SiloUtil.trackWriter.println("Person " + perId +
                        " has divorced from household " + oldHh + " and established the new household " +
                        newHhId + ".");
                householdData.addHouseholdThatChanged(oldHh); // consider original household for update in car ownership
                if (Properties.get().main.implementation == Implementation.MUNICH) {
                    carOwnership.simulateCarOwnership(newHh); // set initial car ownership of new household
                }
                return true;
            }
        }
        return false;
    }
}
