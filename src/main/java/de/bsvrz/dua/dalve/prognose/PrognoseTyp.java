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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;

/**
 * Prognosetyp:
 * <ul>
 * <li><code>Flink</code></li>
 * <li><code>Normal</code></li>
 * <li><code>Träge</code></li>
 * </ul>
 * .
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class PrognoseTyp {

	/** Prognosewerte folgen den Messwerten sehr schnell, geringe Glättung. */
	public static PrognoseTyp flink = null;

	/** Prognosewerte folgen den Messwerten normal, normale Glättung. */
	public static PrognoseTyp normal = null;

	/** Prognosewerte folgen den Messwerten sehr langsam, starke Glättung. */
	public static PrognoseTyp traege = null;

	/** Der Aspekt. */
	private Aspect aspekt = null;

	/** Parameterattributgruppe fuer Fahrstreifen. */
	private AttributeGroup atgFahrStreifen = null;

	/** Parameterattributgruppe fuer Messquerschnitte. */
	private AttributeGroup atgMessQuerschnitt = null;

	/**
	 * Standardkonstruktor.
	 *
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 * @param name
	 *            <code>Flink</code>, <code>Normal</code> oder <code>Träge</code>
	 */
	private PrognoseTyp(final ClientDavInterface dav, final String name) {
		aspekt = dav.getDataModel().getAspect("asp.prognose" + name); //$NON-NLS-1$
		atgFahrStreifen = dav.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognose" + name + "Fs"); //$NON-NLS-1$//$NON-NLS-2$
		atgMessQuerschnitt = dav.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognose" + name + "Mq"); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Initialisiert alle statischen Objekte dieses Typs.
	 *
	 * @param dav
	 *            Verbindung zum Datenverteiler
	 */
	public static final void initialisiere(final ClientDavInterface dav) {
		if (flink == null) {
			flink = new PrognoseTyp(dav, "Flink"); //$NON-NLS-1$
			normal = new PrognoseTyp(dav, "Normal"); //$NON-NLS-1$
			traege = new PrognoseTyp(dav, "Träge"); //$NON-NLS-1$
		}
	}

	/**
	 * Erfragt den Aspekt, unter dem Daten dieses Typs publiziert werden sollen.
	 *
	 * @return der Aspekt, unter dem Daten dieses Typs publiziert werden sollen
	 */
	public final Aspect getAspekt() {
		return aspekt;
	}

	/**
	 * Erfragt die Parameterattributgruppe dieses Prognosetyps für einen Fahrstreifen oder einen
	 * Messquerschnitt.
	 *
	 * @param fuerFahrStreifen
	 *            ob die Parameterattributgruppe für einen Fahrstreifen <code>true</code> oder einen
	 *            Messquerschnitt <code>false</code> benötigt wird
	 * @return die Parameterattributgruppe dieses Prognosetyps für einen Fahrstreifen oder einen
	 *         Messquerschnitt
	 */
	public final AttributeGroup getParameterAtg(final boolean fuerFahrStreifen) {
		return fuerFahrStreifen ? atgFahrStreifen : atgMessQuerschnitt;
	}

	@Override
	public String toString() {
		return aspekt.getPid().substring("asp.prognose".length());
	}

}
