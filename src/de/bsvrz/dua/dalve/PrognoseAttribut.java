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
package de.bsvrz.dua.dalve;

import java.util.HashSet;
import java.util.Set;

/**
 * Container fuer Attribute die zur Messwertprognose herangezogen werden (jeweils 
 * fuer Fahrstreifen bzw. Messquerschnitte):<br>
 * <code>qKfz</code> bzw. <code>QKfz</code>,<br>
 * <code>qLkw</code> bzw. <code>QLkw</code>,<br>
 * <code>qPkw</code> bzw. <code>QPkw</code>,<br>
 * <code>vKfz</code> bzw. <code>VKfz</code>,<br>
 * <code>vLkw</code> bzw. <code>VLkw</code>,<br>
 * <code>vPkw</code> bzw. <code>VPkw</code>,<br>
 * <code>aLkw</code> bzw. <code>ALkw</code>,<br>
 * <code>kKfz</code> bzw. <code>KKfz</code>,<br>
 * <code>kLkw</code> bzw. <code>KLkw</code>,<br>
 * <code>kPkw</code> bzw. <code>KPkw</code>,<br>
 * <code>qB</code> bzw. <code>QB</code> und<br>
 * <code>kB</code> bzw. <code>KB</code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseAttribut {
	
	/**
	 * Wertebereich
	 */
	private static Set<PrognoseAttribut> WERTE_BEREICH = new HashSet<PrognoseAttribut>();

	/**
	 * Attribut <code>qKfz</code> bzw. <code>QKfz</code>
	 */
	public static final PrognoseAttribut Q_KFZ = null;

	/**
	 * Attribut <code>qLkw</code> bzw. <code>QLkw</code>
	 */
	public static final PrognoseAttribut Q_LKW = null;
	
	/**
	 * Attribut <code>qPkw</code> bzw. <code>QPkw</code>
	 */
	public static final PrognoseAttribut Q_PKW = null;
	
	/**
	 * Attribut <code>vKfz</code> bzw. <code>VKfz</code>
	 */
	public static final PrognoseAttribut V_KFZ = null;
	
	/**
	 * Attribut <code>vLkw</code> bzw. <code>VLkw</code>
	 */
	public static final PrognoseAttribut V_LKW = null;
	
	/**
	 * Attribut <code>vPkw</code> bzw. <code>VPkw</code>
	 */
	public static final PrognoseAttribut V_PKW = null;
	
	/**
	 * Attribut <code>aLkw</code> bzw. <code>ALkw</code>
	 */
	public static final PrognoseAttribut A_LKW = null;
	
	/**
	 * Attribut <code>kKfz</code> bzw. <code>KKfz</code>
	 */
	public static final PrognoseAttribut K_KFZ = null;
	
	/**
	 * Attribut <code>kLkw</code> bzw. <code>KLkw</code>
	 */
	public static final PrognoseAttribut K_LKW = null;

	/**
	 * Attribut <code>kPkw</code> bzw. <code>KPkw</code>
	 */
	public static final PrognoseAttribut K_PKW = null;
	
	/**
	 * Attribut <code>qB</code> bzw. <code>QB</code>
	 */
	public static final PrognoseAttribut QB = null;
	
	/**
	 * Attribut <code>kB</code> bzw. <code>BK</code>
	 */
	public static final PrognoseAttribut KB = null;
	
	/**
	 * Startwert für die Glättung (FS)
	 */
	private String fsStart = null;

	/**
	 * Glättungsparameter für abnehmende Messwerte (FS)
	 */
	private String fsAlpha1 = null;
	
	/**
	 * Glättungsparameter für steigende Messwerte (FS)
	 */
	private String fsAlpha2 = null;
	
	/**
	 * Prognoseparameter für abnehmende Messwerte (FS)
	 */
	private String fsBeta1 = null;
	
	/**
	 * Prognoseparameter für steigende Messwerte (FS)
	 */
	private String fsBeta2 = null;

	/**
	 * Startwert für die Glättung (MQ)
	 */
	private String mqStart = null;

	/**
	 * Glättungsparameter für abnehmende Messwerte (MQ)
	 */
	private String mqAlpha1 = null;
	
	/**
	 * Glättungsparameter für steigende Messwerte (MQ)
	 */
	private String mqAlpha2 = null;
	
	/**
	 * Prognoseparameter für abnehmende Messwerte (MQ)
	 */
	private String mqBeta1 = null;
	
	/**
	 * Prognoseparameter für steigende Messwerte (MQ)
	 */
	private String mqBeta2 = null;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param praefixFS das Attributpraefix bei Fahrstreifendaten
	 * @param praefixMQ das Attributpraefix bei Messquerschnittsdaten
	 * @param attributAllgemein der Name des allgeimeinen Attributs 
	 */
	private PrognoseAttribut(final String praefixFS,
							 final String praefixMQ,
							 final String attributAllgemein){
	}
	
	
	public String getAttributName(final boolean fuerFahrStreifen){
		return null;
	}
	
	public String getParameterStart(final boolean fuerFahrStreifen){
		return null;
	}
		
	/**
	 * Erfragt alle statischen Instanzen dieser Klasse
	 * 
	 * @return alle statischen Instanzen dieser Klasse
	 */
	public static final Set<PrognoseAttribut> getInstanzen(){
		return WERTE_BEREICH;
	}
	
}