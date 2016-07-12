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
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;

/**
 * Prognosetyp:<br>
 * - <code>Flink<code><br>
 * - <code>Normal<code><br>
 * - <code>Träge<code>
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class PrognoseTyp {
	
	/**
	 * Prognosewerte folgen den Messwerten sehr schnell, geringe Glättung
	 */
	public static PrognoseTyp FLINK = null;
	
	/**
	 * Prognosewerte folgen den Messwerten normal, normale Glättung
	 */
	public static PrognoseTyp NORMAL = null;
	
	/**
	 * Prognosewerte folgen den Messwerten sehr langsam, starke Glättung
	 */
	public static PrognoseTyp TRAEGE = null;

	/**
	 * Der Aspekt
	 */
	private Aspect aspekt = null;

	/**
	 * Parameterattributgruppe fuer Fahrstreifen
	 */
	private AttributeGroup atgFahrStreifen = null;
	
	/**
	 * Parameterattributgruppe fuer Messquerschnitte
	 */
	private AttributeGroup atgMessQuerschnitt = null;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 * @param name <code>Flink</code>, <code>Normal</code> oder <code>Träge</code>  
	 */
	private PrognoseTyp(final ClientDavInterface dav,
						final String name){
		this.aspekt = dav.getDataModel().getAspect("asp.prognose" + name); //$NON-NLS-1$
		this.atgFahrStreifen = dav.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognose" + name + "Fs");  //$NON-NLS-1$//$NON-NLS-2$
		this.atgMessQuerschnitt = dav.getDataModel().getAttributeGroup(
				"atg.verkehrsDatenKurzZeitTrendExtraPolationPrognose" + name + "Mq");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	
	/**
	 * Initialisiert alle statischen Objekte dieses Typs
	 * 
	 * @param dav Verbindung zum Datenverteiler
	 */
	public static final void initialisiere(final ClientDavInterface dav){
		FLINK = new PrognoseTyp(dav, "Flink"); //$NON-NLS-1$
		NORMAL = new PrognoseTyp(dav, "Normal"); //$NON-NLS-1$
		TRAEGE = new PrognoseTyp(dav, "Träge"); //$NON-NLS-1$
	}

	
	/**
	 * Erfragt den Aspekt, unter dem Daten dieses Typs publiziert werden sollen
	 * 
	 * @return der Aspekt, unter dem Daten dieses Typs publiziert werden sollen
	 */
	public final Aspect getAspekt(){
		return this.aspekt;
	}
	
	
	/**
	 * Erfragt die Parameterattributgruppe dieses Prognosetyps für einen
	 * Fahrstreifen oder einen Messquerschnitt
	 * 
	 * @param fuerFahrStreifen ob die Parameterattributgruppe für einen Fahrstreifen
	 * <code>true</code> oder einen Messquerschnitt <code>false</code> benötigt wird 
	 * @return die Parameterattributgruppe dieses Prognosetyps für einen
	 * Fahrstreifen oder einen Messquerschnitt
	 */
	public final AttributeGroup getParameterAtg(final boolean fuerFahrStreifen){
		return fuerFahrStreifen?this.atgFahrStreifen:this.atgMessQuerschnitt;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return this.aspekt.getPid().substring("asp.prognose".length());
	}
	
}
