
package app.controllers.impl;

import app.daos.impl.CitizenDAO;
import app.entities.Citizen;

public class CitizenController extends CrudController<Citizen> {

    public CitizenController(CitizenDAO citizenDAO) {
        super(
                citizenDAO,
                Citizen.class,
                "/citizens",
                "Citizen",
                Citizen::setId
        );
    }
}