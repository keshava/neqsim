/*
 * Stream.java
 *
 * Created on 12. mars 2001, 13:11
 */
package neqsim.processSimulation.processEquipment.stream;

import neqsim.processSimulation.processEquipment.ProcessEquipmentBaseClass;
import neqsim.thermo.system.SystemInterface;
import neqsim.thermodynamicOperations.ThermodynamicOperations;

/**
 *
 * @author Even Solbraa
 * @version
 */
public class Stream extends ProcessEquipmentBaseClass implements StreamInterface, Cloneable {

    /**
     * @return the gasQuality
     */
    public double getGasQuality() {
        return gasQuality;
    }

    /**
     * @param gasQuality the gasQuality to set
     */
    public void setGasQuality(double gasQuality) {
        this.gasQuality = gasQuality;
    }

    private static final long serialVersionUID = 1000;

    protected SystemInterface thermoSystem;
    protected ThermodynamicOperations thermoOps;
    protected int streamNumber = 0;
    protected static int numberOfStreams = 0;
    private double gasQuality = 0.5;
    protected String name = new String();
    protected StreamInterface stream = null;

    /**
     * Creates new Stream
     */
    public Stream() {
    }

    public Stream(SystemInterface thermoSystem) {
        this.thermoSystem = thermoSystem;
        numberOfStreams++;
        streamNumber = numberOfStreams;
    }

    public Stream(StreamInterface stream) {
        this.stream = stream;
        thermoSystem = stream.getThermoSystem();
        numberOfStreams++;
        streamNumber = numberOfStreams;
    }

