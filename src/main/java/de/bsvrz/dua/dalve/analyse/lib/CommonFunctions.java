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

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.analyse.AtgVerkehrsDatenKurzZeitAnalyse;
import de.bsvrz.dua.dalve.analyse.AtgVerkehrsDatenKurzZeitAnalyseFs;
import de.bsvrz.dua.dalve.analyse.AtgVerkehrsDatenKurzZeitAnalyseMq;
import de.bsvrz.dua.dalve.prognose.DaMesswertUnskaliert;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.function.LongSupplier;

/**
 * Hilfsklasse für gemeinsame Berechnugen von AggrLve und DaLve
 *
 * @author Kappich Systemberatung
 */
public class CommonFunctions {
	
	private static final Debug _debug = Debug.getLogger();
	
	private final AtgVerkehrsDatenKurzZeitAnalyse parameter;
	private final boolean _isFs;
	
	private String _v;
	private String _q;
	private String _k;

	public CommonFunctions(ClientDavInterface dav, SystemObject object) {
		if(object.isOfType(DUAKonstanten.TYP_FAHRSTREIFEN)){
			this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseFs(dav, object);
			_isFs = true;
			_v = "v";
			_q = "q";
			_k = "k";
		}
		else {
			this.parameter = new AtgVerkehrsDatenKurzZeitAnalyseMq(dav, object);
			_isFs = false;
			_v = "V";
			_q = "Q";
			_k = "K";
		}
	}

	/**
	 * Berechnet den Lkw-Anteil <code>aLkw</code> (nach SE-02.00.00.00.00-AFo-4.0 S.117f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 */
	public void berechneLkwAnteil(Data analyseDatum){
		DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert(AnalyseAttribut.Q_LKW.getAttributName(_isFs), analyseDatum); 
		DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert(AnalyseAttribut.Q_KFZ.getAttributName(_isFs), analyseDatum); 
		MesswertUnskaliert aLkw = new MesswertUnskaliert(AnalyseAttribut.A_LKW.getAttributName(_isFs)); 
		
		if(qLkw.isFehlerhaftBzwImplausibel() ||
		   qKfz.isFehlerhaftBzwImplausibel()){
			aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{	
			if(qLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || 
			   qKfz.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				/**
				 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet, wird
				 * der Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
				 */
				aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}else{
				boolean nichtErmittelbarFehlerhaft = true;
				
				if(qLkw.getWertUnskaliert() == 0){
					aLkw.setWertUnskaliert(0);
					nichtErmittelbarFehlerhaft = false;
					if(qLkw.isPlausibilisiert() || qKfz.isPlausibilisiert()){
						aLkw.setInterpoliert(true);
					}
				}else{
					if(qKfz.getWertUnskaliert() > 0){
						long aLkwWert = Math.round((100.0 * ((double)qLkw.getWertUnskaliert()) / ((double)qKfz.getWertUnskaliert())));
						if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(AnalyseAttribut.A_LKW.getAttributName(_isFs)).getItem("Wert"), aLkwWert)){  
							aLkw.setWertUnskaliert(aLkwWert);
							

							
							if(qLkw.isPlausibilisiert() || qKfz.isPlausibilisiert()){
								aLkw.setInterpoliert(true);
							}
							nichtErmittelbarFehlerhaft = false;
						}
					}
				}
				
				if(nichtErmittelbarFehlerhaft){
					aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}
		}

		GWert qLkwGuete;
		try {
			qLkwGuete = new GWert(analyseDatum, AnalyseAttribut.Q_LKW.getAttributName(_isFs)); 
			GWert qKfzGuete = new GWert(analyseDatum, AnalyseAttribut.Q_KFZ.getAttributName(_isFs)); 
			GWert aLkwGuete = GueteVerfahren.quotient(qLkwGuete, qKfzGuete);

			aLkw.getGueteIndex().setWert(aLkwGuete.getIndexUnskaliert());
			aLkw.setVerfahren(aLkwGuete.getVerfahren().getCode());
		} catch (GueteException e) {
			_debug.error("Guete-Index fuer aLkw nicht berechenbar in " + analyseDatum, e); 
			e.printStackTrace();
		}

		aLkw.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die Bemessungsverkehrsstärke <code>qB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.117f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)

