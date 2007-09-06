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

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dua.dalve.AbstraktAttributPrognoseObjekt;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;

/**
 * Fuehrt die Berechnung der Prognosewerte bzw. der geglaetteten Werte fuer ein
 * Attribut eines Fahrstreifens bzw. eines Messquerschnittes durch
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class DavAttributPrognoseObjekt
extends AbstraktAttributPrognoseObjekt
implements IAtgPrognoseParameterListener{
	
	/**
	 * Das Objekt, dessen Attribut hier betrachtet wird
	 */
	private PrognoseSystemObjekt prognoseObjekt = null;
	
	/**
	 * Das Attribut, das hier betrachtet wird
	 */
	private PrognoseAttribut attribut = null;
		
	/**
	 * Erfragt, ob es sich bei diesem Attribut um ein
	 * Geschwindigkeitsattribut handelt
	 */
	private boolean vAttribut = false;
	
	/**
	 * aktuelles Datum
	 */
	private DaMesswertUnskaliert aktuellesDatum = null; 
	
	/**
	 * der Name des Attributs, das hier betrachtet wird (Daten-Quelle) Prognose
	 */
	private String attributNameP = null; 

	/**
	 * der Name des Attributs, das hier betrachtet wird (Daten-Quelle) glatt
	 */
	private String attributNameG = null;
	
	/**
	 * der Name des Attributs, das hier betrachtet wird (Daten-Ziel)
	 */
	private String attributNameQuelle = null; 

	
	
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
		this.attributNameP = this.attribut.getAttributNamePrognose(this.prognoseObjekt.isFahrStreifen());
		this.attributNameG = this.attribut.getAttributNameGlatt(this.prognoseObjekt.isFahrStreifen());
		this.attributNameQuelle = this.attribut.getAttributName(prognoseObjekt.isFahrStreifen());
	}
	

	/**
	 * Aktualisiert die Daten dieses Prognoseobjektes mit empfangenen Daten
	 * 
	 * @param resultat ein empfangenes Analysedatum 
	 * @throws PrognoseParameterException wenn die Parameter noch nicht gesetzte wurden
	 */
	public final void aktualisiere(ResultData resultat)
	throws PrognoseParameterException{

		if(resultat.getData() != null){
			this.aktuellesDatum = new DaMesswertUnskaliert(attributNameQuelle, resultat.getData()); 
			long ZAktuell = resultat.getData().getItem(attributNameQuelle).getUnscaledValue("Wert").longValue(); //$NON-NLS-1$
						
			/**
			 * Messintervallen ohne Fahrzeugdetektion?
			 */
			boolean keineVerkehrsStaerke = false;
			if(this.vAttribut){
				keineVerkehrsStaerke = resultat.getData().getItem(this.attribut.getQAttributAnalogon(
								   this.prognoseObjekt.isFahrStreifen())).getUnscaledValue("Wert").longValue() <= 0; //$NON-NLS-1$
			}

			this.berechneGlaettungsParameterUndStart(ZAktuell, keineVerkehrsStaerke);
		}else{
			this.aktuellesDatum = null;
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
		MesswertUnskaliert exportWert = new MesswertUnskaliert(this.attributNameG);			
			
		exportWert.setWertUnskaliert(this.getZG());
		exportWert.setNichtErfasst(this.aktuellesDatum.isNichtErfasst());
		exportWert.setInterpoliert(this.aktuellesDatum.isPlausibilisiert());
		exportWert.getGueteIndex().setWert(this.aktuellesDatum.getGueteIndex().getWert());
		exportWert.setVerfahren(this.aktuellesDatum.getVerfahren());
		
		exportWert.kopiereInhaltNach(zielDatum);
	}
	
	
	/**
	 * Exportiert die letzten hier errechneten Prognosewerte
	 * in das uebergebene Zieldatum
	 * 
	 * @param zielDatum ein veraenderbares Zieldatum der Attributgruppe
	 * <code>atg.verkehrsDatenKurzZeitTrendExtraPolationFs</code>
	 */
	public final void exportiereDatenPrognose(Data zielDatum){		
		MesswertUnskaliert exportWert = new MesswertUnskaliert(this.attributNameP);
		
		exportWert.setWertUnskaliert(this.getZP());
		exportWert.setNichtErfasst(this.aktuellesDatum.isNichtErfasst());
		exportWert.setInterpoliert(this.aktuellesDatum.isPlausibilisiert());
		exportWert.getGueteIndex().setWert(this.aktuellesDatum.getGueteIndex().getWert());
		exportWert.setVerfahren(this.aktuellesDatum.getVerfahren());
		
		exportWert.kopiereInhaltNach(zielDatum);
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
