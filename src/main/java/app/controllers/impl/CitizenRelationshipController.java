
package app.controllers.impl;

import app.daos.impl.CitizenRelationshipDAO;
import app.entities.CitizenRelationship;

public class CitizenRelationshipController
        extends CrudController<CitizenRelationship> {

    public CitizenRelationshipController(
            CitizenRelationshipDAO relationshipDAO) {
        super(
                relationshipDAO,
                CitizenRelationship.class,
                "/citizen-relationships",
                "Citizen relationship",
                CitizenRelationship::setId
        );
    }
}