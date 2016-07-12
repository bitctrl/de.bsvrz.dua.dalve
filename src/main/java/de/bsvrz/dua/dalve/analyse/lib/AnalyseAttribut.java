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

package de.bsvrz.dua.dalve.analyse.lib;

import java.util.HashSet;
import java.util.Set;

/**
 * Container fuer Attribute die zur Aggregation herangezogen werden (jeweils
 * fuer Fahrstreifen bzw. Messquerschnitte):<br>
 * <code>qKfz</code> bzw. <code>QKfz</code>,<br>
 * <code>qLkw</code> bzw. <code>QLkw</code>,<br>
 * <code>qPkw</code> bzw. <code>QPkw</code>,<br>
 * <code>vKfz</code> bzw. <code>VKfz</code>,<br>
 * <code>vLkw</code> bzw. <code>VLkw</code> und<br>
 * <code>vPkw</code> bzw. <code>VPkw</code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 * 
 * @version $Id$
 */
public final class AnalyseAttribut {

	/**
	 * Wertebereich.
	 */
	private static final Set<AnalyseAttribut> WERTE_BEREICH = new HashSet<AnalyseAttribut>();

	/**
	 * Attribut <code>qKfz</code> bzw. <code>QKfz</code>
	 */
	public static final AnalyseAttribut Q_KFZ = new AnalyseAttribut(
			"qKfz", "QKfz", null);

	/**
	 * Attribut <code>qLkw</code> bzw. <code>QLkw</code>
	 */
	public static final AnalyseAttribut Q_LKW = new AnalyseAttribut(
			"qLkw", "QLkw", null);

	/**
	 * Attribut <code>qPkw</code> bzw. <code>QPkw</code>
	 */
	public static final AnalyseAttribut Q_PKW = new AnalyseAttribut(
			"qPkw", "QPkw", null);

	/**
	 * Attribut <code>vKfz</code> bzw. <code>VKfz</code>
	 */
	public static final AnalyseAttribut V_KFZ = new AnalyseAttribut(
			"vKfz", "VKfz", Q_KFZ);

	/**
	 * Attribut <code>vLkw</code> bzw. <code>VLkw</code>
	 */
	public static final AnalyseAttribut V_LKW = new AnalyseAttribut(
			"vLkw", "VLkw", Q_LKW);

	/**
	 * Attribut <code>vPkw</code> bzw. <code>VPkw</code>
	 */
	public static final AnalyseAttribut V_PKW = new AnalyseAttribut(
			"vPkw", "VPkw", Q_PKW);

	/**
	 * Attribut <code>kKfz</code> bzw. <code>KKfz</code>
	 */
	public static final AnalyseAttribut K_KFZ = new AnalyseAttribut(
			"kKfz", "KKfz", null);

	/**
	 * Attribut <code>kLkw</code> bzw. <code>KLkw</code>
	 */
	public static final AnalyseAttribut K_LKW = new AnalyseAttribut(
			"kLkw", "KLkw", null);

	/**
	 * Attribut <code>kPkw</code> bzw. <code>KPkw</code>
	 */
	public static final AnalyseAttribut K_PKW = new AnalyseAttribut(
			"kPkw", "KPkw", null);

	/**
	 * Attribut <code>kB</code> bzw. <code>KB</code>
	 */
	public static final AnalyseAttribut K_B = new AnalyseAttribut(
			"kB", "KB", null);	
	/**
	 * Attribut <code>qB</code> bzw. <code>QB</code>
	 */
	public static final AnalyseAttribut Q_B = new AnalyseAttribut(
			"qB", "QB", null);
	
	/**
	 * Attribut <code>aLkw</code> bzw. <code>ALkw</code>
	 */
	public static final AnalyseAttribut A_LKW = new AnalyseAttribut(
			"aLkw", "ALkw", null);

	/**
	 * der Name des Attributs (FS).
	 */
	private final String nameFS;

	/**
	 * der Name des Attributs (MQ).
	 */
	private final String nameMQ;

	private final AnalyseAttribut _q;

	/**
	 * Standardkonstruktor.
	 *  @param nameFS
	 *            der Attributname bei Fahrstreifendaten z.B. <code>qKfz</code>
	 *            oder <code>vKfz</code>
	 * @param nameMQ
	 *            der Attributname bei Messquerschnittdaten z.B.
	 * @param q
	 */
	private AnalyseAttribut(final String nameFS, final String nameMQ, final AnalyseAttribut q) {
		this.nameFS = nameFS;
		this.nameMQ = nameMQ;
		_q = q;
		AnalyseAttribut.WERTE_BEREICH.add(this);
	}

	/**
	 * Erfragt den Namen dieses Attributs.
	 * 
	 * @param fuerFahrStreifen
	 *            das Objekt, fuer den der Name dieses Attributs erfragt wird
	 * @return der Name dieses Attributs
	 */
	public String getAttributName(final boolean fuerFahrStreifen) {
		return fuerFahrStreifen ? this.nameFS : this.nameMQ;
	}

	/**
	 * Erfragt alle statischen Instanzen dieser Klasse.
	 * 
	 * @return alle statischen Instanzen dieser Klasse
	 */
	public static Set<AnalyseAttribut> getInstanzen() {
		return AnalyseAttribut.WERTE_BEREICH;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.nameFS + " (" + this.nameMQ + ")";
	}

	public AnalyseAttribut getQ() {
		return _q;
	}
}
