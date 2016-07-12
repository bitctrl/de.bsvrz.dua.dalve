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
package de.bsvrz.dua.dalve;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;

/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer ein
 * Messwertattribut durch (analog SE-02.00.00.00.00-AFo-4.0, S. 135).<br>
 * <b>Achtung:</b> Diese Implementierung geht davon aus, dass nur Attribute verarbeitet
 * werden, die die Zustaende <code>fehlerhaft</code>, <code>nicht ermittelbar</code>
 * und <code>fehlerhaft/nicht ermittelbar</code> in den Werten -1, -2 und -3 
 * besitzen. Werte mit einem dieser Zustände werden igniriert.
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class AbstraktAttributPrognoseObjekt{

	/**
	 * DAV-Parameter <code>alpha</code>  des letzten 
	 * Glaettungsintervalls mit Trend deltaZNeu = 0
	 */
	private double alphaAltBeiDeltaZNeuGleich0 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>beta</code>  des letzten 
	 * Glaettungsintervalls mit Trend deltaZNeu = 0
	 */
	private double betaAltBeiDeltaZNeuGleich0 = Double.NaN;	
	
	/**
	 * DAV-Parameter <code>alpha1</code> dieses Attributs
	 */
	protected double alpha1 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>alpha2</code> dieses Attributs
	 */
	protected double alpha2 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>beta1</code> dieses Attributs
	 */
	protected double beta1 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>beta2</code> dieses Attributs
	 */
	protected double beta2 = Double.NaN;
		
	/**
	 * initialialer Wert fuer ZAlt
	 */
	protected double ZAltInit = Double.NaN;
	
	/**
	 * Alter Wert fuer deltaZ
	 */
	protected long deltaZAlt = 0;
	
	/**
	 * Alter Prognosewert
	 */
	protected long ZPAlt = -4;
	
	/**
	 * Prognosewert
	 */
	private long ZP = DUAKonstanten.NICHT_ERMITTELBAR;

	/**
	 * geglaetteter Wert ohne Prognoseanteil
	 */
	private long ZG = DUAKonstanten.NICHT_ERMITTELBAR;
	/**
	 * Alter Wert fuer Z
	 */
	private long ZAlt = DUAKonstanten.NICHT_ERMITTELBAR;
	
	/**
	 * Wurde das Programm gerade gestartet?
	 */
	private boolean start = true;

	
	
	/**
	 * Berechnet die Glaettungsparameter alpha und beta und startet die
	 * Berechnung der Prognosewerte
	 * 
	 * @param ZAktuell aktueller Wert fuer den der geglaettete und der Prognoseteil
	 * berechnet werden soll
	 * @param implausibel zeigt an, ob das Attribut als implausibel markiert ist
	 * @param istVAttributUndKeineVerkehrsStaerke indiziert, ob es sich
	 * hier um ein Geschwindigkeitsattribut handelt <b>und</b> dies ein 
	 * Messintervall ohne Fahrzeugdetektion ist
	 * @param davDatum das DAV-Datum, aus dem der Z-Wert entnommen wurde bzw.
	 * <code>null</code>, wenn nicht auf Wertebereiche geachtet werden soll
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzt wurden
	 */
	protected final void berechneGlaettungsParameterUndStart(
								final long ZAktuell,
								final boolean implausibel,
								final boolean istVAttributUndKeineVerkehrsStaerke,
								Data davDatum)
	throws PrognoseParameterException{
		this.ueberpruefeParameter();
		
		if(this.ZAlt == DUAKonstanten.NICHT_ERMITTELBAR){
			this.ZAlt = Math.round(this.ZAltInit);
		}
		
		/**
		 * Fehlerhafte Werte werden vom Verfahren ignoriert
		 */	
		if(istVAttributUndKeineVerkehrsStaerke){
			ZP = ZAlt;
			deltaZAlt = 0;
		}
		else if(ZAktuell >= 0 && !implausibel){
			double alpha = this.alphaAltBeiDeltaZNeuGleich0;
			double beta = this.betaAltBeiDeltaZNeuGleich0;

			if(start || this.deltaZAlt != 0){
				start = false;
				/**
				 * 5. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Ist der Trend DZNeu = 0, dann gelten die Glättungsfaktoren des letzten Glättungsintervalls
				 * mit Trend DZNeu = 0.
				 * Email H.C.Kniss (11.09.07):
				 * War der Trend im letzten Intervall 0, so werden die Glättungsparamter für den aktuellen
				 * Zyklus nicht geändert (man tut so, als ob sich der Trend der davor liegenden Zyklen fortsetzt).
				 */
				if(ZAktuell > ZAlt){
					alpha = this.alpha2;
					beta = this.beta2;
				}else{
					alpha = this.alpha1;
					beta = this.beta1;					
				}
			}
			
			long ZNeu = Math.round(alpha * ZAktuell + (1.0 - alpha) * ZAlt);
			long deltaZNeu;
			if(this.ZPAlt == 0){
				/**
				 * 2. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Tritt bei der Kurzzeitprognose ein Wert ZP = 0 auf, so muss der nächste Messwert direkt als
				 * Ergebnis der Prognoserechnung übernommen und der alte Trend auf 0 gesetzt werden
				 * Email H.C.Kniss (11.09.07):
				 * War ZP = 0 so ist im aktuellen Zyklus der Messwert als Prognosewert zu verwenden. 
				 * Der Trend wird dabei zu 0 gesetzt, weil in diesem Fall kein Trend ermittelbar ist.
				 */
				deltaZNeu = 0;
				this.ZP = ZAktuell;
			}else{				
				deltaZNeu = Math.round(beta * (ZAktuell - ZAlt) + (1 - beta) * deltaZAlt);
				this.ZP = ZNeu + deltaZNeu;
			}

			if(this.ZP < 0){
				/**
				 * 1. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Tritt bei der Kurzzeitprognose ein Wert ZP < 0 auf, so ist ZP = 0 zu setzen
				 */
				this.ZP = 0;
			}

			this.ZG = ZNeu;

			this.alphaAltBeiDeltaZNeuGleich0 = alpha;
			this.betaAltBeiDeltaZNeuGleich0 = beta;
			
			this.ZAlt = ZNeu;
			this.deltaZAlt = deltaZNeu;
			this.ZPAlt = ZP;
		}else{
			/**
			 * Ausgangzustand wieder herstellen
			 */
			this.deltaZAlt = 0;
			this.ZPAlt = -4;
			this.ZAlt = DUAKonstanten.NICHT_ERMITTELBAR;
			this.ZP = DUAKonstanten.NICHT_ERMITTELBAR;
			this.ZG = DUAKonstanten.NICHT_ERMITTELBAR;
			
			start = true;
			//this.alphaAltBeiDeltaZNeuGleich0 = Double.NaN;
			//this.betaAltBeiDeltaZNeuGleich0 = Double.NaN;		
		}
		
		if(davDatum != null && !DUAUtensilien.isWertInWerteBereich(davDatum, ZP)){
			/**
			 * Ausgangzustand wieder herstellen
			 */
			this.ZPAlt = -4;
			this.ZP = DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT;
			
			start = true;
			//this.alphaAltBeiDeltaZNeuGleich0 = Double.NaN;
			//this.betaAltBeiDeltaZNeuGleich0 = Double.NaN;		
		}
	}


	/**
	 * Erfragt den aktuellen Prognosewert <code>ZP</code>
	 * 
	 * @return der aktuelle Prognosewert <code>ZP</code>
	 */
	public final long getZP() {
		return ZP;
	}


	/**
	 * Erfragt den aktuellen geglaetteten Wert <code>ZG</code>
	 * 
	 * @return der aktuelle geglaettete Wert <code>ZG</code>
	 */
	public final long getZG() {
		return ZG;
	}

	/**
	 * Ueberprueft, ob die Parameter schon gesetzt wurden
	 * 
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzt wurden
	 */
	private final void ueberpruefeParameter()
	throws PrognoseParameterException{
		String subjekt = null;
		if(Double.isNaN(this.ZAltInit)){
			subjekt = "ZAlt";  //$NON-NLS-1$
		}else
		if(Double.isNaN(this.alpha1)){
			subjekt = "alpha1";  //$NON-NLS-1$
		}else
		if(Double.isNaN(this.alpha2)){
			subjekt = "alpha2";  //$NON-NLS-1$
		}else
		if(Double.isNaN(this.beta1)){
			subjekt = "beta1";  //$NON-NLS-1$
		}else
		if(Double.isNaN(this.beta2)){
			subjekt = "beta2";  //$NON-NLS-1$
		}

		if(subjekt != null){
			throw new PrognoseParameterException("Der Parameter " + //$NON-NLS-1$
					subjekt + " wurde noch nicht initialisiert"); //$NON-NLS-1$
		}
	}
}