	 */
	public void berechneBemessungsVerkehrsStaerke(Data analyseDatum){
		MesswertUnskaliert qB = new MesswertUnskaliert(AnalyseAttribut.Q_B.getAttributName(_isFs)); 
		boolean interpoliert = false;
		try {
			if(parameter.isInitialisiert()) {
				double k1 = parameter.getFlk1();
				double k2 = parameter.getFlk2();

				DaMesswertUnskaliert vPkw = new DaMesswertUnskaliert(AnalyseAttribut.V_PKW.getAttributName(_isFs), analyseDatum); 
				DaMesswertUnskaliert vLkw = new DaMesswertUnskaliert(AnalyseAttribut.V_LKW.getAttributName(_isFs), analyseDatum); 
				DaMesswertUnskaliert qPkw = new DaMesswertUnskaliert(AnalyseAttribut.Q_PKW.getAttributName(_isFs), analyseDatum); 
				DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert(AnalyseAttribut.Q_LKW.getAttributName(_isFs), analyseDatum); 


				if(qLkw.isFehlerhaftBzwImplausibel() || vLkw.isFehlerhaftBzwImplausibel() || qLkw.isFehlerhaftBzwImplausibel()) {
					qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
				else {
					double fL;
					GWert gueteqPkw = new GWert(analyseDatum, AnalyseAttribut.Q_PKW.getAttributName(_isFs));
					GWert gueteqLkw = new GWert(analyseDatum, AnalyseAttribut.Q_LKW.getAttributName(_isFs));

					if(vLkw.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR || vLkw.getWertUnskaliert() == 0) {
						fL = k1;
					}
					else {
						if(vPkw.isFehlerhaftBzwImplausibel() || qPkw.isFehlerhaftBzwImplausibel()) {
							qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
							return;
						}
						else if(vPkw.getWertUnskaliert() <= vLkw.getWertUnskaliert()) {
							fL = k1;
						}
						else {
							fL = k1 + k2 * (vPkw.getWertUnskaliert() - vLkw.getWertUnskaliert());
						}
					}

					long qPkwValue = qPkw.getWertUnskaliert();
					long qLkwValue = qLkw.getWertUnskaliert();
					
					if(qPkwValue == DUAKonstanten.NICHT_ERMITTELBAR && qLkwValue == DUAKonstanten.NICHT_ERMITTELBAR){
						qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					}
					else {
						if(qPkwValue == DUAKonstanten.NICHT_ERMITTELBAR) qPkwValue = 0;
						if(qLkwValue == DUAKonstanten.NICHT_ERMITTELBAR) qLkwValue = 0;
						qB.setWertUnskaliert(Math.round(qPkwValue + fL * qLkwValue));
					}

					GWert gueteQB;
					try {
						gueteQB = GueteVerfahren.summe(gueteqPkw, gueteqLkw);
					}
					catch(GueteException e) {
						_debug.error("Guete von qB konnte nicht berechnet werden in " + analyseDatum, e);
						gueteQB = GWert.getNichtErmittelbareGuete(GueteVerfahren.getZustand(qB.getVerfahren()));
					}
					long indexUnskaliert = gueteQB.getIndexUnskaliert();
					qB.getGueteIndex().setWert(indexUnskaliert);
					qB.setVerfahren(gueteQB.getVerfahren().getCode());
					qB.setInterpoliert(qPkw.isPlausibilisiert() || qLkw.isPlausibilisiert() || interpoliert);
				}


			}
			else {
				qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}
		finally {
			qB.kopiereInhaltNach(analyseDatum);
		}
	}

	/**
	 * Berechnet die Bemessungsdichte <code>kB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)

	 * @param getPreviousValue
	 **/
	public void berechneBemessungsDichte(Data analyseDatum, final LongSupplier getPreviousValue){
		DaMesswertUnskaliert vT = new DaMesswertUnskaliert(AnalyseAttribut.V_KFZ.getAttributName(_isFs), analyseDatum); 
		DaMesswertUnskaliert qT = new DaMesswertUnskaliert(AnalyseAttribut.Q_B.getAttributName(_isFs), analyseDatum); 
		
		MesswertUnskaliert zielK = new MesswertUnskaliert(AnalyseAttribut.K_B.getAttributName(_isFs)); 
		
		if(vT.isFehlerhaftBzwImplausibel() ||
		   qT.isFehlerhaftBzwImplausibel()){
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}else{
			if(vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}
			else if(vT.getWertUnskaliert() == 0) {
				long kTminus1Wert = getPreviousValue.getAsLong();

				if(kTminus1Wert >= 0) {
					if(parameter.isInitialisiert()) {
						long kGrenz;
						long kMax;

						kGrenz = parameter.getKBGrenz();
						kMax = parameter.getKBMax();

						if(kGrenz != DUAKonstanten.FEHLERHAFT &&
								kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT &&
								kMax != DUAKonstanten.FEHLERHAFT &&
								kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
							if(kGrenz == DUAKonstanten.NICHT_ERMITTELBAR) {
								zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
							}
							else {
								if(kTminus1Wert < kGrenz) {
									zielK.setWertUnskaliert(0);
								}
								else {
									zielK.setWertUnskaliert(Math.max(kMax, kTminus1Wert));
								}
							}
						}
					}
				}
				else {
					zielK.setWertUnskaliert(0);
				}
			}
			else{
				if(qT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
					zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
				}else{
					zielK.setWertUnskaliert(Math.round((double)qT.getWertUnskaliert() / (double)vT.getWertUnskaliert()));
					
					/**
					 * Aenderung analog Mail von Herrn Kappich vom 27.03.08, 1400 
					 */

					if(parameter.isInitialisiert()){
						final long kMax = parameter.getKBMax();
	
						if(kMax >= 0){
							if(zielK.getWertUnskaliert() > kMax){
								zielK.setWertUnskaliert(kMax);
							}
						}
					}

					if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(AnalyseAttribut.K_KFZ.getAttributName(_isFs)).getItem("Wert"), zielK.getWertUnskaliert())){  
						if(qT.isPlausibilisiert() || vT.isPlausibilisiert()){
							zielK.setInterpoliert(true);
						}
					}else{
						zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
					}
				}
			}
			
			if(zielK.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT){
				zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			}
		}

		try {
			GWert qGuete = new GWert(analyseDatum, AnalyseAttribut.Q_B.getAttributName(_isFs)); 
			GWert vGuete = new GWert(analyseDatum, AnalyseAttribut.V_KFZ.getAttributName(_isFs)); 
			GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
			zielK.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
			zielK.setVerfahren(kGuete.getVerfahren().getCode());
		} catch (GueteException e) {
			_debug.error("Guete-Index fuer kB nicht berechenbar in " + analyseDatum, e); 
			e.printStackTrace();
		}

		zielK.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Berechnet die Fahrzeugdichte (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 * @param fahrZeugKlasse eine Fahrzeugklasse, für die der Analysewert ermittelt werden soll

	 * @param getPreviousValue
	 */
	public void berechneDichte(Data analyseDatum, String fahrZeugKlasse,  final LongSupplier getPreviousValue){
		DaMesswertUnskaliert vT = new DaMesswertUnskaliert(_v + fahrZeugKlasse, analyseDatum);
		DaMesswertUnskaliert qT = new DaMesswertUnskaliert(_q + fahrZeugKlasse, analyseDatum);
		MesswertUnskaliert zielK = new MesswertUnskaliert(_k + fahrZeugKlasse); 
				
		if(vT.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT && 
		   qT.getWertUnskaliert() != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT){

			if(vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
				zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}
			else if(vT.getWertUnskaliert() == 0) {
				long kTminus1Wert = getPreviousValue.getAsLong();

				if(parameter.isInitialisiert()) {
					long kGrenz;
					long kMax;

					if(fahrZeugKlasse.startsWith("K")) {    // Kfz 
						kGrenz = parameter.getKKfzGrenz();
						kMax = parameter.getKKfzMax();
					}
					else if(fahrZeugKlasse.startsWith("L")) {    // Lkw 
						kGrenz = parameter.getKLkwGrenz();
						kMax = parameter.getKLkwMax();
					}
					else {    // Pkw
						kGrenz = parameter.getKPkwGrenz();
						kMax = parameter.getKPkwMax();
					}

					if(kGrenz != DUAKonstanten.FEHLERHAFT &&
							kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT &&
							kMax != DUAKonstanten.FEHLERHAFT &&
							kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
						if(kGrenz == DUAKonstanten.NICHT_ERMITTELBAR) {
							zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
						}
						else {
							if(kTminus1Wert < kGrenz && kTminus1Wert >= 0) {
								zielK.setWertUnskaliert(0);
							}
							else {
								zielK.setWertUnskaliert(Math.max(kMax, kTminus1Wert));
							}
						}
					}
				}
			}
			else {
				if(qT.isFehlerhaftBzwImplausibel()) {
					zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
				else {

					if(qT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR) {
						zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
					}
					else {
						zielK.setWertUnskaliert(Math.round((double) qT.getWertUnskaliert() / (double) vT.getWertUnskaliert()));


						/**
						 * Aenderung analog Mail von Herrn Kappich vom 27.03.08, 1400 
						 */
						if(parameter.isInitialisiert()) {
							long kGrenz;
							long kMax;

							if(fahrZeugKlasse.startsWith("K")) {    // Kfz 
								kGrenz = parameter.getKKfzGrenz();
								kMax = parameter.getKKfzMax();
							}
							else if(fahrZeugKlasse.startsWith("L")) {    // Lkw 
								kGrenz = parameter.getKLkwGrenz();
								kMax = parameter.getKLkwMax();
							}
							else {    // Pkw
								kGrenz = parameter.getKPkwGrenz();
								kMax = parameter.getKPkwMax();
							}

							if(kGrenz != DUAKonstanten.FEHLERHAFT &&
									kGrenz != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT &&
									kMax != DUAKonstanten.FEHLERHAFT &&
									kMax != DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT) {
								if(zielK.getWertUnskaliert() > kMax) {
									zielK.setWertUnskaliert(kMax);
								}
							}
						}


						if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(_k + fahrZeugKlasse)
								                                      .getItem("Wert"), zielK.getWertUnskaliert())) {  

							if(qT.isPlausibilisiert() || vT.isPlausibilisiert()) {
								zielK.setInterpoliert(true);
							}
						}
						else {
							zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
						}
					}
				}
			}
		}

		if(zielK.getWertUnskaliert() == DUAKonstanten.MESSWERT_UNBEKANNT){
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}

		try{
			GWert qGuete = new GWert(analyseDatum, _q + fahrZeugKlasse); 
			GWert vGuete = new GWert(analyseDatum, _v + fahrZeugKlasse); 
			GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
			zielK.getGueteIndex().setWert(kGuete.getIndexUnskaliert());
			zielK.setVerfahren(kGuete.getVerfahren().getCode());
		} catch (GueteException e) {
			_debug.error("Guete-Index fuer k" + fahrZeugKlasse + 
					                        " nicht berechenbar in " + analyseDatum, e); 
			e.printStackTrace();
		}

		zielK.kopiereInhaltNach(analyseDatum);


	}

	/**
	 * Berechnet die Verkehrsstärken analog SE-02.00.00.00.00-AFo-4.0 S.116f
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param kurzZeitDatum das Roh-KZ-Datum
	 * @param attName der Attributname des Verkehrswertes, der berechnet werden soll
	 */
	public void berechneVerkehrsStaerke(Data analyseDatum, ResultData kurzZeitDatum, String attName){
		Data sourceData = kurzZeitDatum.getData();
		
		assert sourceData != null;
		
		long TinS = sourceData.getTimeValue("T").getMillis() / 1000; 
		DaMesswertUnskaliert qMwe = new DaMesswertUnskaliert(attName, sourceData);
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert(attName, sourceData);
		
		if(qMwe.isFehlerhaftBzwImplausibel()){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			qAnalyse.setImplausibel(false); // DUA-25 Flag Implausibel löschen
		}else{
			/**
			 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet, wird
			 * der Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
			 */
			if(qMwe.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}else{
				boolean nichtErmittelbarFehlerhaft = true;
				if(TinS > 0){
					long q = Math.round(((double)qMwe.getWertUnskaliert() * 3600.0 / (double)TinS));
					if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(attName).getItem("Wert"), q)){ 
						qAnalyse.setWertUnskaliert(q);
				
						if(qMwe.isPlausibilisiert()){
							qAnalyse.setInterpoliert(true);
							qAnalyse.setFormalMax(false);
							qAnalyse.setFormalMin(false);
							qAnalyse.setLogischMax(false);
							qAnalyse.setLogischMin(false);
						}
						nichtErmittelbarFehlerhaft = false;
					}
				}
				
				if(nichtErmittelbarFehlerhaft){
					qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
				}
			}			
		}

