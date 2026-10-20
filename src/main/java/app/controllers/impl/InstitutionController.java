package app.controllers.impl;

import app.daos.impl.InstitutionDAO;
import app.entities.Institution;

public class InstitutionController
        extends CrudController<Institution> {

    public InstitutionController(InstitutionDAO institutionDAO) {
        super(
                institutionDAO,
                Institution.class,
                "/institutions",
                "Institution",
                Institution::setId
        );
    }
}