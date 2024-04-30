# KPI Data Requirements and Expectations

Below you can find additional information about data set-up for MATSim models in relation to each of the KPIs produced 
by Gelato. 
In general the following outputs from a MATSim model are expected on top of the MATSim config:

- `output_network.xml.gz`
- `output_transitSchedule.xml.gz`
- `output_transitVehicles.xml.gz`
- `output_households.xml.gz` (can be empty)
- `output_facilities.xml.gz` (can be empty)
- `output_vehicles.xml.gz`
- `output_persons.csv.gz`
- `output_legs.xml.gz`
- `output_trips.xml.gz`
- `drt_vehicles.xml.gz` (for simulations with DRT mode)

Please note, if you are using a simulation configuration that modifies the inherent structure of the above MATsim outputs, that will impact your KPI calculation. For example, if you have used the Eqasim Cutter extension ([link](https://github.com/eqasim-org/eqasim-java/blob/develop/docs/cutting.md)), this will increase the number of trips in your `output_trips.xml.gz` file and relabel some to an `outside` mode or activity. This will impact how the KPI is structured and calculated, and you will need to validate the outputs are as you intend.

## Affordability

### Cost of Travel
This metric relies on `modeParams` being set correctly in the MATSim config and `PersonMoneyEvent`s in the output
events file.

For `modeParams`, make sure you set the `dailyMonetaryConstant` and/or `monetaryDistanceRate` to non-zero values, 
for the modes that make sense (i.e. walking will not actually cost any money to perform)
```xml
<parameterset type="modeParams" >
    <param name="constant" value="0.0" />
    <param name="dailyMonetaryConstant" value="-1.0" />  <!--here-->
    <param name="dailyUtilityConstant" value="0.0" />
    <param name="marginalUtilityOfDistance_util_m" value="0.0" />
    <param name="marginalUtilityOfTraveling_util_hr" value="0.0" />
    <param name="mode" value="car" />
    <param name="monetaryDistanceRate" value="-1.0" />  <!--here-->
</parameterset>
```

If `PersonMoneyEvent`s are present in the events file, they will contribute to the cost of the relevant trip performed 
by an agent.

### Income Information
This metric is strongly dependent on the income information set for agents.
It is expected that your persons data has an `income` column with numeric values or
a `subpopulation` column with string values, where one of the categories is `low income`

## GHG Emissions

You can set your own emissions factors and fuel types for vehicles in the model to be used in the calculation.
You provide them in the MATSim xml files `output_vehicles.xml.gz` or `output_transitVehicles.xml.gz` in the following
way:
(This requires [v2 vehicle file](https://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd))

```xml
<vehicleType id="defaultVehicleType">
    <capacity seats="4" standingRoomInPersons="0">
    </capacity>
    <length meter="7.5"/>
    <width meter="1.0"/>
    <engineInformation>
        <attributes>
            <attribute name="emissionsFactor" class="java.lang.Double">0.222</attribute>
            <attribute name="fuelType" class="java.lang.String">petrol</attribute>
        </attributes>
    </engineInformation>
    <costInformation>
    </costInformation>
    <passengerCarEquivalents pce="1.0"/>
    <networkMode networkMode="car"/>
    <flowEfficiencyFactor factor="1.0"/>
</vehicleType>
```

Otherwise, the factors will default to `"fuelType"="petrol"` and `"emissionsFactor"=0.222` for personal agent's cars, `"fuelType"="ev"` and `"emissionsFactor"=0.076` for
drt vehicles, and `"fuelType"="cng"` and `"emissionsFactor"=1.372` for buses. 
Other PT vehicles are not defaulted and do not contribute to emission calculations.

We recommend setting fuel types and emission factors for LGVs and HGVs, otherwise they will not have an emissions estimate.
The defaults we suggest are:

- LGV: `"fuelType"="petrol"`, `"emissionsFactor"=0.317`
- HGV: `"fuelType"="diesel"`, `"emissionsFactor"=0.761`

The above values are all based on the European Environmental Agency guidebook. 
Car, LGV, HGV, and Buses emissions factors are derived from Tables 3-12 and 3-15 from their [guidebook](https://www.eea.europa.eu/publications/emep-eea-guidebook-2023/part-b-sectoral-guidance-chapters/1-energy/1-a-combustion/1-a-3-b-i/view)
EV emissions factors for cars are from page 32 of their [report](https://www.eea.europa.eu/publications/electric-vehicles-from-life-cycle) estimating life cycle emissions.

# Access to Mobility Services

The $(x, y)$ spatial coordinates are assumed to be in metre-based, distance-preserving projection.
