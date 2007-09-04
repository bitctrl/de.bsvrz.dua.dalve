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
package de.bsvrz.dua.dalve.prognose;

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
	public static final PrognoseAttribut Q_KFZ = new PrognoseAttribut("qKfz", "QKfz"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Attribut <code>qLkw</code> bzw. <code>QLkw</code>
	 */
	public static final PrognoseAttribut Q_LKW = new PrognoseAttribut("qLkw", "QLkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>qPkw</code> bzw. <code>QPkw</code>
	 */
	public static final PrognoseAttribut Q_PKW = new PrognoseAttribut("qPkw", "QPkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>vKfz</code> bzw. <code>VKfz</code>
	 */
	public static final PrognoseAttribut V_KFZ = new PrognoseAttribut("vKfz", "VKfz"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>vLkw</code> bzw. <code>VLkw</code>
	 */
	public static final PrognoseAttribut V_LKW = new PrognoseAttribut("vLkw", "VLkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>vPkw</code> bzw. <code>VPkw</code>
	 */
	public static final PrognoseAttribut V_PKW = new PrognoseAttribut("vPkw", "VPkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>aLkw</code> bzw. <code>ALkw</code>
	 */
	public static final PrognoseAttribut A_LKW = new PrognoseAttribut("aLkw", "ALkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>kKfz</code> bzw. <code>KKfz</code>
	 */
	public static final PrognoseAttribut K_KFZ = new PrognoseAttribut("kKfz", "KKfz"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>kLkw</code> bzw. <code>KLkw</code>
	 */
	public static final PrognoseAttribut K_LKW = new PrognoseAttribut("kLkw", "KLkw"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Attribut <code>kPkw</code> bzw. <code>KPkw</code>
	 */
	public static final PrognoseAttribut K_PKW = new PrognoseAttribut("kPkw", "KPkw"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>qB</code> bzw. <code>QB</code>
	 */
	public static final PrognoseAttribut QB = new PrognoseAttribut("qB", "QB"); //$NON-NLS-1$ //$NON-NLS-2$
	
	/**
	 * Attribut <code>kB</code> bzw. <code>BK</code>
	 */
	public static final PrognoseAttribut KB = new PrognoseAttribut("kB", "BK"); //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * der Name des geglaetteten Attributs (FS)
	 */
	private String nameFSGlatt = null;

	/**
	 * der Name des Prognoseattributs (FS)
	 */
	private String nameFSPrognose = null;

	/**
	 * der Name des geglaetteten Attributs (MQ)
	 */
	private String nameMQGlatt = null;

	/**
	 * der Name des Prognoseattributs (MQ)
	 */
	private String nameMQPrognose = null;

	/**
	 * der Name des Attributs (FS)
	 */
	private String nameFS = null;

	/**
	 * der Name des Attributs (MQ)
	 */
	private String nameMQ = null;

	/**
	 * Startwert für die Glättung (FS)
	 */
	private String fsStart = null;

	/**
	 * Startwert für die Glättung (MQ)
	 */
	private String mqStart = null;

	/**
	 * Der Name des mit diesem Geschwindigkeitsattribut korrespondierenden Verkehrsstärke-Attribut (FS)
	 */
	private String qAnalogon = null;

	/**
	 * Der Name des mit diesem Geschwindigkeitsattribut korrespondierenden Verkehrsstärke-Attribut (MQ)
	 */
	private String QAnalogon = null;
	
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param nameFS der Attributname bei Fahrstreifendaten z.B.
	 * <code>qKfz</code> oder <code>vKfz</code>
	 * @param nameMQ der Attributname bei Messquerschnittdaten z.B.
	 * <code>QKfz</code> oder <code>VKfz</code>
	 */
	private PrognoseAttribut(final String nameFS,
							 final String nameMQ){
		this.nameFS = nameFS;
		this.nameMQ = nameMQ;
		this.nameFSGlatt = nameFS + "G"; //$NON-NLS-1$
		this.nameMQGlatt = nameMQ + "G"; //$NON-NLS-1$
		this.nameFSPrognose = nameFS + "P"; //$NON-NLS-1$
		this.nameMQPrognose = nameMQ + "P"; //$NON-NLS-1$
		this.fsStart = nameFS + "Start"; //$NON-NLS-1$
		this.mqStart = nameMQ + "Start"; //$NON-NLS-1$
		if(this.nameFS.toLowerCase().equals("vkfz")){ //$NON-NLS-1$
			this.qAnalogon = "qKfz"; //$NON-NLS-1$
			this.QAnalogon = "QKfz"; //$NON-NLS-1$
		}else
		if(this.nameFS.toLowerCase().equals("vlkw")){ //$NON-NLS-1$
			this.qAnalogon = "qLkw"; //$NON-NLS-1$
			this.QAnalogon = "QLkw"; //$NON-NLS-1$
		}else
		if(this.nameFS.toLowerCase().equals("vpkw")){ //$NON-NLS-1$
			this.qAnalogon = "qPkw"; //$NON-NLS-1$
			this.QAnalogon = "QPkw"; //$NON-NLS-1$
		}
		WERTE_BEREICH.add(this);
	}
	
	
	/**
	 * Erfragt, so es sich bei diesem Attribut um ein Geschwindigkeitsattribut handelt,
	 * den Namen des mit diesem Geschwindigkeitsattribut korrespondierenden Verkehrsstärke-Attributs
	 * 
	 * @param fuerFahrStreifen das Objekt, fuer den der Name dieses
	 * Attributs erfragt wird
	 */
	public final String getQAttributAnalogon(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.qAnalogon:this.QAnalogon;
	}
	

	/**
	 * Erfragt den Namen dieses Attributs fuer geglaettete Werte
	 * 
	 * @param fuerFahrStreifen das Objekt, fuer den der Name dieses
	 * Attributs erfragt wird
	 * @return der Name dieses Attributs fuer geglaettete Werte
	 */
	public final String getAttributNameGlatt(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.nameFSGlatt:this.nameMQGlatt;
	}


	/**
	 * Erfragt den Namen dieses Attributs fuer Prognosewerte
	 * 
	 * @param fuerFahrStreifen das Objekt, fuer den der Name dieses
	 * Attributs erfragt wird
	 * @return der Name dieses Attributs fuer Prognosewerte
	 */
	public final String getAttributNamePrognose(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.nameFSPrognose:this.nameMQPrognose;
	}

	
	/**
	 * Erfragt den Namen dieses Attributs
	 * 
	 * @param fuerFahrStreifen das Objekt, fuer den der Name dieses
	 * Attributs erfragt wird
	 * @return der Name dieses Attributs
	 */
	public String getAttributName(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.nameFS:this.nameMQ;
	}
	
	
	/**
	 * Erfragt den Namen des Startwertes dieses Attributs
	 * 
	 * @param fuerFahrStreifen das Objekt, fuer den der Name des Startwertes 
	 * dieses Attributs erfragt wird
	 * @return der Name des Startwertes dieses Attributs
	 */
	public String getParameterStart(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.fsStart:this.mqStart;
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