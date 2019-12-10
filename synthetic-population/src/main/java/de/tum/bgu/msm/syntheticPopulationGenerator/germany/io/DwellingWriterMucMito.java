package de.tum.bgu.msm.syntheticPopulationGenerator.germany.io;

import de.tum.bgu.msm.data.household.Household;
import de.tum.bgu.msm.data.household.HouseholdDataManager;
import de.tum.bgu.msm.data.household.HouseholdMuc;
import de.tum.bgu.msm.io.output.HouseholdWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

public class DwellingWriterMucMito implements HouseholdWriter {

    private final HouseholdDataManager householdData;
    private final static Logger logger = Logger.getLogger(DwellingWriterMucMito.class);

    public DwellingWriterMucMito(HouseholdDataManager householdData) {
        this.householdData = householdData;
    }
    @Override
    public void writeHouseholds(String path) {
        logger.info("  Writing household file to " + path);
        PrintWriter pwh = SiloUtil.openFileForSequentialWriting(path, false);
        pwh.println("id,hhId,zone,coordX,coordY");
        for (Household hh : householdData.getHouseholds()) {
            if (hh.getId() == SiloUtil.trackHh) {
                SiloUtil.trackingFile("Writing hh " + hh.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(hh.toString());
            }
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(hh.getId());
            pwh.print(",");
            pwh.print(((HouseholdMuc) hh).getAdditionalAttributes().get("zone"));
            pwh.print(",");
            pwh.print(((HouseholdMuc) hh).getAdditionalAttributes().get("coordX"));
            pwh.print(",");
            pwh.println(((HouseholdMuc) hh).getAdditionalAttributes().get("coordY"));
        }
        pwh.close();
    }
}
