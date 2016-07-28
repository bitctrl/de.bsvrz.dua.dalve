/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
 * Copyright 2015 by Kappich Systemberatung Aachen
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.
 * 
 * de.bsvrz.dua.dalve is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */
package de.bsvrz.dua.dalve.prognose;


/**
 * Ueber dieses Objekt werden die Prognosedaten fuer <b>einen</b>
 * Fahrstreifen oder einen Messquerschnitt erstellt/publiziert
 * bzw. deren Erstellung verwaltet<br>
 * (fuer den Prognosetyp <code>Normal</code>)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseObjektNormal
extends AbstraktPrognoseObjekt{	
	
	@Override
	protected PrognoseTyp getPrognoseTyp() {
		return PrognoseTyp.NORMAL;
	}

}
