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

import stauma.dav.clientside.Data;
import stauma.dav.clientside.ResultData;
import sys.funclib.debug.Debug;

/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer ein
 * Attribut eines Fahrstreifens bzw. eines Messquerschnittes durch
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DavAttributPrognoseObjekt
implements IAtgPrognoseParameterListener{

	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();
	
	/**
	 * Das Objekt, dessen Attribut hier betrachtet wird
	 */
	private PrognoseSystemObjekt prognoseObjekt = null;
	
	/**
	 * Das Attribut, das hier betrachtet wird
	 */
	private PrognoseAttribut attribut = null;
	
	/**
	 * DAV-Parameter <code>alpha1</code> dieses Attributs
	 */
	private double alpha1 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>alpha2</code> dieses Attributs
	 */
	private double alpha2 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>beta1</code> dieses Attributs
	 */
	private double beta1 = Double.NaN;
	
	/**
	 * DAV-Parameter <code>beta2</code> dieses Attributs
	 */
	private double beta2 = Double.NaN;
		
	/**
	 * Alter Wert fuer Z
	 */
	private double ZAlt = Double.NaN;
	
	/**
	 * Alter Wert fuer deltaZ
	 */
	private double deltaZAlt = 0.0;
	
	/**
	 * Prognosewert
	 */
	private long ZP = -4;
	
	/**
	 * geglaetteter Wert ohne Prognoseanteil
	 */
	private long ZG = -4;
	
	/**
	 * Erfragt, ob es sich bei diesem Attribut um ein
	 * Geschwindigkeitsattribut handelt
	 */
	private boolean vAttribut = false;
	
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param prognoseObjekt das Objekt, das hier betrachtet wird
	 * @param attribut das Attribut, das hier betrachtet wird
	 */
	public DavAttributPrognoseObjekt(final PrognoseSystemObjekt prognoseObjekt,
									 final PrognoseAttribut attribut){
		this.prognoseObjekt = prognoseObjekt;
		this.attribut = attribut;
		this.vAttribut = attribut.equals(PrognoseAttribut.V_KFZ) ||
						 attribut.equals(PrognoseAttribut.V_LKW) ||
						 attribut.equals(PrognoseAttribut.V_PKW);
	}
	

	/**
	 * Aktualisiert die Daten dieses Prognoseobjektes mit empfangenen Daten
	 * 
	 * @param resultat 
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzte wurden
	 */
	public final void aktualisiere(ResultData resultat)
	throws PrognoseParameterException{
		this.ueberpruefeParameter();
		String attributName = this.attribut.getAttributName(this.prognoseObjekt.isFahrStreifen());

		if(resultat.getData() != null){
			long ZAktuell = resultat.getData().getItem(attributName).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$

			if(this.vAttribut && 
			   resultat.getData().getItem(
					   this.attribut.getQAttributAnalogon(
						   this.prognoseObjekt.isFahrStreifen())).getUnscaledValue("Wert").longValue() <= 0){ //$NON-NLS-1$){
				/**
				 * 4. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 * Nach Messintervallen ohne Fahrzeugdetektion müssen alle geglätteten Geschwindigkeitswerte
				 * vom Vorgängerintervall übernommen werden.
				 */
 
			}
			
			
			if(ZAktuell >= 0){
				double alpha = this.alpha1;
				double beta = this.beta1;
				if(ZAktuell > ZAlt){
					alpha = this.alpha2;
					beta = this.beta2;
				}

				double ZNeu = alpha * ZAktuell + (1.0 - alpha) * ZAlt;
				double deltaZNeu = beta * (ZAktuell - ZAlt) + (1 - beta) * deltaZAlt;
				this.ZP = Math.round(ZNeu + deltaZNeu);
				/**
				 * 1. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 */
				if(this.ZP < 0){
					this.ZP = 0;
				}
				/**
				 * 2. Randbedingung SE-02.00.00.00.00-AFo-4.0, S.135
				 */
				else if(this.ZP == 0){
					deltaZNeu = 0;
				}						
				this.ZG = Math.round(ZNeu);

				this.ZAlt = ZNeu;
				this.deltaZAlt = deltaZNeu;
			}
		}
	}
	
	
	/**
	 * Exportiert die letzten hier errechneten geglaetteten Werte
	 * in das uebergebene Zieldatum
	 * 
	 * @param zielDatum ein veraenderbares Zieldatum der Attributgruppe
	 * <code>atg.verkehrsDatenKurzZeitGeglättetFs</code>
	 */
	public final void exportiereDatenGlatt(Data zielDatum){
		String attributName = this.attribut.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen());
		
		zielDatum.getItem(attributName).getUnscaledValue("Wert").set(ZG); //$NON-NLS-1$		
	}
	
	
	/**
	 * Exportiert die letzten hier errechneten Prognosewerte
	 * in das uebergebene Zieldatum
	 * 
	 * @param zielDatum ein veraenderbares Zieldatum der Attributgruppe
	 * <code>atg.verkehrsDatenKurzZeitTrendExtraPolationFs</code>
	 */
	public final void exportiereDatenPrognose(Data zielDatum){
		String attributName = this.attribut.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen());
		
		zielDatum.getItem(attributName).getUnscaledValue("Wert").set(ZP); //$NON-NLS-1$
		
//		qKfzP.Wert 
//		qKfzP.Status.Erfassung.NichtErfasst  
//		qKfzP.Status.PlFormal.WertMax  
//		qKfzP.Status.PlFormal.WertMin  
//		qKfzP.Status.PlLogisch.WertMaxLogisch  
//		qKfzP.Status.PlLogisch.WertMinLogisch  
//		qKfzP.Status.MessWertErsetzung.Implausibel  
//		qKfzP.Status.MessWertErsetzung.Interpoliert  
//		qKfzP.Güte.Index  
//		qKfzP.Güte.Verfahren 
	}

	
	/**
	 * Ueberprueft, ob die Parameter schon gesetzt wurden
	 * 
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzte wurden
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
	

	/**
	 * {@inheritDoc}
	 */
	public void aktualisiereParameter(PrognoseAttributParameter parameterSatzFuerAttribut) {
		this.ZAlt = parameterSatzFuerAttribut.getStart();
		this.alpha1 = parameterSatzFuerAttribut.getAlpha1();
		this.alpha2 = parameterSatzFuerAttribut.getAlpha2();
		this.beta1 = parameterSatzFuerAttribut.getBeta1();
		this.beta2 = parameterSatzFuerAttribut.getBeta2();
	}

}
