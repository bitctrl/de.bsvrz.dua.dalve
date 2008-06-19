/**
 * Segment 4 Datenübernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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

import java.util.LinkedList;

/**
 * Ringpuffer fuer aktuelle und zurueckliegende Werte des Stoerfallindikators
 * VKDiffKfz.
 * TODO: Anpassen an nicht auf Erfassungsintervalldauer genormte tReise-Paramter
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
class RingPuffer {
	
	/**
	 * die Zeitspanne, die im Puffer verarbeitet werden soll.
	 */
	private long zeitSpanne = -1;
	
	/**
	 * Ringpuffer mit den zeitlich aktuellsten Daten.
	 */
	private LinkedList<VKDiffWert> ringPuffer = new LinkedList<VKDiffWert>();

	/**
	 * Aktualisiert diesen Puffer mit neuen Daten. Alte Daten werden dabei ggf. 
	 * aus dem Puffer geloescht
	 * 
	 * @param wert
	 *            ein aktuelles Datum.
	 */
	void put(VKDiffWert wert) {
		this.ringPuffer.addFirst(wert);
		
		long letzteErlaubteDatenzeit = wert.getZeitStempel() - zeitSpanne;
		boolean stop = false;
		do{
			if(!this.ringPuffer.isEmpty()) {
				if(this.ringPuffer.getLast().getZeitStempel() < letzteErlaubteDatenzeit) {
					this.ringPuffer.removeLast();
				}else{
					stop = true;
				}
			} else {
				stop = true;
			}
		}while(!stop);
	}

	/**
	 * Setzt die Zeitspanne, die im Puffer verarbeitet werden soll.
	 * 
	 * @param zeitSpanne die Zeitspanne, die im Puffer verarbeitet werden soll.
	 */
	void setGroesse(long zeitSpanne) {
		this.zeitSpanne = zeitSpanne;
	}

	/**
	 * Erfragt das im Puffer stehende Datum fuer den uebergebenen Zeitstempel.
	 * 
	 * @param zeitStempel
	 *            ein Zeitstempel
	 * @return das im Puffer stehende Datum fuer den uebergebenen Zeitstempel.
	 */
	VKDiffWert getDatumFuerZeitpunkt(long zeitStempel) {
		VKDiffWert wert = VKDiffWert.getLeer(zeitStempel);
		
		if(!this.ringPuffer.isEmpty()) {
			if(this.ringPuffer.getLast().getZeitStempel() <= zeitStempel){
				wert = this.ringPuffer.getLast();
			} 
		}
		
		return wert;
	}
		
}
