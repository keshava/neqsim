/*
 * ThrottelValve.java
 *
 * Created on 22. august 2001, 17:20
 */
package neqsim.processSimulation.processEquipment.compressor;

import java.awt.*;
import java.text.*;
import javax.swing.*;

import org.apache.logging.log4j.*;

import neqsim.processSimulation.mechanicalDesign.compressor.CompressorMechanicalDesign;
import neqsim.processSimulation.processEquipment.ProcessEquipmentBaseClass;
import neqsim.processSimulation.processEquipment.stream.StreamInterface;
import neqsim.thermo.system.SystemInterface;
import neqsim.thermodynamicOperations.ThermodynamicOperations;

/**
 *
 * @author esol
 * @version
 */
public class Compressor extends ProcessEquipmentBaseClass implements CompressorInterface {

	private static final long serialVersionUID = 1000;
	static Logger logger = LogManager.getLogger(Compressor.class);
	String name = new String();
	public SystemInterface thermoSystem;
	public ThermodynamicOperations thermoOps;
	public StreamInterface inletStream;
	public StreamInterface outStream;
	private double outTemperature = 298.15;
	private boolean useOutTemperature = false;
	public double dH = 0.0;
	public double inletEnthalpy = 0;
	public double pressure = 0.0;
	private int speed = 3000;
	public double isentropicEfficiency = 1.0, polytropicEfficiency = 1.0;
	public boolean usePolytropicCalc = false;
	public boolean powerSet = false;
	private CompressorChartInterface compressorChart = new CompressorChart();
	private AntiSurge antiSurge = new AntiSurge();
	private double polytropicHead = 0;
	private double polytropicFluidHead = 0, polytropicHeadMeter = 0.0;
	private double polytropicExponent = 0;
	private int numberOfCompresorCalcSteps = 40;
	private boolean useRigorousPolytropicMethod = false;

	/**
	 * Creates new ThrottelValve
	 */
	public Compressor() {
		mechanicalDesign = new CompressorMechanicalDesign(this);
	}

	public Compressor(StreamInterface inletStream) {
		this();
		setInletStream(inletStream);
	}

	public Compressor(String name, StreamInterface inletStream) {
		this();
		this.name = name;
		setInletStream(inletStream);
	}

