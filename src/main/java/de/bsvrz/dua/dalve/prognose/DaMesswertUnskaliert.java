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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

/**
 * Funktioniert wie die Superklasse (plus einige nur für Datenaufbereitung 
 * notwendige Eigenschaften)
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DaMesswertUnskaliert
extends MesswertUnskaliert{

	
	/**
	 * Standardkonstruktor
	 * 
	 * @param attName der Attributname dieses Messwertes
	 * @param datum das Datum aus dem der Messwert ausgelesen werden soll
	 */
	public DaMesswertUnskaliert(final String attName, final Data datum){
		super(attName, datum);
	}
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param attName der Attributname dieses Messwertes
	 */
	public DaMesswertUnskaliert(final String attName){
		super(attName);
	}
	
		
	/**
	 * Erfragt, ob bei diesem Wert<br>
	 * Interpoliert UND/ODER<br>
	 * WertMax UND/ODER<br>
	 * WertMin UND/ODER<br>
	 * WertMaxLogisch UND/ODER<br>
	 * WertMinLogisch gesetzt ist
	 *  
	 * @return ob dieser Wert schon plausibilisiert wurde
	 */
	public final boolean isPlausibilisiert(){
		return this.isInterpoliert() ||
			   this.isFormalMax() || 
			   this.isFormalMin() ||
			   this.isLogischMax() ||
			   this.isLogischMin();
	}
}
