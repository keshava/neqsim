/*
 * FrictionFactorBaseClass.java
 *
 * Created on 12. juni 2001, 19:58
 */

package neqsim.fluidMechanics.flowNode.fluidBoundary.interphaseTransportCoefficient;

import neqsim.MathLib.generalMath.GeneralMath;
import neqsim.fluidMechanics.flowNode.FlowNodeInterface;

/**
 *
 * @author  esol
 * @version
 */
public class InterphaseTransportCoefficientBaseClass implements InterphaseTransportCoefficientInterface{

    private static final long serialVersionUID = 1000;
    
    /** Creates new FrictionFactorBaseClass
     * All frictionfactors are the fanning frictionfactor.
     */
    
    public InterphaseTransportCoefficientBaseClass() {
    }
    
    public InterphaseTransportCoefficientBaseClass(FlowNodeInterface node) {
        //      flowNode = node;
    }
    
    
    public double calcWallFrictionFactor(FlowNodeInterface node){
        if(Math.abs(node.getReynoldsNumber()) < 2000) {
            return 64.0/node.getReynoldsNumber();
        } else {
            return Math.pow((1.0/(-1.8*GeneralMath.log10(6.9/node.getReynoldsNumber()+Math.pow(node.getGeometry().getRelativeRoughnes()/3.7,1.11)))),2.0);
        }
    }
    
    public double calcWallFrictionFactor(int phase, FlowNodeInterface node){
        if(Math.abs(node.getReynoldsNumber())< 2000) {
            return 64.0/node.getReynoldsNumber(phase);
        } else {
            return Math.pow((1.0/(-1.8*GeneralMath.log10(6.9/node.getReynoldsNumber(phase)+Math.pow(node.getGeometry().getRelativeRoughnes()/3.7,1.11)))),2.0);
        }
        
    }
    
    public double calcInterPhaseFrictionFactor(int phase, FlowNodeInterface node){
        return (1.0 + 75.0*node.getPhaseFraction(1))*calcWallFrictionFactor(0, node);
    }
    
    public double calcWallHeatTransferCoefficient(int phase, double prandtlNumber, FlowNodeInterface node){
        return 0;
    }
    
    public double calcWallHeatTransferCoefficient(int phase, FlowNodeInterface node){
        return this.calcWallHeatTransferCoefficient(phase,node.getPrandtlNumber(phase),node);
    }
    
    public double calcWallMassTransferCoefficient(int phase, double schmidtNumber,  FlowNodeInterface node){
        return 0;
    }
    
    public double calcInterphaseHeatTransferCoefficient(int phase, double prandtlNumber, FlowNodeInterface node){
        return 0;
    }
    
    public double calcInterphaseMassTransferCoefficient(int phase, double schmidt, FlowNodeInterface node){
        return 0;
    }
    
    
    
    
    
    
    
}
