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
package de.bsvrz.dua.dalve.prognose;

import java.util.Collection;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;

// TODO: Auto-generated Javadoc
/**
 * Modul, das die Berechnung sämtlicher Werte startet, 
 * die unter den Attributgruppen <code>atg.verkehrsDatenKurzZeitTrendExtraPolationFs</code>
 * und <code>atg.verkehrsDatenKurzZeitGeglättetFs</code> veröffentlicht werden
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseModul {

	/**
	 * Initialisiert alle Klassen die zur Berechnung der geglaetteten und der Prognosewerte
	 * notwendig sind.
	 *
	 * @param dav Verbindung zum Datenverteiler
	 * @param objekte Menge der Fahrstreifen und Messquerschnitte, die betrachtet werden sollen
	 * @throws DUAInitialisierungsException wird weitergereicht
	 */
	public final void initialisiere(final ClientDavInterface dav,
			final Collection<SystemObject> objekte)
			throws DUAInitialisierungsException {
		PrognoseTyp.initialisiere(dav);

		/**
		 * (I) SFI nach Verfahren MARZ 
		 */
		for (SystemObject obj : objekte) {
			new PrognoseObjektFlink().initialisiere(dav, obj);
			new PrognoseObjektNormal().initialisiere(dav, obj);
			new PrognoseObjektTraege().initialisiere(dav, obj);
		}
	}

}
