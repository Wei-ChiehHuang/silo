package de.tum.bgu.msm.io;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.data.dwelling.Dwelling;
import de.tum.bgu.msm.data.person.Person;
import de.tum.bgu.msm.io.output.PersonWriter;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

public class PersonWriterMuc implements PersonWriter {

    private final static Logger logger = Logger.getLogger(PersonWriterMuc.class);

    private final DataContainer dataContainer;

    public PersonWriterMuc(DataContainer dataContainer) {
        this.dataContainer = dataContainer;
    }

    @Override
    public void writePersons(String path) {

        logger.info("  Writing person file to " + path);
        PrintWriter pwp = SiloUtil.openFileForSequentialWriting(path, false);
        pwp.print("id,hhid,age,gender,relationShip,race,occupation,driversLicense,workplace,income");
        pwp.print(",");
        pwp.print("nationality");
        pwp.print(",");
        pwp.print("education");
        pwp.print(",");
        pwp.print("homeZone");
        pwp.print(",");
        pwp.print("disability");
        pwp.print(",");
        pwp.print("schoolId");

        pwp.println();
        for (Person pp : dataContainer.getHouseholdDataManager().getPersons()) {
            pwp.print(pp.getId());
            pwp.print(",");
            pwp.print(pp.getHousehold().getId());
            pwp.print(",");
            pwp.print(pp.getAge());
            pwp.print(",");
            pwp.print(pp.getGender().getCode());
            pwp.print(",\"");
            String role = pp.getRole().toString();
            pwp.print(role);
            pwp.print("\",\"");
            pwp.print(pp.getRace());
            pwp.print("\",");
            pwp.print(pp.getOccupation().getCode());
            pwp.print(",");
            pwp.print(pp.hasDriverLicense());

            pwp.print(",");
            final int jobId = pp.getJobId();
            pwp.print(pp.getJobId());
            pwp.print(",");
            pwp.print(pp.getIncome());
            pwp.print(",");
            pwp.print(pp.getNationality().toString());
            pwp.print(",");
            Dwelling dd = dataContainer.getRealEstateDataManager().getDwelling(pp.getHousehold().getDwellingId());
            pwp.print(dd.getZoneId());
            pwp.print(",");
            pwp.print(0);
            pwp.print(",");
            pwp.print(pp.getSchoolId());


            pwp.println();


            if (pp.getId() == SiloUtil.trackPp) {
                SiloUtil.trackingFile("Writing pp " + pp.getId() + " to micro data file.");
                SiloUtil.trackWriter.println(pp.toString());
            }
        }
        pwp.close();


    }
}