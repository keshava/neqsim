/*
 * Heater.java
 *
 * Created on 15. mars 2001, 14:17
 */
package neqsim.processSimulation.processEquipment.absorber;

import java.awt.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import neqsim.processSimulation.mechanicalDesign.absorber.AbsorberMechanicalDesign;
import neqsim.processSimulation.processEquipment.stream.Stream;
import neqsim.processSimulation.processEquipment.stream.StreamInterface;
import neqsim.thermo.system.SystemInterface;
import neqsim.thermodynamicOperations.ThermodynamicOperations;

/**
 *
 * @author Even Solbraa
 * @version
 */
public class SimpleTEGAbsorber extends SimpleAbsorber implements AbsorberInterface {

    private static final long serialVersionUID = 1000;

    protected ArrayList streams = new ArrayList(0);
    protected double pressure = 0;
    protected int numberOfInputStreams = 0;
    protected Stream mixedStream;
    protected Stream gasInStream;
    protected Stream solventInStream;
    private Stream gasOutStream;
    private Stream solventOutStream;
    protected String name = "mixer";
    protected ThermodynamicOperations testOps;
    protected Stream outStream;
    private double waterDewPointTemperature = 263.15, dewPressure = 70.0, kwater = 1e-4;

    /**
     * Creates new staticMixer
     */
    public SimpleTEGAbsorber() {
        mechanicalDesign = new AbsorberMechanicalDesign(this);
    }

