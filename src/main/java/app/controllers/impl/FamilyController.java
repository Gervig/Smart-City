package app.controllers.impl;

import app.daos.impl.FamilyDAO;
import app.entities.Family;

public class FamilyController extends CrudController<Family> {

    public FamilyController(FamilyDAO familyDAO) {
        super(
                familyDAO,
                Family.class,
                "/families",
                "Family",
                Family::setId
        );
    }
}