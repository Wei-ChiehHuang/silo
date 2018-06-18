package de.tum.bgu.msm.syntheticPopulationGenerator.munich.allocation;

import de.tum.bgu.msm.util.js.JavaScriptCalculator;

import java.io.Reader;

public class BaseYearDisabilityJSCalculator extends JavaScriptCalculator<Double> {

    public BaseYearDisabilityJSCalculator (Reader reader){
            super(reader);
        }

    public double calculateBaseYearDisabilityProbability(int personAge, int personSex) {
        return super.calculate("calculateBaseYearDisabilityProbability", personAge, personSex);
    }

    public double probabilityForPhysicalDisability(int personAge, int personSex){
        return super.calculate("probabilityForPhysicalDisability", personAge, personSex);
    }
}
