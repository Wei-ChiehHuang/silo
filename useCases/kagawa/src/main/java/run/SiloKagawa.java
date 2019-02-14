package run;

import de.tum.bgu.msm.Implementation;
import de.tum.bgu.msm.SiloModel;
import de.tum.bgu.msm.properties.Properties;
import de.tum.bgu.msm.utils.SiloUtil;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class SiloKagawa {

    private static Logger logger = Logger.getLogger(SiloKagawa.class);

    public static void main(String[] args) {

        //we choose temporarily implementation munich
        Properties properties = SiloUtil.siloInitialization(Implementation.MUNICH, args[0]);

        Config config = null;
        if (args.length > 1 && args[1] != null) {

            config = ConfigUtils.loadConfig(args[1]);
        }
        logger.info("Starting SILO land use model for the Munich Metropolitan Area");
        SiloModel model = new SiloModel(config, properties);
        model.runModel();
        logger.info("Finished SILO.");
    }
}
