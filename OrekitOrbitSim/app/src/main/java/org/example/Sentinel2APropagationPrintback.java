package org.example;

import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Frame;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.Constants;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.models.earth.atmosphere.NRLMSISE00;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.geometry.euclidean.threed.Vector3D;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class Sentinel2APropagationPrintback {
    public static void main(String[] args) {
        try {
            Locale.setDefault(Locale.US);
            System.out.println("Starting 1-year propagation and printing altitude...");

            File orekitData = new File("C:\\Users\\Adm Local\\Desktop\\AICOX\\in work\\OrekitOrbitSim\\orekit-data-main");
            DirectoryCrawler crawler = new DirectoryCrawler(orekitData);
            DataContext.getDefault().getDataProvidersManager().addProvider(crawler);

            double a = 7150000.0;
            double e = 0.0001;
            double i = Math.toRadians(98.62);
            double raan = Math.toRadians(153.0);
            double omega = Math.toRadians(0.0);
            double M0 = Math.toRadians(0.0);

            AbsoluteDate startDate = new AbsoluteDate(2025, 3, 18, 0, 0, 0.0, TimeScalesFactory.getUTC());
            Frame inertialFrame = FramesFactory.getEME2000();
            Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, M0,
                    PositionAngleType.MEAN, inertialFrame, startDate, Constants.WGS84_EARTH_MU);

            SpacecraftState initialState = new SpacecraftState(initialOrbit, 1200.0);

            double minStep = 0.001;
            double maxStep = 1000.0;
            double[][] tolerances = NumericalPropagator.tolerances(10.0, initialOrbit, OrbitType.KEPLERIAN);
            DormandPrince853Integrator integrator = new DormandPrince853Integrator(minStep, maxStep, tolerances[0], tolerances[1]);
            NumericalPropagator propagator = new NumericalPropagator(integrator);
            propagator.setOrbitType(OrbitType.CARTESIAN);
            propagator.setInitialState(initialState);

            NormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getNormalizedProvider(6, 6);
            propagator.addForceModel(new HolmesFeatherstoneAttractionModel(earthFrame, gravityProvider));

            MarshallSolarActivityFutureEstimation msafe = new MarshallSolarActivityFutureEstimation(
                    MarshallSolarActivityFutureEstimation.DEFAULT_SUPPORTED_NAMES,
                    MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
            OneAxisEllipsoid earth = new OneAxisEllipsoid(
                    Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                    Constants.WGS84_EARTH_FLATTENING,
                    earthFrame);
            NRLMSISE00 atmosphere = new NRLMSISE00(msafe, CelestialBodyFactory.getSun(), earth);
            IsotropicDrag dragModel = new IsotropicDrag(4.0, 2.2);
            propagator.addForceModel(new DragForce(atmosphere, dragModel));

            RadiationSensitive radiationModel = new IsotropicRadiationSingleCoefficient(4.0, 1.5);
            SolarRadiationPressure srp = new SolarRadiationPressure(CelestialBodyFactory.getSun(), earth, radiationModel);
            propagator.addForceModel(srp);

            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
            propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));

           
            PrintWriter writer = new PrintWriter(new FileWriter("C:\\Users\\Adm Local\\Desktop\\AICOX\\in work\\OrekitOrbitSim\\orbit_output.csv"));
            writer.println("Date,Altitude(m),SpecificEnergy(J/kg),Speed(m/s),a(m),e,i(deg),RAAN(deg),Latitude(deg),Longitude(deg)");

            propagator.setStepHandler(60.0, currentState -> {
                AbsoluteDate date = currentState.getDate();
                PVCoordinates pv = currentState.getPVCoordinates(earthFrame);
                Vector3D position = pv.getPosition();
                double altitude = earth.transform(position, earth.getBodyFrame(), date).getAltitude();

                
                System.out.println(date + "  Altitude: " + String.format(Locale.US, "%.2f", altitude) + " m");

                
                Vector3D velocity = pv.getVelocity();
                double speed = velocity.getNorm();
                double r = position.getNorm();
                double mu = Constants.WGS84_EARTH_MU;
                double specificEnergy = 0.5 * speed * speed - mu / r;

                Orbit currentOrbit = currentState.getOrbit();
                double a_now = currentOrbit.getA();
                double e_now = currentOrbit.getE();
                double i_now = Math.toDegrees(currentOrbit.getI());
                double raan_now = Math.toDegrees(new KeplerianOrbit(currentOrbit).getRightAscensionOfAscendingNode());

                

                
                writer.printf(Locale.US, "%s,%.2f,%.4f,%.4f,%.2f,%.5f,%.4f,%.4f",
                        date, altitude, specificEnergy, speed, a_now, e_now, i_now, raan_now);
            });

            AbsoluteDate finalDate = startDate.shiftedBy(365 * 86400.0);
            propagator.propagate(finalDate);

            writer.close();
            System.out.println("Propagation finished and data saved to orbit_output.csv");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