		qAnalyse.setGueteIndex(qMwe.getGueteIndex());
		qAnalyse.setVerfahren(qMwe.getVerfahren());

		qAnalyse.kopiereInhaltNach(analyseDatum);
	}

	/**
	 * Übernimmt einen Wert aus dem Kurzzeitdatum in das Analysedatum
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param kurzZeitDatum das Roh-KZ-Datum
	 * @param attName der Attributname des Verkehrswertes, der übernommen werden soll
	 */
	public void uebernehmeWert(Data analyseDatum, ResultData kurzZeitDatum, String attName){
		DaMesswertUnskaliert mweWert = new DaMesswertUnskaliert(attName, kurzZeitDatum.getData());
		MesswertUnskaliert analyseWert = new MesswertUnskaliert(attName, kurzZeitDatum.getData());
		
		if(mweWert.isFehlerhaftBzwImplausibel()){
			analyseWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
			analyseWert.setImplausibel(false); // DUA-25 Flag Implausibel löschen
		}else{
			/**
			 * Ist eine der bei der Berechnung beteiligten Größen als nicht ermittelbar gekennzeichnet, wird
			 * der Zielwert mit den Statusflags nicht ermittelbar gekennzeichnet.
			 */
			if(mweWert.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
				mweWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR);
			}else{
				analyseWert.setWertUnskaliert(mweWert.getWertUnskaliert());
				if(mweWert.isPlausibilisiert()){
					analyseWert.setInterpoliert(true);
					analyseWert.setFormalMax(false);
					analyseWert.setFormalMin(false);
					analyseWert.setLogischMax(false);
					analyseWert.setLogischMin(false);
				}
			}
			analyseWert.setGueteIndex(mweWert.getGueteIndex());
			analyseWert.setVerfahren(mweWert.getVerfahren());
		}

		analyseWert.kopiereInhaltNach(analyseDatum);
	}
}
