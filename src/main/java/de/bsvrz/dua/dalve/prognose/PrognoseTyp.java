/*
 * Segment 4 Daten�bernahme und Aufbereitung (DUA), SWE 4.7 Datenaufbereitung LVE
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
 * Wei�enfelser Stra�e 67<br>
 * 04229 Leipzig<br>
 * Phone: +49 341-490670<br>
 * mailto: info@bitctrl.de
 */
package de.bsvrz.dua.dalve.prognose;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;

// TODO: Auto-generated Javadoc
/**
 * Prognosetyp:<br>
 * - <code>Flink<code><br>
 * - <code>Normal<code><br>
 * - <code>Tr�ge<code>.
 *
 * @author BitCtrl Systems GmbH, Thierfelder
 */
public class PrognoseTyp {

	/** Prognosewerte folgen den Messwerten sehr schnell, geringe Gl�ttung. */
	public static PrognoseTyp FLINK = null;

	/** Prognosewerte folgen den Messwerten normal, normale Gl�ttung. */
	public static PrognoseTyp NORMAL = null;

	/** Prognosewerte folgen den Messwerten sehr langsam, starke Gl�ttung. */
	public static PrognoseTyp TRAEGE = null;

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
	 *            <code>Flink</code>, <code>Normal</code> oder <code>Tr�ge</code>
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
		if (FLINK == null) {
			FLINK = new PrognoseTyp(dav, "Flink"); //$NON-NLS-1$
			NORMAL = new PrognoseTyp(dav, "Normal"); //$NON-NLS-1$
			TRAEGE = new PrognoseTyp(dav, "Tr�ge"); //$NON-NLS-1$
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
	 * Erfragt die Parameterattributgruppe dieses Prognosetyps f�r einen Fahrstreifen oder einen
	 * Messquerschnitt.
	 *
	 * @param fuerFahrStreifen
	 *            ob die Parameterattributgruppe f�r einen Fahrstreifen <code>true</code> oder einen
	 *            Messquerschnitt <code>false</code> ben�tigt wird
	 * @return die Parameterattributgruppe dieses Prognosetyps f�r einen Fahrstreifen oder einen
	 *         Messquerschnitt
	 */
	public final AttributeGroup getParameterAtg(final boolean fuerFahrStreifen) {
		return fuerFahrStreifen ? atgFahrStreifen : atgMessQuerschnitt;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return aspekt.getPid().substring("asp.prognose".length());
	}

}
