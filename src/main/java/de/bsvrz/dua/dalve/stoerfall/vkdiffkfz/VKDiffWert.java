/**
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

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

// TODO: Auto-generated Javadoc
/**
 * Wert wie er von Stoerfallindikator VKDiffKfz benoetigt wird. 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
class VKDiffWert {

	/**
	 * Der Messwert.
	 */
	private MesswertUnskaliert wert = null;

	/**
	 * Der Zeitstempel des Messwerts.
	 */
	private long zeitStempel = -1;

	/**
	 * Die Erfassungsintervalldauer des Messwerts.
	 */
	private long intervallDauer = -1;

	/**
	 * Standardkonstruktor fuer Datensatz ohne Nutzdaten.
	 * 
	 * @param zeitStempel der Zeitstempel des Messwerts.
	 */
	private VKDiffWert(long zeitStempel) {
		assert (zeitStempel >= 0);
		this.zeitStempel = zeitStempel;
	}

	/**
	 * Standardkonstruktor.
	 * 
	 * @param wert der Messwert.
	 * @param zeitStempel der Zeitstempel des Messwerts.
	 * @param intervallDauer die Erfassungsintervalldauer des Messwerts. 
	 */
	VKDiffWert(MesswertUnskaliert wert, long zeitStempel, long intervallDauer) {
		assert (wert != null && zeitStempel >= 0 && intervallDauer >= 0);
		this.wert = wert;
		this.zeitStempel = zeitStempel;
		this.intervallDauer = intervallDauer;
	}

	/**
	 * Erfragt einen Wert, der keine Nutzdaten enthaelt.
	 * 
	 * @param zeitStempel der Zeitstempel.
	 * @return einen Wert, der keine Nutzdaten enthaelt.
	 */
	static VKDiffWert getLeer(long zeitStempel) {
		return new VKDiffWert(zeitStempel);
	}

	/**
	 * Erfragt den Messwert.
	 *  
	 * @return der Messwert oder <code>Double.NaN</code>, wenn kein Nutzdatum vorliegt,
	 * oder dieses < 0 ist.
	 */
	double getWert() {
		if (this.wert == null || this.wert.getWertUnskaliert() < 0) {
			return Double.NaN;
		}
		return this.wert.getWertUnskaliert();
	}

	/**
	 * Erfragt den Guete-Wert.
	 *  
	 * @return der Guete-Wert.
	 */
	GWert getGWert() {
		if (this.wert == null) {
			return GWert.getNichtErmittelbareGuete(GueteVerfahren.STANDARD);
		}
		return new GWert(this.wert.getGueteIndex(), GueteVerfahren
				.getZustand(this.wert.getVerfahren()), this.wert
				.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR);
	}

	/**
	 * Erfragt den Zeitstempel des Messwerts.
	 *  
	 * @return der Zeitstempel des Messwerts.
	 */
	long getZeitStempel() {
		return zeitStempel;
	}

	/**
	 * Erfragt die Erfassungsintervalldauer des Messwerts.
	 *  
	 * @return die Erfassungsintervalldauer des Messwerts.
	 */
	long getIntervallDauer() {
		return intervallDauer;
	}

	/**
	 * Erfragt, ob dieses Objekt keinen Wert besitzt.
	 * 
	 * @return ob dieses Objekt keinen Wert besitzt.
	 */
	boolean isLeer() {
		return this.intervallDauer < 0;
	}
}
