package org.example;

import org.orekit.data.*;
import org.orekit.time.*;
import org.orekit.frames.*;
import org.orekit.bodies.*;
import org.orekit.utils.*;
import org.orekit.propagation.analytical.tle.*;
import org.orekit.propagation.*;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.io.*;
import java.util.Locale;

public class TLEPropagationExample {
    public static void main(String[] args) {
        try {
            Locale.setDefault(Locale.US);
            System.out.println("TLE Propagation Started...");

            File orekitData = new File("../orekit-data-main");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
            manager.addProvider(new DirectoryCrawler(orekitData));

            BufferedReader br = new BufferedReader(new FileReader("../tle_25994_20250513.txt"));


            PrintWriter writer = new PrintWriter(new FileWriter("tle_output.csv"));
            writer.println("EpochDate,PredictionDate,Latitude(deg),Longitude(deg),Altitude(m),Velocity(m/s)");

            String line1, line2;
            while ((line1 = br.readLine()) != null && (line2 = br.readLine()) != null) {
                if (!line1.startsWith("1") || !line2.startsWith("2")) continue;

                TLE tle = new TLE(line1, line2);
                TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

                AbsoluteDate startDate = tle.getDate();
                Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
                OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                              Constants.WGS84_EARTH_FLATTENING,
                                                              earthFrame);

                AbsoluteDate targetDate = startDate.shiftedBy(86400.0);
                SpacecraftState state = propagator.propagate(targetDate);

                PVCoordinates pv = state.getPVCoordinates(earthFrame);
                Vector3D position = pv.getPosition();
                Vector3D velocity = pv.getVelocity();

                GeodeticPoint geoPoint = earth.transform(position, earthFrame, targetDate);

                double altitude = geoPoint.getAltitude();
                double lat = Math.toDegrees(geoPoint.getLatitude());
                double lon = Math.toDegrees(geoPoint.getLongitude());
                double speed = velocity.getNorm();

                System.out.printf("Epoch: %s | Date: %s | Lat: %.6f | Lon: %.6f | Alt: %.2f m | V: %.2f m/s\n",
                                  startDate, targetDate, lat, lon, altitude, speed);

                writer.printf(Locale.US, "%s,%s,%.6f,%.6f,%.2f,%.2f\n",
                              startDate, targetDate, lat, lon, altitude, speed);
            }

            br.close();
            writer.close();

            System.out.println("Propagation finished. Results saved to tle_output.csv");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