    public double getHydrateEquilibriumTemperature() {
        if (!thermoSystem.getPhase(0).hasComponent("water")) {
            System.out.println("ny hydrate: no water in stream: " + name);
            return 0.0;
        }
        try {
            thermoSystem.setHydrateCheck(true);
            thermoOps = new ThermodynamicOperations(thermoSystem);
            thermoOps.hydrateFormationTemperature();
            return thermoSystem.getTemperature();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public double getSolidFormationTemperature(String solidName) {

        try {
            if (solidName.equals("hydrate")) {
                thermoSystem.setHydrateCheck(true);
                thermoOps = new ThermodynamicOperations(thermoSystem);
                thermoOps.hydrateFormationTemperature();
            } else {
                thermoSystem.setSolidPhaseCheck(false);
                thermoSystem.setSolidPhaseCheck(solidName);
                thermoOps = new ThermodynamicOperations(thermoSystem);
                thermoOps.freezingPointTemperatureFlash();
            }
            return thermoSystem.getTemperature();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public Stream(String name, SystemInterface thermoSystem) {
        this.thermoSystem = thermoSystem;
        this.name = name;
        numberOfStreams++;
        streamNumber = numberOfStreams;
    }

    public Object clone() {
        Stream clonedSystem = null;
        try {
            clonedSystem = (Stream) super.clone();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        clonedSystem.thermoSystem = (SystemInterface) getThermoSystem().clone();
        return clonedSystem;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return thermoSystem.getTemperature();
    }

    public double getPressure() {
        return thermoSystem.getPressure();
    }

    public double getMolarRate() {
        return thermoSystem.getTotalNumberOfMoles();
    }

    public void setThermoSystem(SystemInterface thermoSystem) {
        this.thermoSystem = thermoSystem;
    }

    public void setThermoSystemFromPhase(SystemInterface thermoSystem, String phaseTypeName) {
        if (phaseTypeName.equals("liquid")) {
            if (thermoSystem.hasPhaseType("oil") && thermoSystem.hasPhaseType("aqueous")) {
                this.thermoSystem = thermoSystem.phaseToSystem(thermoSystem.getPhaseNumberOfPhase("oil"), thermoSystem.getPhaseNumberOfPhase("aqueous"));
            } else if (thermoSystem.hasPhaseType("oil")) {
                this.thermoSystem = thermoSystem.phaseToSystem(thermoSystem.getPhaseNumberOfPhase("oil"));
            } else if (thermoSystem.hasPhaseType("aqueous")) {
                this.thermoSystem = thermoSystem.phaseToSystem(thermoSystem.getPhaseNumberOfPhase("aqueous"));
            } else {
                System.out.println("no phase of type " + phaseTypeName);
                System.out.println("...returning empty system ");
                setEmptyThermoSystem(thermoSystem);
            }
            return;
        }
        if (thermoSystem.hasPhaseType(phaseTypeName)) {
            this.thermoSystem = thermoSystem.phaseToSystem(phaseTypeName);
        } else {
            System.out.println("no phase of type " + phaseTypeName);
            System.out.println("...returning empty system ");
            setEmptyThermoSystem(thermoSystem);
        }
    }

    public void setEmptyThermoSystem(SystemInterface thermoSystem) {
        this.thermoSystem = thermoSystem.getEmptySystemClone();
        this.thermoSystem.setNumberOfPhases(0);
    }

    public SystemInterface getThermoSystem() {
        return this.thermoSystem;
    }

    public void setFlowRate(double flowrate, String unit) {
    	this.getFluid().setTotalFlowRate(flowrate, unit);
    }
    
    public void setPressure(double pressure, String unit) {
    	getFluid().setPressure(pressure, unit);
    }
    
    public void setTemperature(double temperature, String unit) {
    	getFluid().setTemperature(temperature, unit);
    }
    
    public double getFlowRate(String unit) {
    	return this.getFluid().getFlowRate(unit);
    }
    
    public double getPressure(String unit) {
    	return getFluid().getPressure(unit);
    }
    
    public double getTemperature(String unit) {
    	return getFluid().getTemperature(unit);
    }
    
    public void run() {
        //System.out.println("start flashing stream... " + streamNumber);
        if (stream != null) {
            thermoSystem = (SystemInterface) this.stream.getThermoSystem().clone();
        }
        thermoOps = new ThermodynamicOperations(thermoSystem);

        if (specification.equals("TP")) {
            thermoOps.TPflash();
        } else if (specification.equals("dewP")) {
            try {
                thermoOps.dewPointTemperatureFlash();
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else if (specification.equals("dewT")) {
            try {
                thermoOps.dewPointPressureFlash();
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else if (specification.equals("gas quality")) {
            try {
                thermoSystem.init(0);
                thermoSystem.init(2);
                double gasEnthalpy = thermoSystem.getPhase(0).getEnthalpy();
                double liquidEnthalpy = thermoSystem.getPhase(1).getEnthalpy();
                
                double enthalpySpec = getGasQuality()*gasEnthalpy+ (1.0-getGasQuality())*liquidEnthalpy;
                thermoOps.PHflash(enthalpySpec);
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else if (specification.equals("bubP")) {
            try {
                thermoOps.bubblePointTemperatureFlash();
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else if (specification.equals("bubT")) {
            try {
                thermoOps.bubblePointPressureFlash(false);
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else if (specification.equals("PH")) {
            try {
                thermoOps.PHflash(getThermoSystem().getEnthalpy(), 0);
            } catch (Exception e) {
                e.printStackTrace();
                thermoOps.TPflash();
            }
        } else {
            thermoOps.TPflash();
        }

        thermoSystem.initProperties();
        //System.out.println("number of phases: " + thermoSystem.getNumberOfPhases());
        //System.out.println("beta: " + thermoSystem.getBeta());
    }

    public void displayResult() {
        thermoSystem.display(name);
    }

    public String getName() {
        return name;
    }

    public void runTransient(double dt) {
        run();
    }

    public void flashStream() {
        ThermodynamicOperations ops = new ThermodynamicOperations(thermoSystem);
        ops.TPflash();
    }

    public void phaseEnvelope() {
        ThermodynamicOperations ops = new ThermodynamicOperations(thermoSystem);
        ops.setRunAsThread(true);
        ops.calcPTphaseEnvelope(true);
        boolean isFinished = ops.waitAndCheckForFinishedCalculation(10000);
        ops.displayResult();
        //ops.getJfreeChart();
    }

    public String[][] reportResults() {
        return thermoSystem.getResultTable();
    }
}
