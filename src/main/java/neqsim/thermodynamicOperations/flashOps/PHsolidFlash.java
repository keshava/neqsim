/*
 * Copyright 2018 ESOL.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * PHflash.java
 *
 * Created on 8. mars 2001, 10:56
 */
package neqsim.thermodynamicOperations.flashOps;

import neqsim.thermo.system.SystemInterface;
import neqsim.thermodynamicOperations.ThermodynamicOperations;
import org.apache.logging.log4j.*;

/**
 *
 * @author  even solbraa
 * @version
 */
public class PHsolidFlash extends Flash implements java.io.Serializable {

    private static final long serialVersionUID = 1000;
    static Logger logger = LogManager.getLogger(PHsolidFlash.class);

    Flash tpFlash;
    int refluxPhase = 0;
    double enthalpyspec = 0.5;

    /** Creates new PHflash */
    public PHsolidFlash() {
    }

    public PHsolidFlash(SystemInterface system, double ent) {
        this.system = system;
        this.tpFlash = new TPflash(this.system ,true);
        this.enthalpyspec = ent;
    }

    public void run() {
        //logger.info("enthalpy: " + system.getEnthalpy());
        double err = 0;
        int iter = 0;
        double f_func = 0.0, f_func_old = 0.0, df_func_dt = 0, t_old = 0, t_oldold = 0.0;
        ThermodynamicOperations ops = new ThermodynamicOperations(system);
        tpFlash.run();
        double dt = 10;
        do {
            iter++;

            f_func_old = f_func;
            t_oldold = t_old;
            t_old = system.getTemperature();
            system.init(3);
            f_func = enthalpyspec - system.getEnthalpy();
            logger.info("entalp diff " + f_func);
            df_func_dt = (f_func - f_func_old) / (t_old - t_oldold);

            err = Math.abs(f_func);

            if (iter < 2) {
                if (f_func > 0) {
                    system.setTemperature(system.getTemperature() + 0.1);
                } else if (f_func < 0) {
                    system.setTemperature(system.getTemperature() - 0.1);
                }
            } else {
                dt = f_func / df_func_dt;
                if (Math.abs(dt) > 2.0) {
                    dt = Math.signum(dt) * 2.0;
                }
                system.setTemperature(system.getTemperature() - 0.8*dt);
            }
            tpFlash.run();

//            logger.info("temp " + system.getTemperature() + " err " + err);
        } while (Math.abs(dt) > 1e-8 && iter<200);

    }

    public org.jfree.chart.JFreeChart getJFreeChart(String name) {
        return null;
    }
}
