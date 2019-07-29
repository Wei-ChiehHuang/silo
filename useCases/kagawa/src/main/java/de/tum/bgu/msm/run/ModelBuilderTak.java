package de.tum.bgu.msm.run;

import de.tum.bgu.msm.container.DataContainer;
import de.tum.bgu.msm.container.ModelContainer;
import de.tum.bgu.msm.data.dwelling.DwellingFactory;
import de.tum.bgu.msm.data.household.HouseholdFactory;
import de.tum.bgu.msm.data.person.PersonFactory;
import de.tum.bgu.msm.models.autoOwnership.CreateCarOwnershipModel;
import de.tum.bgu.msm.models.demography.birth.BirthModelImpl;
import de.tum.bgu.msm.models.demography.birth.DefaultBirthStrategy;
import de.tum.bgu.msm.models.demography.birthday.BirthdayModel;
import de.tum.bgu.msm.models.demography.birthday.BirthdayModelImpl;
import de.tum.bgu.msm.models.demography.death.DeathModel;
import de.tum.bgu.msm.models.demography.death.DeathModelImpl;
import de.tum.bgu.msm.models.demography.death.DefaultDeathStrategy;
import de.tum.bgu.msm.models.demography.divorce.DefaultDivorceStrategy;
import de.tum.bgu.msm.models.demography.divorce.DivorceModel;
import de.tum.bgu.msm.models.demography.divorce.DivorceModelImpl;
import de.tum.bgu.msm.models.demography.driversLicense.DefaultDriversLicenseStrategy;
import de.tum.bgu.msm.models.demography.driversLicense.DriversLicenseModel;
import de.tum.bgu.msm.models.demography.driversLicense.DriversLicenseModelImpl;
import de.tum.bgu.msm.models.demography.education.EducationModel;
import de.tum.bgu.msm.models.demography.education.EducationModelImpl;
import de.tum.bgu.msm.models.demography.employment.EmploymentModel;
import de.tum.bgu.msm.models.demography.employment.EmploymentModelImpl;
import de.tum.bgu.msm.models.demography.leaveParentalHousehold.DefaultLeaveParentalHouseholdStrategy;
import de.tum.bgu.msm.models.demography.leaveParentalHousehold.LeaveParentHhModel;
import de.tum.bgu.msm.models.demography.leaveParentalHousehold.LeaveParentHhModelImpl;
import de.tum.bgu.msm.models.demography.marriage.DefaultMarriageStrategy;
import de.tum.bgu.msm.models.demography.marriage.MarriageModel;
import de.tum.bgu.msm.models.demography.marriage.MarriageModelImpl;
import de.tum.bgu.msm.models.jobmography.JobMarketUpdate;
import de.tum.bgu.msm.models.jobmography.JobMarketUpdateImpl;
import de.tum.bgu.msm.models.realEstate.construction.*;
import de.tum.bgu.msm.models.realEstate.demolition.DefaultDemolitionStrategy;
import de.tum.bgu.msm.models.realEstate.demolition.DemolitionModel;
import de.tum.bgu.msm.models.realEstate.demolition.DemolitionModelImpl;
import de.tum.bgu.msm.models.realEstate.pricing.DefaultPricingStrategy;
import de.tum.bgu.msm.models.realEstate.pricing.PricingModel;
import de.tum.bgu.msm.models.realEstate.pricing.PricingModelImpl;
import de.tum.bgu.msm.models.realEstate.renovation.DefaultRenovationStrategy;
import de.tum.bgu.msm.models.realEstate.renovation.RenovationModel;
import de.tum.bgu.msm.models.realEstate.renovation.RenovationModelImpl;
import de.tum.bgu.msm.models.relocation.migration.InOutMigrationImpl;
import de.tum.bgu.msm.models.relocation.moves.*;
import de.tum.bgu.msm.models.transportModel.TransportModel;
import de.tum.bgu.msm.models.transportModel.matsim.MatsimTransportModel;
import de.tum.bgu.msm.models.transportModel.matsim.ZoneConnectorManager;
import de.tum.bgu.msm.properties.Properties;
import org.matsim.core.config.Config;

