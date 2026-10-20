
package app.controllers.impl;

import app.daos.impl.LocationDAO;
import app.entities.Location;

public class LocationController extends CrudController<Location> {

    public LocationController(LocationDAO locationDAO) {
        super(
                locationDAO,
                Location.class,
                "/locations",
                "Location",
                Location::setId
        );
    }
}