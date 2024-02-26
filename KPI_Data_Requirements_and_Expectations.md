# KPI Data Requirements and Expectations

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

Otherwise, the factors will default to `"fuelType"="petrol"` and `"emissionsFactor"=0.222` for personal agent's cars and
drt vehicles, and `"fuelType"="cng"` and `"emissionsFactor"=1.372` for buses. 
Other PT vehicles are not defaulted and do not contribute to emission calculations.

We recommend setting fuel types and emission factors for LGVs and HGVs, otherwise they may will regarded as personal cars.
The defaults we use are:

- LGV: `"fuelType"="petrol"`, `"emissionsFactor"=0.317`
- HGV: `"fuelType"="diesel"`, `"emissionsFactor"=0.761`