public class ModelBuilderTak {
    public static ModelContainer getTakModels(DataContainer dataContainer, Properties properties, Config config) {

            PersonFactory ppFactory = dataContainer.getHouseholdDataManager().getPersonFactory();
            HouseholdFactory hhFactory = dataContainer.getHouseholdDataManager().getHouseholdFactory();
            DwellingFactory ddFactory = dataContainer.getRealEstateDataManager().getDwellingFactory();

            BirthModelImpl birthModel = new BirthModelImpl(dataContainer, ppFactory, properties, new DefaultBirthStrategy());

            BirthdayModel birthdayModel = new BirthdayModelImpl(dataContainer, properties);

            DeathModel deathModel = new DeathModelImpl(dataContainer, properties, new DefaultDeathStrategy());

            MovesModelImpl movesModel = new MovesModelImpl(
                    dataContainer, properties, new DefaultMovesStrategy(),
                    new CarOnlyHousingStrategyImpl(dataContainer,
                                                properties, dataContainer.getTravelTimes(),
                                                new DwellingUtilityStrategyImpl(), new DefaultDwellingProbabilityStrategy(),
                            new SelectRegionStrategyImpl()));

            //TODO reconsider this
            CreateCarOwnershipModel carOwnershipModel = null;

            DivorceModel divorceModel = new DivorceModelImpl(
                    dataContainer, movesModel, carOwnershipModel, hhFactory,
                    properties, new DefaultDivorceStrategy());

            DriversLicenseModel driversLicenseModel = new DriversLicenseModelImpl(dataContainer, properties, new DefaultDriversLicenseStrategy());

            EducationModel educationModel = new EducationModelImpl(dataContainer, properties);

            EmploymentModel employmentModel = new EmploymentModelImpl(dataContainer, properties);

            LeaveParentHhModel leaveParentsModel = new LeaveParentHhModelImpl(dataContainer, movesModel,
                    carOwnershipModel, hhFactory, properties, new DefaultLeaveParentalHouseholdStrategy());

            JobMarketUpdate jobMarketUpdateModel = new JobMarketUpdateImpl(dataContainer, properties);

            ConstructionModel construction = new ConstructionModelImpl(dataContainer, ddFactory,
                    properties, new DefaultConstructionLocationStrategy(), new DefaultConstructionDemandStrategy());


            PricingModel pricing = new PricingModelImpl(dataContainer, properties, new DefaultPricingStrategy());

            RenovationModel renovation = new RenovationModelImpl(dataContainer, properties, new DefaultRenovationStrategy());

            ConstructionOverwrite constructionOverwrite = new ConstructionOverwriteImpl(dataContainer, ddFactory, properties);

            InOutMigrationImpl inOutMigration = new InOutMigrationImpl(dataContainer, employmentModel, movesModel,
                    carOwnershipModel, driversLicenseModel, properties);

            DemolitionModel demolition = new DemolitionModelImpl(dataContainer, movesModel,
                    inOutMigration, properties, new DefaultDemolitionStrategy());

            MarriageModel marriageModel = new MarriageModelImpl(dataContainer, movesModel, inOutMigration,
                    carOwnershipModel, hhFactory, properties, new DefaultMarriageStrategy());

            TransportModel transportModel;
            switch (properties.transportModel.transportModelIdentifier) {
                case MATSIM:
                    transportModel = new MatsimTransportModel(dataContainer, config, properties, null,
                            ZoneConnectorManager.ZoneConnectorMethod.RANDOM);
                    // (MatsimAccessibility) dataContainer.getAccessibility());
                    break;
                case NONE:
                default:
                    transportModel = null;
            }
            final ModelContainer modelContainer = new ModelContainer(
                    birthModel, birthdayModel,
                    deathModel, marriageModel,
                    divorceModel, driversLicenseModel,
                    educationModel, employmentModel,
                    leaveParentsModel, jobMarketUpdateModel,
                    construction, demolition, pricing, renovation,
                    constructionOverwrite, inOutMigration, movesModel, transportModel);
            return modelContainer;
    }
}
