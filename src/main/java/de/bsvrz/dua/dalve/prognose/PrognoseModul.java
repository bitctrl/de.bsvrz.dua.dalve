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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;

import java.util.Collection;

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
	 * notwendig sind
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
