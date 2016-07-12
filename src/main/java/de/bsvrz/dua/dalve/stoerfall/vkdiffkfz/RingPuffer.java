/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
 * Copyright (C) 2007 BitCtrl Systems GmbH 
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

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import java.util.LinkedList;

/**
 * Ringpuffer fuer aktuelle und zurueckliegende Werte des Stoerfallindikators
 * <code>VKDiffKfz</code>.<br>
 * <b>Achtung:</b> nicht synchronisierter Puffer!
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
		final long letzteErlaubteDatenzeit = wert.getZeitStempel() - this.zeitSpanne;
		for (int i = this.ringPuffer.size() - 2; i > -1; i--) {
			if (this.ringPuffer.get(i).getZeitStempel() < letzteErlaubteDatenzeit) {
				this.ringPuffer.removeLast();
			} else {
				break;
			}
		}
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
			final VKDiffWert letzter = this.ringPuffer.getLast();
			final long diffZumLetzten = Math.abs(zeitStempel - letzter.getZeitStempel());
			
			if(this.ringPuffer.size() > 1){
				final VKDiffWert vorLetzer = this.ringPuffer.get(this.ringPuffer.size() - 2);
				final long diffZumVorLetzten = Math.abs(zeitStempel - vorLetzer.getZeitStempel());
				
				if(diffZumLetzten > diffZumVorLetzten && diffZumVorLetzten <= this.zeitSpanne) {
					wert = vorLetzer;
				}else if(diffZumLetzten <= diffZumVorLetzten && diffZumLetzten <= this.zeitSpanne){
					wert = letzter;
				}
			}else{
				if(diffZumLetzten <= this.zeitSpanne) {
					wert = letzter;
				}
			}
		}

		return wert;
	}

}
