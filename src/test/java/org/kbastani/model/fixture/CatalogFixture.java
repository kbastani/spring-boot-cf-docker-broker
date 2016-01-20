package org.kbastani.model.fixture;

import org.kbastani.catalog.Catalog;

public class CatalogFixture {

	public static Catalog getCatalog() {
		return new Catalog(ServiceFixture.getAllServices());
	}
	
}
