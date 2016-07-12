/*
 * Segment Datenübernahme und Aufbereitung (DUA), SWE Datenaufbereitung LVE
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

package de.bsvrz.dua.dalve.analyse;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public interface AtgVerkehrsDatenKurzZeitAnalyse {
	/**
	 * Erfragt <code>kKfz.Grenz</code>.
	 * 
	 * @return <code>kKfz.Grenz</code>
	 */
	long getKKfzGrenz();

	/**
	 * Erfragt <code>kKfz.Max</code>.
	 * 
	 * @return <code>kKfz.Max</code>
	 */
	long getKKfzMax();

	/**
	 * Erfragt <code>kLkw.Grenz</code>.
	 * 
	 * @return <code>kLkw.Grenz</code>
	 */
	long getKLkwGrenz();

	/**
	 * Erfragt <code>kLkw.Max</code>.
	 * 
	 * @return <code>kLkw.Max</code>
	 */
	long getKLkwMax();

	/**
	 * Erfragt <code>kPkw.Grenz</code>.
	 * 
	 * @return <code>kPkw.Grenz</code>
	 */
	long getKPkwGrenz();

	/**
	 * Erfragt <code>kPkw.Max</code>.
	 * 
	 * @return <code>kPkw.Max</code>
	 */
	long getKPkwMax();

	/**
	 * Erfragt <code>kB.Grenz</code>.
	 * 
	 * @return <code>kB.Grenz</code>
	 */
	long getKBGrenz();

	/**
	 * Erfragt <code>kB.Max</code>.
	 * 
	 * @return <code>kB.Max</code>
	 */
	long getKBMax();

	/**
	 * Erfragt <code>fl.k1</code>.
	 * 
	 * @return <code>fl.k1</code>
	 */
	double getFlk1();

	/**
	 * Erfragt <code>fl.k2</code>.
	 * 
	 * @return <code>fl.k2</code>
	 */
	double getFlk2();

	/**
	 * Erfragt, ob dieses Objekt bereits Parameter emfangen hat.
	 * 
	 * @return ob dieses Objekt bereits Parameter emfangen hat
	 */
	boolean isInitialisiert();
}
