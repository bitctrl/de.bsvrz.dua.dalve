/*
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007-2015 BitCtrl Systems GmbH
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:<br>
 * BitCtrl Systems GmbH<br>
 * Weißenfelser Straße 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

/**
 * Funktioniert wie die Superklasse (plus einige nur für Datenaufbereitung notwendige
 * Eigenschaften).
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class DaMesswertUnskaliert extends MesswertUnskaliert {

	/**
	 * Standardkonstruktor.
	 *
	 * @param attName
	 *            der Attributname dieses Messwertes
	 * @param datum
	 *            das Datum aus dem der Messwert ausgelesen werden soll
	 */
	public DaMesswertUnskaliert(final String attName, final Data datum) {
		super(attName, datum);
	}

	/**
	 * Standardkonstruktor.
	 *
	 * @param attName
	 *            der Attributname dieses Messwertes
	 */
	public DaMesswertUnskaliert(final String attName) {
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
	public final boolean isPlausibilisiert() {
		return isInterpoliert() || isFormalMax() || isFormalMin() || isLogischMax()
				|| isLogischMin();
	}
}
