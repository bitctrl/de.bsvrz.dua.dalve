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

package de.bsvrz.dua.dalve.stoerfall.vkdiffkfz;

import java.util.LinkedList;

/**
 * Ringpuffer fuer aktuelle und zurueckliegende Werte des Stoerfallindikators <code>VKDiffKfz</code>
 * .<br>
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
	private final LinkedList<VKDiffWert> ringPuffer = new LinkedList<VKDiffWert>();

	/**
	 * Aktualisiert diesen Puffer mit neuen Daten. Alte Daten werden dabei ggf. aus dem Puffer
	 * geloescht
	 *
	 * @param wert
	 *            ein aktuelles Datum.
	 */
	void put(final VKDiffWert wert) {
		ringPuffer.addFirst(wert);
		final long letzteErlaubteDatenzeit = wert.getZeitStempel() - zeitSpanne;
		for (int i = ringPuffer.size() - 2; i > -1; i--) {
			if (ringPuffer.get(i).getZeitStempel() < letzteErlaubteDatenzeit) {
				ringPuffer.removeLast();
			} else {
				break;
			}
		}
	}

	/**
	 * Setzt die Zeitspanne, die im Puffer verarbeitet werden soll.
	 *
	 * @param zeitSpanne
	 *            die Zeitspanne, die im Puffer verarbeitet werden soll.
	 */
	void setGroesse(final long zeitSpanne) {
		this.zeitSpanne = zeitSpanne;
	}

	/**
	 * Erfragt das im Puffer stehende Datum fuer den uebergebenen Zeitstempel.
	 *
	 * @param zeitStempel
	 *            ein Zeitstempel
	 * @return das im Puffer stehende Datum fuer den uebergebenen Zeitstempel.
	 */
	VKDiffWert getDatumFuerZeitpunkt(final long zeitStempel) {
		VKDiffWert wert = VKDiffWert.getLeer(zeitStempel);

		if (!ringPuffer.isEmpty()) {
			final VKDiffWert letzter = ringPuffer.getLast();
			final long diffZumLetzten = Math.abs(zeitStempel - letzter.getZeitStempel());

			if (ringPuffer.size() > 1) {
				final VKDiffWert vorLetzer = ringPuffer.get(ringPuffer.size() - 2);
				final long diffZumVorLetzten = Math.abs(zeitStempel - vorLetzer.getZeitStempel());

				if ((diffZumLetzten > diffZumVorLetzten) && (diffZumVorLetzten <= zeitSpanne)) {
					wert = vorLetzer;
				} else if ((diffZumLetzten <= diffZumVorLetzten) && (diffZumLetzten <= zeitSpanne)) {
					wert = letzter;
				}
			} else {
				if (diffZumLetzten <= zeitSpanne) {
					wert = letzter;
				}
			}
		}

		return wert;
	}

}