	public Compressor(boolean interpolateMapLookup) {
		this();
		if (interpolateMapLookup) {
			compressorChart = new CompressorChartAlternativeMapLookup();
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setInletStream(StreamInterface inletStream) {
		this.inletStream = inletStream;
		try {
			this.outStream = (StreamInterface) inletStream.clone();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void solveAntiSurge() {
		if (getAntiSurge().isActive()) {
			// ....
		}
	}

	public void setOutletPressure(double pressure) {
		this.pressure = pressure;
	}

	public double getOutletPressure() {
		return pressure;
	}

	public double getEnergy() {
		return getTotalWork();
	}

	public double getPower() {
		return getTotalWork();
	}

	public void setPower(double p) {
		powerSet = true;
		dH = p;
	}

	public StreamInterface getOutStream() {
		return outStream;
	}

	/**
	 * Calculates polytropic or isentropic efficiency
	 *
	 */
	public double solveEfficiency(double outTemperature) {
		double funk = 0.0, funkOld = 0.0;
		double newPoly;
		double dfunkdPoly = 100.0, dPoly = 100.0, oldPoly = outTemperature;
		useOutTemperature = false;
		run();
		useOutTemperature = true;
		int iter = 0;
		boolean useOld = usePolytropicCalc;
		// usePolytropicCalc = true;
		// System.out.println("use polytropic " + usePolytropicCalc);
		do {
			iter++;
			funk = getThermoSystem().getTemperature() - outTemperature;
			dfunkdPoly = (funk - funkOld) / dPoly;
			newPoly = polytropicEfficiency - funk / dfunkdPoly;
			if (iter <= 1) {
				newPoly = polytropicEfficiency + 0.01;
			}
			oldPoly = polytropicEfficiency;
			polytropicEfficiency = newPoly;
			isentropicEfficiency = newPoly;
			dPoly = polytropicEfficiency - oldPoly;
			funkOld = funk;
			useOutTemperature = false;
			run();
			useOutTemperature = true;
			// System.out.println("temperature compressor " +
			// getThermoSystem().getTemperature() + " funk " + funk + " polytropic " +
			// polytropicEfficiency);
		} while ((Math.abs((getThermoSystem().getTemperature() - outTemperature)) > 1e-5 || iter < 3) && (iter < 50));
		usePolytropicCalc = useOld;
		return newPoly;
	}

	public double findOutPressure(double hinn, double hout, double polytropicEfficiency) {
		double entropy = getThermoSystem().getEntropy();
		getThermoSystem().setPressure(getThermoSystem().getPressure() + 1.0);

		// System.out.println("entropy inn.." + entropy);
		thermoOps.PSflash(entropy);

		double houtGuess = hinn + dH / polytropicEfficiency;
		thermoOps.PHflash(houtGuess, 0);
		System.out.println("TEMPERATURE .." + getThermoSystem().getTemperature());
		return getThermoSystem().getPressure();
	}

	public void run() {
		// System.out.println("compressor running..");
		thermoSystem = (SystemInterface) inletStream.getThermoSystem().clone();
		thermoOps = new ThermodynamicOperations(getThermoSystem());
		getThermoSystem().init(3);
		double presinn = getThermoSystem().getPressure();
		double hinn = getThermoSystem().getEnthalpy();
		double densInn = getThermoSystem().getDensity();
		double entropy = getThermoSystem().getEntropy();
		inletEnthalpy = hinn;
		boolean surgeCheck = false;
		double orginalMolarFLow = thermoSystem.getTotalNumberOfMoles();
		double fractionAntiSurge = 0.0;
		double kappa = 0.0;
		if (useOutTemperature) {
			if (useRigorousPolytropicMethod) {
				solveEfficiency(outTemperature);
				polytropicHead = getPower() / getThermoSystem().getFlowRate("kg/sec") / 1000.0
						* getPolytropicEfficiency();
				polytropicFluidHead = polytropicHead;
				polytropicHeadMeter = polytropicFluidHead * 1000.0 / 9.81;
				return;
			} else {
				double MW = thermoSystem.getMolarMass();

				thermoSystem.setPressure(getOutletPressure());
				thermoOps.PSflash(entropy);
				thermoSystem.initPhysicalProperties("density");
				double densOutIsentropic = thermoSystem.getDensity("kg/m3");
				double enthalpyOutIsentropic = thermoSystem.getEnthalpy();
				//System.out.println("temperature isentropic "+thermoSystem.getTemperature());
				thermoSystem.setTemperature(outTemperature);
				thermoOps.TPflash();
				thermoSystem.init(2);
				thermoSystem.initPhysicalProperties("density");
				double outEnthalpy = thermoSystem.getEnthalpy();
				dH = outEnthalpy - inletEnthalpy;
				//System.out.println("total power " + dH/getThermoSystem().getFlowRate("kg/sec"));
				double densOut = thermoSystem.getDensity("kg/m3");
				
				double n = Math.log(getOutletPressure() / presinn) / Math.log(densOut / densInn);
				double CF = (enthalpyOutIsentropic-inletEnthalpy) / thermoSystem.getFlowRate("kg/sec")
						/ (n / (n - 1.0) * (getOutletPressure() * 1e5 / densOutIsentropic - presinn * 1e5 / densInn));
				
				double F1 = thermoSystem.getTotalNumberOfMoles();
				double polytropicPower = F1 * MW * (n / (n - 1.0)) * CF * presinn*1e5 / densInn
						* (Math.pow((getOutletPressure() / presinn), (n - 1.0) / n) - 1.0);
				//System.out.println("polytropic power " + polytropicPower/getThermoSystem().getFlowRate("kg/sec"));
				polytropicEfficiency = polytropicPower/getThermoSystem().getFlowRate("kg/sec")/(dH/getThermoSystem().getFlowRate("kg/sec"));
				isentropicEfficiency = (enthalpyOutIsentropic-inletEnthalpy)/dH;
				
				//isentropicEfficiency = (getThermoSystem().getEnthalpy() - hinn) / dH;
				
				double k = Math.log(getOutletPressure() / presinn) / Math.log(densOutIsentropic / densInn);
				double term1 = Math.pow(getOutletPressure() / presinn, (n-1.0)/n)-1.0;
				double term2 = n/(n-1.0)*(k-1.0)/k;
				double term3 = Math.pow(getOutletPressure() / presinn, (k-1.0)/k)-1.0;
				double polyPow =term1*term2/term3*isentropicEfficiency; 
				polytropicEfficiency = polyPow;
				polytropicPower = dH*polytropicEfficiency;
				//System.out.println("polytropic eff " + polytropicEfficiency);
				//System.out.println("isentropic eff " + isentropicEfficiency);
				polytropicFluidHead = polytropicPower / getThermoSystem().getFlowRate("kg/sec") / 1000.0;
				polytropicHeadMeter = polytropicFluidHead * 1000.0 / 9.81;
				return;
			}
		}
		if (compressorChart.isUseCompressorChart()) {
			do {
				double polytropEff = getCompressorChart().getPolytropicEfficiency(thermoSystem.getFlowRate("m3/hr"),
						getSpeed());
				setPolytropicEfficiency(polytropEff / 100.0);
				// logger.info("actual inlet flow " + thermoSystem.getFlowRate("m3/hr") + "
				// m/hr");
				polytropicHead = getCompressorChart().getPolytropicHead(thermoSystem.getFlowRate("m3/hr"), getSpeed());
				double temperature_inlet = thermoSystem.getTemperature();
				double z_inlet = thermoSystem.getZ();
				double MW = thermoSystem.getMolarMass();

				if (getCompressorChart().useRealKappa()) {
					kappa = thermoSystem.getGamma();
				} else {
					kappa = thermoSystem.getGamma2();
				}

				double n = 1.0 / (1.0 - (kappa - 1.0) / kappa * 1.0 / (polytropEff / 100.0));
				// logger.info("n " + n);
				polytropicExponent = n;
				if (getCompressorChart().getHeadUnit().equals("meter")) {
					polytropicFluidHead = polytropicHead / 1000.0 * 9.81;
					polytropicHeadMeter = polytropicHead;
				} else {
					polytropicFluidHead = polytropicHead;
					polytropicHeadMeter = polytropicHead * 1000.0 / 9.81;
				}

				double pressureRatio = Math.pow(
						(polytropicFluidHead * 1000.0 + (n / (n - 1.0) * z_inlet * 8.314 * (temperature_inlet) / MW))
								/ (n / (n - 1.0) * z_inlet * 8.314 * (temperature_inlet) / MW),
						n / (n - 1.0));
				// System.out.println("pressure ratio " + pressureRatio);
				// logger.info("pressure ratio " + pressureRatio);
				setOutletPressure(thermoSystem.getPressure() * pressureRatio);
				// logger.info("polytropic head " + polytropicFluidHead + " kj/kg");
				// logger.info("polytropic head " + polytropicHeadMeter + " meter");
				if (getAntiSurge().isActive()) {
					logger.info("surge flow " + getCompressorChart().getSurgeCurve().getSurgeFlow(polytropicHead)
							+ " m3/hr");
					surgeCheck = isSurge(polytropicHead, thermoSystem.getFlowRate("m3/hr"));
					// logger.info("surge? " + surgeCheck);
				}
				if (getCompressorChart().getStoneWallCurve().isActive()) {
					// logger.info("stone wall? " + isStoneWall(polytropicHead,
					// thermoSystem.getFlowRate("m3/hr")));
				}
				if (surgeCheck && getAntiSurge().isActive()) {
					double surgeFLow = getCompressorChart().getSurgeCurve().getSurgeFlow(polytropicHead);
					double correction = surgeFLow / thermoSystem.getFlowRate("m3/hr");
					thermoSystem.setTotalNumberOfMoles(
							getAntiSurge().getSurgeControlFactor() * thermoSystem.getTotalNumberOfMoles());
					thermoSystem.init(3);
					fractionAntiSurge = thermoSystem.getTotalNumberOfMoles() / orginalMolarFLow - 1.0;
					getAntiSurge().setCurrentSurgeFraction(fractionAntiSurge);
					// logger.info("fractionAntiSurge: " + fractionAntiSurge);
				}

				powerSet = true;
				dH = polytropicFluidHead * 1000.0 * thermoSystem.getMolarMass() / getPolytropicEfficiency()
						* thermoSystem.getTotalNumberOfMoles();
				// logger.info("dH: " + dH);
			} while (surgeCheck && getAntiSurge().isActive());
		}

		if (usePolytropicCalc) {

			if (powerSet) {
				// dH = (getPower() - hinn) / polytropicEfficiency;
				double hout = hinn * (1 - 0 + fractionAntiSurge) + dH;
				thermoSystem.setPressure(pressure);
				// findOutPressure(hinn, hout, polytropicEfficiency);
				// System.out.println("hout " + hout);
				thermoOps.PHflash(hout, 0);
			} else {
				int numbersteps = numberOfCompresorCalcSteps;
				double dp = (pressure - getThermoSystem().getPressure()) / (1.0 * numbersteps);
				for (int i = 0; i < numbersteps; i++) {
					entropy = getThermoSystem().getEntropy();
					hinn = getThermoSystem().getEnthalpy();
					getThermoSystem().setPressure(getThermoSystem().getPressure() + dp);
					thermoOps.PSflash(entropy);
					double hout = hinn + (getThermoSystem().getEnthalpy() - hinn) / polytropicEfficiency;
					thermoOps.PHflash(hout, 0);
				}
			}
			/*
			 * HYSYS method double oldPolyt = 10.5; int iter = 0; do {
			 *
			 *
			 * iter++; double n = Math.log(thermoSystem.getPressure() / presinn) /
			 * Math.log(thermoSystem.getDensity() / densInn); double k =
			 * Math.log(thermoSystem.getPressure() / presinn) / Math.log(densOutIdeal /
			 * densInn); double factor = ((Math.pow(thermoSystem.getPressure() / presinn, (n
			 * - 1.0) / n) - 1.0) * (n / (n - 1.0)) * (k - 1) / k) /
			 * (Math.pow(thermoSystem.getPressure() / presinn, (k - 1.0) / k) - 1.0);
			 * oldPolyt = polytropicEfficiency; polytropicEfficiency = factor *
			 * isentropicEfficiency; dH = thermoSystem.getEnthalpy() - hinn; hout = hinn +
			 * dH / polytropicEfficiency; thermoOps.PHflash(hout, 0);
			 * System.out.println(" factor " + factor + " n " + n + " k " + k +
			 * " polytropic effici " + polytropicEfficiency + " iter " + iter);
			 *
			 * } while (Math.abs((oldPolyt - polytropicEfficiency) / oldPolyt) > 1e-5 &&
			 * iter < 500); // polytropicEfficiency = isentropicEfficiency * ();
			 *
			 */
		} else {
			getThermoSystem().setPressure(pressure);
			// System.out.println("entropy inn.." + entropy);
			thermoOps.PSflash(entropy);
			// double densOutIdeal = getThermoSystem().getDensity();
			if (!powerSet) {
				dH = (getThermoSystem().getEnthalpy() - hinn) / isentropicEfficiency;
			}
			double hout = hinn + dH;
			isentropicEfficiency = (getThermoSystem().getEnthalpy() - hinn) / dH;
			dH = hout - hinn;
			thermoOps.PHflash(hout, 0);
		}
		// thermoSystem.display();

		if (getCompressorChart().isUseCompressorChart() && getAntiSurge().isActive()) {
			thermoSystem.setTotalNumberOfMoles(orginalMolarFLow);
			thermoSystem.init(3);
		}
		thermoSystem.initProperties();
		outStream.setThermoSystem(getThermoSystem());

		polytropicFluidHead = getPower() / getThermoSystem().getFlowRate("kg/sec") / 1000.0 * getPolytropicEfficiency();
		polytropicHeadMeter = polytropicFluidHead * 1000.0 / 9.81;
	}

	public void displayResult() {

		DecimalFormat nf = new DecimalFormat();
		nf.setMaximumFractionDigits(5);
		nf.applyPattern("#.#####E0");

		JDialog dialog = new JDialog(new JFrame(), "Results from TPflash");
		Container dialogContentPane = dialog.getContentPane();
		dialogContentPane.setLayout(new FlowLayout());

		getThermoSystem().initPhysicalProperties();
		String[][] table = new String[50][5];
		String[] names = { "", "Phase 1", "Phase 2", "Phase 3", "Unit" };
		table[0][0] = "";
		table[0][1] = "";
		table[0][2] = "";
		table[0][3] = "";
		StringBuffer buf = new StringBuffer();
		FieldPosition test = new FieldPosition(0);

		for (int i = 0; i < getThermoSystem().getNumberOfPhases(); i++) {
			for (int j = 0; j < getThermoSystem().getPhases()[0].getNumberOfComponents(); j++) {
				table[j + 1][0] = getThermoSystem().getPhases()[0].getComponents()[j].getName();
				buf = new StringBuffer();
				table[j + 1][i + 1] = nf.format(getThermoSystem().getPhases()[i].getComponents()[j].getx(), buf, test)
						.toString();
				table[j + 1][4] = "[-]";
			}
			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 2][0] = "Density";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 2][i + 1] = nf
					.format(getThermoSystem().getPhases()[i].getPhysicalProperties().getDensity(), buf, test)
					.toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 2][4] = "[kg/m^3]";

			// Double.longValue(thermoSystem.getPhases()[i].getBeta());
			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 3][0] = "PhaseFraction";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 3][i + 1] = nf
					.format(getThermoSystem().getPhases()[i].getBeta(), buf, test).toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 3][4] = "[-]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 4][0] = "MolarMass";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 4][i + 1] = nf
					.format(getThermoSystem().getPhases()[i].getMolarMass() * 1000, buf, test).toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 4][4] = "[kg/kmol]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 5][0] = "Cp";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 5][i + 1] = nf
					.format((getThermoSystem().getPhases()[i].getCp()
							/ getThermoSystem().getPhases()[i].getNumberOfMolesInPhase() * 1.0
							/ getThermoSystem().getPhases()[i].getMolarMass() * 1000), buf, test)
					.toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 5][4] = "[kJ/kg*K]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 7][0] = "Viscosity";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 7][i + 1] = nf
					.format((getThermoSystem().getPhases()[i].getPhysicalProperties().getViscosity()), buf, test)
					.toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 7][4] = "[kg/m*sec]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 8][0] = "Conductivity";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 8][i + 1] = nf
					.format(getThermoSystem().getPhases()[i].getPhysicalProperties().getConductivity(), buf, test)
					.toString();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 8][4] = "[W/m*K]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 10][0] = "Pressure";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 10][i + 1] = Double
					.toString(getThermoSystem().getPhases()[i].getPressure());
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 10][4] = "[bar]";

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 11][0] = "Temperature";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 11][i + 1] = Double
					.toString(getThermoSystem().getPhases()[i].getTemperature());
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 11][4] = "[K]";
			Double.toString(getThermoSystem().getPhases()[i].getTemperature());

			buf = new StringBuffer();
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 13][0] = "Stream";
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 13][i + 1] = name;
			table[getThermoSystem().getPhases()[0].getNumberOfComponents() + 13][4] = "-";
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

	public double getTotalWork() {
		return getThermoSystem().getEnthalpy() - inletEnthalpy;
	}

	public void runTransient() {
	}

	/**
	 * @return the isentropicEfficientcy
	 */
	public double getIsentropicEfficiency() {
		return isentropicEfficiency;
	}

	/**
	 * @param isentropicEfficientcy the isentropicEfficientcy to set
	 */
	public void setIsentropicEfficiency(double isentropicEfficientcy) {
		this.isentropicEfficiency = isentropicEfficientcy;
	}

	/**
	 * @return the usePolytropicCalc
	 */
	public boolean usePolytropicCalc() {
		return usePolytropicCalc;
	}

	/**
	 * @param usePolytropicCalc the usePolytropicCalc to set
	 */
	public void setUsePolytropicCalc(boolean usePolytropicCalc) {
		this.usePolytropicCalc = usePolytropicCalc;
	}

	/**
	 * @return the polytropicEfficiency
	 */
	public double getPolytropicEfficiency() {
		return polytropicEfficiency;
	}

	/**
	 * @param polytropicEfficiency the polytropicEfficiency to set
	 */
	public void setPolytropicEfficiency(double polytropicEfficiency) {
		this.polytropicEfficiency = polytropicEfficiency;
	}

	/**
	 * @return the thermoSystem
	 */
	public SystemInterface getThermoSystem() {
		return thermoSystem;
	}

	public CompressorChartInterface getCompressorChart() {
		return compressorChart;
	}

	public void setCompressorChart(CompressorChart compressorChart) {
		this.compressorChart = compressorChart;
	}

	public AntiSurge getAntiSurge() {
		return antiSurge;
	}

	public boolean isSurge(double flow, double head) {
		getAntiSurge().setSurge(getCompressorChart().getSurgeCurve().isSurge(flow, head));
		return getAntiSurge().isSurge();
	}

	public boolean isStoneWall(double flow, double head) {
		return getCompressorChart().getStoneWallCurve().isStoneWall(flow, head);
	}

	public void setAntiSurge(AntiSurge antiSurge) {
		this.antiSurge = antiSurge;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public double getPolytropicHead(String unit) {
		if (unit.equals("kJ/kg"))
			return polytropicFluidHead;
		else if (unit.equals("meter"))
			return polytropicHeadMeter;
		else
			return polytropicHead;
	}

	public double getPolytropicHead() {
		return polytropicHead;
	}

	public double getPolytropicFluidHead() {
		return polytropicFluidHead;
	}

	public double getPolytropicExponent() {
		return polytropicExponent;
	}

	public double getPolytropicHeadMeter() {
		return polytropicHeadMeter;
	}

	public void setPolytropicHeadMeter(double polytropicHeadMeter) {
		this.polytropicHeadMeter = polytropicHeadMeter;
	}

	public double getOutTemperature() {
		if (useOutTemperature)
			return outTemperature;
		else
			return getThermoSystem().getTemperature();
	}

	public void setOutTemperature(double outTemperature) {
		useOutTemperature = true;
		this.outTemperature = outTemperature;
	}

	public void useOutTemperature(boolean useOutTemperature) {
		this.useOutTemperature = useOutTemperature;
	}

	public int getNumberOfCompresorCalcSteps() {
		return numberOfCompresorCalcSteps;
	}

	public void setNumberOfCompresorCalcSteps(int numberOfCompresorCalcSteps) {
		this.numberOfCompresorCalcSteps = numberOfCompresorCalcSteps;
	}

	public boolean isUseRigorousPolytropicMethod() {
		return useRigorousPolytropicMethod;
	}

	public void setUseRigorousPolytropicMethod(boolean useRigorousPolytropicMethod) {
		this.useRigorousPolytropicMethod = useRigorousPolytropicMethod;
	}

}
