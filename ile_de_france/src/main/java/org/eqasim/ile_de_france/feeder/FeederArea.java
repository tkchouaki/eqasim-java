package org.eqasim.ile_de_france.feeder;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.facilities.Facility;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class FeederArea {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final Geometry area;

	public FeederArea(URL url) {
		Geometry area = null;

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));

			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();

			if (featureCollection.size() > 0) {
				area = (Geometry) featureCollection.features().next().getDefaultGeometry();
			}

			dataStore.dispose();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (area == null) {
			throw new IllegalStateException();
		}

		this.area = area;
	}

	private boolean contains(Facility facility) {
		Coord coord = facility.getCoord();
		return area.contains(geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY())));
	}

	public boolean isAccess(Facility fromFacility, Facility toFacility) {
		return contains(fromFacility) && !contains(toFacility);
	}

	public boolean isEgress(Facility fromFacility, Facility toFacility) {
		return !contains(fromFacility) && contains(toFacility);
	}
}