    public SimpleTEGAbsorber(String name) {
        this.name = name;
        mechanicalDesign = new AbsorberMechanicalDesign(this);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addStream(StreamInterface newStream) {
        streams.add(newStream);

        if (numberOfInputStreams == 0) {
            mixedStream = (Stream) ((StreamInterface) streams.get(0)).clone();
            mixedStream.getThermoSystem().setNumberOfPhases(2);
            mixedStream.getThermoSystem().reInitPhaseType();
            mixedStream.getThermoSystem().init(0);
            mixedStream.getThermoSystem().init(3);
        }

        numberOfInputStreams++;
    }

    public void addGasInStream(StreamInterface newStream) {
        gasInStream = (Stream) newStream;
        gasOutStream = (Stream) newStream.clone();
        addStream(newStream);
    }

    public void addSolventInStream(StreamInterface newStream) {
        solventInStream = (Stream) newStream;
        solventOutStream = (Stream) newStream.clone();
        addStream(newStream);
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public void mixStream() {
        int index = 0;
        String compName = new String();

        for (int k = 1; k < streams.size(); k++) {

            for (int i = 0; i < ((SystemInterface) ((StreamInterface) streams.get(k)).getThermoSystem()).getPhases()[0].getNumberOfComponents(); i++) {

                boolean gotComponent = false;
                String componentName = ((SystemInterface) ((StreamInterface) streams.get(k)).getThermoSystem()).getPhases()[0].getComponents()[i].getName();
                System.out.println("adding: " + componentName);
                int numberOfPhases = ((StreamInterface) streams.get(k)).getThermoSystem().getNumberOfPhases();

                double moles = ((SystemInterface) ((StreamInterface) streams.get(k)).getThermoSystem()).getPhases()[0].getComponents()[i].getNumberOfmoles();
                System.out.println("moles: " + moles + "  " + mixedStream.getThermoSystem().getPhases()[0].getNumberOfComponents());
                for (int p = 0; p < mixedStream.getThermoSystem().getPhases()[0].getNumberOfComponents(); p++) {
                    if (mixedStream.getThermoSystem().getPhases()[0].getComponents()[p].getName().equals(componentName)) {
                        gotComponent = true;
                        index = ((SystemInterface) ((StreamInterface) streams.get(0)).getThermoSystem()).getPhases()[0].getComponents()[p].getComponentNumber();
                        compName = ((SystemInterface) ((StreamInterface) streams.get(0)).getThermoSystem()).getPhases()[0].getComponents()[p].getComponentName();

                    }
                }

                if (gotComponent) {
                    System.out.println("adding moles starting....");
                    mixedStream.getThermoSystem().addComponent(compName, moles);
                    //mixedStream.getThermoSystem().init_x_y();
                    System.out.println("adding moles finished");
                } else {
                    System.out.println("ikke gaa hit");
                    mixedStream.getThermoSystem().addComponent(compName, moles);
                }
            }
        }
        mixedStream.getThermoSystem().init_x_y();
        mixedStream.getThermoSystem().initBeta();
        mixedStream.getThermoSystem().init(2);
    }

    public double guessTemperature() {
        double gtemp = 0;
        for (int k = 0; k < streams.size(); k++) {
            gtemp += ((StreamInterface) streams.get(k)).getThermoSystem().getTemperature() * ((StreamInterface) streams.get(k)).getThermoSystem().getNumberOfMoles() / mixedStream.getThermoSystem().getNumberOfMoles();

        }
        return gtemp;
    }

    public double calcMixStreamEnthalpy() {
        double enthalpy = 0;
        for (int k = 0; k < streams.size(); k++) {
            ((StreamInterface) streams.get(k)).getThermoSystem().init(3);
            enthalpy += ((StreamInterface) streams.get(k)).getThermoSystem().getEnthalpy();
            System.out.println("total enthalpy k : " + ((SystemInterface) ((StreamInterface) streams.get(k)).getThermoSystem()).getEnthalpy());
        }
        System.out.println("total enthalpy of streams: " + enthalpy);
        return enthalpy;
    }

    public Stream getOutStream() {
        return mixedStream;
    }

    public Stream getInStream() {
        return gasInStream;
    }

    public Stream getGasOutStream() {
        return gasOutStream;
    }

    public Stream getLiquidOutStream() {
        return solventOutStream;
    }

    public Stream getSolventInStream() {
        return solventInStream;
    }

    public void runTransient() {
    }

    public double calcEa() {
        double A = mixedStream.getThermoSystem().getPhase(1).getNumberOfMolesInPhase() / mixedStream.getThermoSystem().getPhase(0).getNumberOfMolesInPhase() / kwater;
        absorptionEfficiency = (Math.pow(A, getNumberOfTheoreticalStages()) - A) / (Math.pow(A, getNumberOfTheoreticalStages()) - 1.0);
        return absorptionEfficiency;
    }

    public double calcY0() {
        SystemInterface tempSolventSystem = (SystemInterface) gasInStream.getThermoSystem().clone();
        //tempSolventSystem.addComponent("methane", tempSolventSystem.getPhase(0).getNumberOfMolesInPhase() / 10.0);
        tempSolventSystem.setTemperature(waterDewPointTemperature);
        ThermodynamicOperations testOps3 = new ThermodynamicOperations(tempSolventSystem);
        try{
        testOps3.TPflash();
        }
        catch(Exception e){
        e.printStackTrace();
        }
            System.out.println("water in gas " + (tempSolventSystem.getPhase(0).getComponent("water").getx()));
        //tempSolventSystem.display();
        double y0 = tempSolventSystem.getPhase(0).getComponent("water").getx();
        return y0;
    }

    public double calcNumberOfTheoreticalStages() {
        setNumberOfTheoreticalStages(getStageEfficiency() * getNumberOfStages());
        return getNumberOfTheoreticalStages();
    }

    public double calcNTU(double y0, double y1, double yb, double ymix) {
        double NTU = Math.log((yb - ymix) / (y1 - y0));
        return NTU;
    }

    public void run() {
        try {
            double y0 = 0.0, y1 = 0.0, yN = gasInStream.getThermoSystem().getPhase(0).getComponent("water").getx();
            double absorptionEffiency = 0.0;
            mixedStream.setThermoSystem(((SystemInterface) ((StreamInterface) streams.get(0)).getThermoSystem().clone()));
            mixedStream.getThermoSystem().setNumberOfPhases(2);
            mixedStream.getThermoSystem().reInitPhaseType();
            mixedStream.getThermoSystem().init(0);
            mixStream();
            double enthalpy = calcMixStreamEnthalpy();
            //System.out.println("temp guess " + guessTemperature());
            mixedStream.getThermoSystem().setTemperature(guessTemperature());
            testOps = new ThermodynamicOperations(mixedStream.getThermoSystem());
            testOps.TPflash();
            testOps.PHflash(enthalpy, 0);

            kwater = mixedStream.getThermoSystem().getPhase(0).getComponent("water").getx() / mixedStream.getThermoSystem().getPhase(1).getComponent("water").getx();

            calcNumberOfTheoreticalStages();
            System.out.println("number of theoretical stages " + getNumberOfTheoreticalStages());
            absorptionEffiency = calcEa();

            y0 = calcY0();
            y1 = gasInStream.getThermoSystem().getPhase(0).getComponent("water").getx() - absorptionEffiency * (gasInStream.getThermoSystem().getPhase(0).getComponent("water").getx() - y0);

            double yMean = mixedStream.getThermoSystem().getPhase(0).getComponent("water").getx();
            double molesWaterToMove = (yMean - y1) * mixedStream.getThermoSystem().getPhase(0).getNumberOfMolesInPhase();
            System.out.println("mole water to move " + molesWaterToMove);

            Stream stream = (Stream) mixedStream.clone();
            stream.setName("test");
            stream.getThermoSystem().addComponent("water", -molesWaterToMove, 0);
            stream.getThermoSystem().addComponent("water", molesWaterToMove, 1);
            stream.getThermoSystem().init_x_y();
            stream.getThermoSystem().initBeta();
            stream.getThermoSystem().init(2);
            mixedStream = stream;
            stream.getThermoSystem().display();

            SystemInterface tempSystem = (SystemInterface) mixedStream.getThermoSystem().clone();
            SystemInterface gasTemp = tempSystem.phaseToSystem(tempSystem.getPhases()[0]);
            gasOutStream.setThermoSystem(gasTemp);

            tempSystem = (SystemInterface) mixedStream.getThermoSystem().clone();
            SystemInterface liqTemp = tempSystem.phaseToSystem(tempSystem.getPhases()[1]);
            solventOutStream.setThermoSystem(liqTemp);


            setNTU(calcNTU(y0, y1, gasInStream.getThermoSystem().getPhase(0).getComponent("water").getx(), yMean));
            System.out.println("NTU " + getNTU());

            double Ks = 0.055;
            getSolventOutStream().getThermoSystem().initPhysicalProperties();
            getGasOutStream().getThermoSystem().initPhysicalProperties();
            double vtemp = Ks * Math.sqrt((getSolventOutStream().getThermoSystem().getPhase(0).getPhysicalProperties().getDensity() - getGasOutStream().getThermoSystem().getPhase(0).getPhysicalProperties().getDensity()) / getSolventOutStream().getThermoSystem().getPhase(0).getPhysicalProperties().getDensity());
            double d = Math.sqrt(4.0 * getGasOutStream().getMolarRate() * getGasOutStream().getThermoSystem().getPhase(0).getMolarMass() / getGasOutStream().getThermoSystem().getPhase(0).getPhysicalProperties().getDensity() / 3.14 / vtemp);
            System.out.println("diameter " + d);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void displayResult() {
        SystemInterface thermoSystem = mixedStream.getThermoSystem();
        DecimalFormat nf = new DecimalFormat();
        nf.setMaximumFractionDigits(5);
        nf.applyPattern("#.#####E0");


        JDialog dialog = new JDialog(new JFrame(), "Results from TPflash");
        Container dialogContentPane = dialog.getContentPane();
        dialogContentPane.setLayout(new FlowLayout());

        thermoSystem.initPhysicalProperties();
        String[][] table = new String[50][5];
        String[] names = {"", "Phase 1", "Phase 2", "Phase 3", "Unit"};
        table[0][0] = "";
        table[0][1] = "";
        table[0][2] = "";
        table[0][3] = "";
        StringBuffer buf = new StringBuffer();
        FieldPosition test = new FieldPosition(0);

        for (int i = 0; i < thermoSystem.getNumberOfPhases(); i++) {
            for (int j = 0; j < thermoSystem.getPhases()[0].getNumberOfComponents(); j++) {
                table[j + 1][0] = thermoSystem.getPhases()[0].getComponents()[j].getName();
                buf = new StringBuffer();
                table[j + 1][i + 1] = nf.format(thermoSystem.getPhases()[i].getComponents()[j].getx(), buf, test).toString();
                table[j + 1][4] = "[-]";
            }
            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 2][0] = "Density";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 2][i + 1] = nf.format(thermoSystem.getPhases()[i].getPhysicalProperties().getDensity(), buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 2][4] = "[kg/m^3]";

            //  Double.longValue(thermoSystem.getPhases()[i].getBeta());

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 3][0] = "PhaseFraction";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 3][i + 1] = nf.format(thermoSystem.getPhases()[i].getBeta(), buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 3][4] = "[-]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 4][0] = "MolarMass";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 4][i + 1] = nf.format(thermoSystem.getPhases()[i].getMolarMass() * 1000, buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 4][4] = "[kg/kmol]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 5][0] = "Cp";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 5][i + 1] = nf.format((thermoSystem.getPhases()[i].getCp() / (thermoSystem.getPhases()[i].getNumberOfMolesInPhase() * thermoSystem.getPhases()[i].getMolarMass() * 1000)), buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 5][4] = "[kJ/kg*K]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 7][0] = "Viscosity";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 7][i + 1] = nf.format((thermoSystem.getPhases()[i].getPhysicalProperties().getViscosity()), buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 7][4] = "[kg/m*sec]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 8][0] = "Conductivity";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 8][i + 1] = nf.format(thermoSystem.getPhases()[i].getPhysicalProperties().getConductivity(), buf, test).toString();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 8][4] = "[W/m*K]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 10][0] = "Pressure";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 10][i + 1] = Double.toString(thermoSystem.getPhases()[i].getPressure());
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 10][4] = "[bar]";

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 11][0] = "Temperature";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 11][i + 1] = Double.toString(thermoSystem.getPhases()[i].getTemperature());
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 11][4] = "[K]";
            Double.toString(thermoSystem.getPhases()[i].getTemperature());

            buf = new StringBuffer();
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 13][0] = "Stream";
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 13][i + 1] = name;
            table[thermoSystem.getPhases()[0].getNumberOfComponents() + 13][4] = "-";
        }

        JTable Jtab = new JTable(table, names);
        JScrollPane scrollpane = new JScrollPane(Jtab);
        dialogContentPane.add(scrollpane);
        dialog.pack();
        dialog.setVisible(true);
    }

    public String getName() {
        return name;
    }

    public double getWaterDewPointTemperature() {
        return waterDewPointTemperature;
    }

    public void setWaterDewPointTemperature(double waterDewPointTemperature, double dewPressure) {
        this.waterDewPointTemperature = waterDewPointTemperature;
        this.dewPressure = dewPressure;
    }

    public void setGasOutStream(Stream gasOutStream) {
        this.gasOutStream = gasOutStream;
    }

    public Stream getSolventOutStream() {
        return solventOutStream;
    }

    public void setSolventOutStream(Stream solventOutStream) {
        this.solventOutStream = solventOutStream;
    }
}
