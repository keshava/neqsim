/*
 * Conductivity.java
 *
 * Created on 1. november 2000, 19:00
 */

package neqsim.physicalProperties.physicalPropertyMethods.liquidPhysicalProperties.diffusivity;

import org.apache.logging.log4j.*;

/**
 *
 * @author  Even Solbraa
 * @version
 */
public class AmineDiffusivity extends SiddiqiLucasMethod{

    private static final long serialVersionUID = 1000;
    static Logger logger = LogManager.getLogger(AmineDiffusivity.class);
    
    /** Creates new Conductivity */
    
    public AmineDiffusivity() {
    }
    
    public AmineDiffusivity(neqsim.physicalProperties.physicalPropertySystem.PhysicalPropertiesInterface liquidPhase) {
        super(liquidPhase);
    }
    
    public void calcEffectiveDiffusionCoeffisients(){
        super.calcEffectiveDiffusionCoeffisients();
        double co2waterdiff = 0.03389*Math.exp(-2213.7/liquidPhase.getPhase().getTemperature())*1e-4; // Tammi (1994) - Pcheco
        double n2owaterdiff = 0.03168*Math.exp(-2209.4/liquidPhase.getPhase().getTemperature())*1e-4;
        double n2oaminediff = 5.533e-8*liquidPhase.getPhase().getTemperature()/Math.pow(liquidPhase.getViscosity(),0.545)*1e-4; // stoke einstein - pacheco
        try{
            double molConsMDEA = liquidPhase.getPhase().getComponent("MDEA").getx()*liquidPhase.getPhase().getDensity()/liquidPhase.getPhase().getMolarMass();
            molConsMDEA += liquidPhase.getPhase().getComponent("MDEA+").getx()*liquidPhase.getPhase().getDensity()/liquidPhase.getPhase().getMolarMass();
            effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("CO2").getComponentNumber()] = n2oaminediff * co2waterdiff/n2owaterdiff;
            effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("MDEA").getComponentNumber()] = 0.0207*Math.exp(-2360.7/liquidPhase.getPhase().getTemperature() - 24.727e-5*molConsMDEA)*1e-4;
        }
        catch(Exception e){
            logger.error("error eff diff calc " + e.toString());
        }
    }
    
    public double calcBinaryDiffusionCoefficient(int i, int j, int method){
        calcEffectiveDiffusionCoeffisients();
        if(liquidPhase.getPhase().getComponent(i).getComponentName().equals("MDEA")) {
            return effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("MDEA").getComponentNumber()];
        } else {
            return effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("CO2").getComponentNumber()];
        }
    }
    
    public double[][] calcDiffusionCoeffisients(int binaryDiffusionCoefficientMethod , int multicomponentDiffusionMethod){
        calcEffectiveDiffusionCoeffisients();
        for(int i = 0; i < liquidPhase.getPhase().getNumberOfComponents(); i++) {
            for(int j = 0; j < liquidPhase.getPhase().getNumberOfComponents(); j++) {
                if(liquidPhase.getPhase().getComponent(i).getComponentName().equals("MDEA")) {
                    binaryDiffusionCoeffisients[i][j]= effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("MDEA").getComponentNumber()];
                } else {
                    binaryDiffusionCoeffisients[i][j]= effectiveDiffusionCoefficient[liquidPhase.getPhase().getComponent("CO2").getComponentNumber()];
                }
            }
        }
        return binaryDiffusionCoeffisients;
    }
    
}
