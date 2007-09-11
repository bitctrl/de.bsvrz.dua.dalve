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

import de.bsvrz.dua.dalve.prognose.PrognoseParameterException;

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
	 * Alter Wert fuer Z
	 */
	protected double ZAlt = Double.NaN;
	
	/**
	 * Alter Wert fuer deltaZ
	 */
	protected double deltaZAlt = 0.0;
	
	/**
	 * Alter Prognosewert
	 */
	protected long ZPAlt = -4;
	
	/**
	 * Prognosewert
	 */
	private long ZP = -4;
	
	/**
	 * geglaetteter Wert ohne Prognoseanteil
	 */
	private long ZG = -4;
	
	
	/**
	 * Berechnet die Glaettungsparameter alpha und beta und startet die
	 * Berechnung der Prognosewerte
	 * 
	 * @param ZAktuell aktueller Wert fuer den der geglaettete und der Prognoseteil
	 * berechnet werden soll
	 * @param istVAttributUndKeineVerkehrsStaerke indiziert, ob es sich
	 * hier um ein Geschwindigkeitsattribut handelt <b>und</b> dies ein 
	 * Messintervall ohne Fahrzeugdetektion ist
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzt wurden
	 */
	protected final void berechneGlaettungsParameterUndStart(
								final long ZAktuell,
								final boolean istVAttributUndKeineVerkehrsStaerke)
	throws PrognoseParameterException{
		this.ueberpruefeParameter();
		
		/**
		 * Fehlerhafte Werte werden vom Verfahren ignoriert
		 */		
		if(ZAktuell >= 0){
			double alpha = this.alpha1;
			double beta = this.beta1;

			if(deltaZAlt == 0){
				/**
				 * 5. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Ist der Trend DZNeu = 0, dann gelten die Glättungsfaktoren des letzten Glättungsintervalls
				 * mit Trend DZNeu = 0.
				 * Email H.C.Kniss (11.09.07):
				 * War der Trend im letzten Intervall 0, so werden die Glättungsparamter für den aktuellen
				 * Zyklus nicht geändert (man tut so, als ob sich der Trend der davor liegenden Zyklen fortsetzt).
				 */
				alpha = this.alphaAltBeiDeltaZNeuGleich0;
				beta = this.betaAltBeiDeltaZNeuGleich0;
			}else{
				if(ZAktuell > ZAlt){
					alpha = this.alpha2;
					beta = this.beta2;
				}
			}
			
			double ZNeu = alpha * ZAktuell + (1.0 - alpha) * ZAlt;
			double deltaZNeu;
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
				deltaZNeu = beta * (ZAktuell - ZAlt) + (1 - beta) * deltaZAlt;
				this.ZP = Math.round(ZNeu + deltaZNeu);
			}

			if(this.ZP < 0){
				/**
				 * 1. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Tritt bei der Kurzzeitprognose ein Wert ZP < 0 auf, so ist ZP = 0 zu setzen
				 */
				this.ZP = 0;
			}

			if(!(istVAttributUndKeineVerkehrsStaerke)){
				/**
				 * 4. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Nach Messintervallen ohne Fahrzeugdetektion müssen alle geglätteten Geschwindigkeitswerte
				 * vom Vorgängerintervall übernommen werden.<br>
				 * D.h. hier, dass ZG nur dann neu bestimmt wird, wenn dies kein Geschwindigkeitsattribut ist
				 * oder wenn es eines ist und Fahrzeuge detektiert wurden
				 */
				this.ZG = Math.round(ZNeu);
			}

			this.alphaAltBeiDeltaZNeuGleich0 = alpha;
			this.betaAltBeiDeltaZNeuGleich0 = beta;
			
			this.ZAlt = ZNeu;
			this.deltaZAlt = deltaZNeu;
			this.ZPAlt = ZP;
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
		if(this.ZAlt == Double.NaN){
			subjekt = "ZAlt";  //$NON-NLS-1$
		}else
		if(this.alpha1 == Double.NaN){
			subjekt = "alpha1";  //$NON-NLS-1$
		}else
		if(this.alpha2 == Double.NaN){
			subjekt = "alpha2";  //$NON-NLS-1$
		}else
		if(this.beta1 == Double.NaN){
			subjekt = "beta1";  //$NON-NLS-1$
		}else
		if(this.beta2 == Double.NaN){
			subjekt = "beta2";  //$NON-NLS-1$
		}

		if(subjekt != null){
			throw new PrognoseParameterException("Der Parameter " + //$NON-NLS-1$
					subjekt + " wurde noch nicht initialisiert"); //$NON-NLS-1$
		}
	}
}
