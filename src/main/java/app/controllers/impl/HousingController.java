
package app.controllers.impl;

import app.daos.impl.HousingDAO;
import app.entities.Housing;

public class HousingController extends CrudController<Housing> {

    public HousingController(HousingDAO housingDAO) {
        super(
                housingDAO,
                Housing.class,
                "/housing",
                "Housing",
                Housing::setId
        );
    }
}