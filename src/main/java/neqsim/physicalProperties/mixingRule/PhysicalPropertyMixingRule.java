/*
 * PhysicalPropertyMixingRule.java
 *
 * Created on 2. august 2001, 13:42
 */
package neqsim.physicalProperties.mixingRule;

import java.util.*;
import neqsim.thermo.ThermodynamicConstantsInterface;
import neqsim.thermo.phase.PhaseInterface;
import org.apache.logging.log4j.*;

/**
 *
 * @author esol
 * @version
 */
public class PhysicalPropertyMixingRule implements Cloneable, PhysicalPropertyMixingRuleInterface, ThermodynamicConstantsInterface, java.io.Serializable {

    private static final long serialVersionUID = 1000;
    static Logger logger = LogManager.getLogger(PhysicalPropertyMixingRule.class);

    public double[][] Gij;

    /**
     * Creates new PhysicalPropertyMixingRule
     */
    public PhysicalPropertyMixingRule() {
    }

    public Object clone() {
        PhysicalPropertyMixingRule mixRule = null;

        try {
            mixRule = (PhysicalPropertyMixingRule) super.clone();
        } catch (Exception e) {
            logger.error("Cloning failed.", e);
        }
        
        double[][] Gij2 = Gij.clone();
        for (int i = 0; i < Gij2.length; i++) {
            Gij2[i] = Gij2[i].clone();
        }
        mixRule.Gij = Gij2;
        return mixRule;
    }

    public double getViscosityGij(int i, int j) {
        return Gij[i][j];
    }

    public void setViscosityGij(double val, int i, int j) {
        Gij[i][j] = val;
    }

    public PhysicalPropertyMixingRuleInterface getPhysicalPropertyMixingRule() {
        return this;
    }

    public void initMixingRules(PhaseInterface phase) {
        // logger.info("reading mix Gij viscosity..");
        Gij = new double[phase.getNumberOfComponents()][phase.getNumberOfComponents()];
        neqsim.util.database.NeqSimDataBase database = null;
        java.sql.ResultSet dataSet = null;

        database = new neqsim.util.database.NeqSimDataBase();
        for (int l = 0; l < phase.getNumberOfComponents(); l++) {
        	 if (phase.getComponent(l).isIsTBPfraction()  || phase.getComponent(l).getIonicCharge() != 0) {
                 break;
             }
            String component_name = phase.getComponents()[l].getComponentName();
            for (int k = l; k < phase.getNumberOfComponents(); k++) {
                if (k == l || phase.getComponent(k).getIonicCharge() != 0 || phase.getComponent(k).isIsTBPfraction()) {
                    break;
                } else {
                    try {
                        dataSet = database.getResultSet("SELECT gijvisc FROM inter WHERE (COMP1='" + component_name + "' AND COMP2='" + phase.getComponents()[k].getComponentName() + "') OR (COMP1='" + phase.getComponents()[k].getComponentName() + "' AND COMP2='" + component_name + "')");
                        if (dataSet.next()) {
                            Gij[l][k] = Double.parseDouble(dataSet.getString("gijvisc"));
                        } else {
                            Gij[l][k] = 0.0;
                        }
                        Gij[k][l] = Gij[l][k];
                    } catch (Exception e) {
                        logger.error("err in phys prop.....");
                        String err = e.toString();
                        logger.error(err);
                    } finally {
                        try {
                        	   if (dataSet != null) {
                                   dataSet.close();
                               }
                           } catch (Exception e) {
                               logger.error("err closing dataSet in physical property mixing rule...", e);
                           }
                    }
                }
            }
        }
        try {
            if (database.getStatement() != null) {
                database.getStatement().close();
            }
            if (database.getConnection() != null) {
                database.getConnection().close();
            }
        } catch (Exception e) {
            logger.error("error closing database.....", e);
        }
    }
}
