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
package de.bsvrz.dua.dalve.fs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import stauma.dav.clientside.Data;
import stauma.dav.clientside.DataDescription;
import stauma.dav.clientside.ResultData;
import stauma.dav.clientside.SenderRole;
import stauma.dav.configuration.interfaces.SystemObject;
import sys.funclib.debug.Debug;
import de.bsvrz.dua.dalve.DaMesswertUnskaliert;
import de.bsvrz.dua.dalve.fs.atg.AtgVerkehrsDatenKurzZeitAnalyseFs;
import de.bsvrz.dua.guete.GWert;
import de.bsvrz.dua.guete.GueteException;
import de.bsvrz.dua.guete.GueteVerfahren;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.adapter.AbstraktBearbeitungsKnotenAdapter;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVSendeAnmeldungsVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.schnittstellen.IDatenFlussSteuerung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.typen.ModulTyp;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.bitctrl.konstante.Konstante;

/**
 * Bekommt die messwertersetzten KZD der Fahrstreifen übergeben und produziert
 * aus Diesen Analysedaten nach den Vorgaben der AFo.<br>
 * (siehe SE-02.00.00.00.00-AFo-4.0 S.115f) 
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class FsAnalyseModul 
extends AbstraktBearbeitungsKnotenAdapter{
	
	/**
	 * Debug-Logger
	 */
	private static final Debug LOGGER = Debug.getLogger();

	/**
	 * Datenbeschreibung zur Publikation der Fahrstreifen-Analysedaten
	 */
	protected static DataDescription PUB_BESCHREIBUNG = null; 
	
	/**
	 * Mapt jeden Fahrstreifen auf seine aktuellen Analyse-Parameter
	 */
	private Map<SystemObject, AtgVerkehrsDatenKurzZeitAnalyseFs> parameter = 
					new HashMap<SystemObject, AtgVerkehrsDatenKurzZeitAnalyseFs>();

	/**
	 * Mapt jeden Fahrstreifen auf seinen letzten Analysedatensatz
	 */
	private Map<SystemObject, ResultData> fsAufDatenPuffer = 
					new HashMap<SystemObject, ResultData>();

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialisiere(IVerwaltung dieVerwaltung)
	throws DUAInitialisierungsException {
		super.initialisiere(dieVerwaltung);
		
		PUB_BESCHREIBUNG = new DataDescription(
				dieVerwaltung.getVerbindung().getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitFs"), //$NON-NLS-1$
				dieVerwaltung.getVerbindung().getDataModel().getAspect("asp.analyse"), //$NON-NLS-1$
				(short)0);
		
		/**
		 * Publikations- und Parameteranmeldungen durchführen
		 */
		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for(SystemObject fsObj:dieVerwaltung.getSystemObjekte()){
			this.parameter.put(fsObj, 
					new AtgVerkehrsDatenKurzZeitAnalyseFs(dieVerwaltung.getVerbindung(), fsObj));
			try {
				anmeldungen.add(new DAVObjektAnmeldung(fsObj, PUB_BESCHREIBUNG));
			} catch (Exception e) {
				throw new DUAInitialisierungsException(Konstante.LEERSTRING, e); 
			}
		}
	
		this.publikationsAnmeldungen.modifiziereObjektAnmeldung(anmeldungen);		
	}


	/**
	 * {@inheritDoc}
	 */
	public void aktualisiereDaten(ResultData[] resultate) {
		if(resultate != null){
			for(ResultData resultat:resultate){
				if(resultat != null){
					ResultData analyseDatum = getAnalyseDatum(resultat);
					if(analyseDatum != null){
						this.fsAufDatenPuffer.put(resultat.getObject(), analyseDatum);
						this.publikationsAnmeldungen.sende(analyseDatum);
					}						
				}
			}
		}
	}

	
	/**
	 * Berechnet ein Analysedatum eines Fahrstreifens in Bezug auf den übergebenen KZ-Datensatz
	 * 
	 * @param kurzZeitDatum ein aktueller KZ-Datensatz
	 * @return ein Analysedatum eines Fahrstreifens in Bezug auf den übergebenen KZ-Datensatz oder
	 * <code>null</code>, wenn dieses nicht ermittelt werden konnte
	 */
	private final ResultData getAnalyseDatum(final ResultData kurzZeitDatum){
		ResultData ergebnis = null;
				
		if(kurzZeitDatum.getData() != null){
			Data analyseDatum = this.verwaltung.getVerbindung().createData(PUB_BESCHREIBUNG.getAttributeGroup());			
			
			analyseDatum.getTimeValue("T").setMillis(kurzZeitDatum.getData().getTimeValue("T").getMillis());  //$NON-NLS-1$//$NON-NLS-2$
			
			/**
			 * Berechne Verkehrsstärken
			 */
			this.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qKfz"); //$NON-NLS-1$
			this.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qLkw"); //$NON-NLS-1$
			this.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qPkw"); //$NON-NLS-1$
			
			/**
			 * Mittlere Geschwindigkeiten,
			 * Belegung und Standardabweichung werden einfach übernommen
			 */
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "vKfz"); //$NON-NLS-1$
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "vLkw"); //$NON-NLS-1$
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "vPkw"); //$NON-NLS-1$
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "vgKfz"); //$NON-NLS-1$
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "b"); //$NON-NLS-1$
			this.uebernehmeWert(analyseDatum, kurzZeitDatum, "sKfz"); //$NON-NLS-1$
			
			/**
			 * Berechne Lkw-Anteil (ab hier sind Analysedaten selbst Eingangswerte)
			 */
			this.berechneLkwAnteil(analyseDatum, kurzZeitDatum);
			
			/**
			 * Berechne Dichten
			 */
			this.berechneDichte(analyseDatum, kurzZeitDatum, "Kfz"); //$NON-NLS-1$
			this.berechneDichte(analyseDatum, kurzZeitDatum, "Lkw"); //$NON-NLS-1$
			this.berechneDichte(analyseDatum, kurzZeitDatum, "Pkw"); //$NON-NLS-1$
			
			/**
			 * Berechne Bemessungsverkehrsstärke
			 */
			this.berechneBemessungsVerkehrsStaerke(analyseDatum, kurzZeitDatum);
			
			/**
			 * Berechne Bemessungsdichte
			 */
			this.berechneBemessungsDichte(analyseDatum, kurzZeitDatum);
			
			ergebnis = new ResultData(kurzZeitDatum.getObject(), PUB_BESCHREIBUNG, 
					kurzZeitDatum.getDataTime(), analyseDatum);
		}else{
			ResultData letzterPublizierterWert = this.fsAufDatenPuffer.get(kurzZeitDatum.getObject());
			if( letzterPublizierterWert == null || letzterPublizierterWert.getData() != null ){
				ergebnis = new ResultData(kurzZeitDatum.getObject(), PUB_BESCHREIBUNG,
					kurzZeitDatum.getDataTime(), null);
			}
		}
		
		return ergebnis;
	}
	
	
	/**
	 * Berechnet die Bemessungsverkehrsstärke <code>qB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.117f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 */
	private final void berechneLkwAnteil(Data analyseDatum, ResultData kurzZeitDatum){
		DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert("qLkw", analyseDatum); //$NON-NLS-1$
		DaMesswertUnskaliert qKfz = new DaMesswertUnskaliert("qKfz", analyseDatum); //$NON-NLS-1$
		MesswertUnskaliert aLkw = new MesswertUnskaliert("aLkw"); //$NON-NLS-1$
		boolean nichtErmittelbarFehlerhaft = true;
		if(qLkw.getWertUnskaliert() >= 0 && qKfz.getWertUnskaliert() > 0){
			long aLkwWert = Math.round((100.0 * ((double)qLkw.getWertUnskaliert()) / ((double)qKfz.getWertUnskaliert())));
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("aLkw"), aLkwWert)){ //$NON-NLS-1$
				aLkw.setWertUnskaliert(aLkwWert);
				
				GWert qLkwGuete;
				try {
					qLkwGuete = new GWert(kurzZeitDatum.getData().getItem("qLkw").getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
					GWert qKfzGuete = new GWert(kurzZeitDatum.getData().getItem("qKfz").getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
					GWert aLkwGuete = GueteVerfahren.quotient(qLkwGuete, qKfzGuete);
					aLkw.setGueteIndex(aLkwGuete.getIndex());
					aLkw.setVerfahren(aLkwGuete.getVerfahren().getCode());
				} catch (GueteException e) {
					LOGGER.error("Guete-Index fuer aLkw nicht berechenbar", e); //$NON-NLS-1$
					e.printStackTrace();
				}
				
				if(qLkw.isPlausibilisiert() || qKfz.isPlausibilisiert()){
					aLkw.setInterpoliert(true);
				}
				nichtErmittelbarFehlerhaft = false;
			}
		}
		if(nichtErmittelbarFehlerhaft){
			aLkw.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		aLkw.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet die Bemessungsverkehrsstärke <code>qB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.117f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 */
	private final void berechneBemessungsVerkehrsStaerke(Data analyseDatum, ResultData kurzZeitDatum){
		AtgVerkehrsDatenKurzZeitAnalyseFs parameter = this.parameter.get(kurzZeitDatum);
		MesswertUnskaliert qB = new MesswertUnskaliert("qB", analyseDatum); //$NON-NLS-1$
		boolean nichtErmittelbarFehlerhaft = true;
		if(parameter.isInitialisiert()){
			double k1 = parameter.getFlk1();
			double k2 = parameter.getFlk2();
			
			DaMesswertUnskaliert vPkw = new DaMesswertUnskaliert("vPkw", analyseDatum); //$NON-NLS-1$
			DaMesswertUnskaliert vLkw = new DaMesswertUnskaliert("vLkw", analyseDatum); //$NON-NLS-1$
			DaMesswertUnskaliert qPkw = new DaMesswertUnskaliert("qPkw", analyseDatum); //$NON-NLS-1$
			DaMesswertUnskaliert qLkw = new DaMesswertUnskaliert("qLkw", analyseDatum); //$NON-NLS-1$

			if(vPkw.getWertUnskaliert() >= 0 &&
			   vLkw.getWertUnskaliert() >= 0 &&
			   qPkw.getWertUnskaliert() >= 0 &&
			   qLkw.getWertUnskaliert() >= 0){
				double fL = 0;
				if(vPkw.getWertUnskaliert() > vLkw.getWertUnskaliert()){
					fL = k1 + k2 * (vPkw.getWertUnskaliert() - vLkw.getWertUnskaliert());
				}else{
					fL = k1;
				}
				long qBWert = Math.round((double)qPkw.getWertUnskaliert() + fL * (double)qLkw.getWertUnskaliert());
				
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("qB"), qBWert)){ //$NON-NLS-1$
					qB.setWertUnskaliert(qBWert);
					double qPkwGuete = qPkw.getGueteIndex();
					double qLkwGuete = qLkw.getGueteIndex();
					
					double qBGuete = (qPkwGuete + fL * qLkwGuete) / (1.0 + fL);
					if(qBGuete < 0.0 || qBGuete > 1.0){
						qBGuete = 1.0;
					}
					qB.setGueteIndex(qBGuete);
					
					if(qPkw.isPlausibilisiert() || qLkw.isPlausibilisiert()){
						qB.setInterpoliert(true);
					}
					nichtErmittelbarFehlerhaft = false;
				}
			}				
		}
		if(nichtErmittelbarFehlerhaft){
			qB.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		qB.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet die Bemessungsdichte <code>kB</code> (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 **/
	private final void berechneBemessungsDichte(Data analyseDatum, ResultData kurzZeitDatum){
		DaMesswertUnskaliert vT = new DaMesswertUnskaliert("vKfz", analyseDatum); //$NON-NLS-1$
		
		MesswertUnskaliert zielK = new MesswertUnskaliert("kB"); //$NON-NLS-1$
		
		if(vT.getWertUnskaliert() == 0 ||
		   vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			
			ResultData analyseTminus1 = this.fsAufDatenPuffer.get(kurzZeitDatum.getObject());
			if(analyseTminus1 != null &&
			   analyseTminus1.getData() != null){
				MesswertUnskaliert kTminus1 = new MesswertUnskaliert("kB", //$NON-NLS-1$
						analyseTminus1.getData());
				
				long kTminus1Wert = kTminus1.getWertUnskaliert();				
				AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = this.parameter.get(kurzZeitDatum.getObject());
				if(fsParameter.isInitialisiert()){
					long kGrenz = -1;
					long kMax = -1;

					kGrenz = fsParameter.getKBGrenz();
					kGrenz = fsParameter.getKBMax();

					if(kTminus1Wert < kGrenz){
						zielK.setWertUnskaliert(0);
					}else{
						zielK.setWertUnskaliert(kMax);
					}
				}
			}
		}else{
			DaMesswertUnskaliert qT = new DaMesswertUnskaliert("qB", analyseDatum); //$NON-NLS-1$
			
			if(qT.getWertUnskaliert() >= 0 && vT.getWertUnskaliert() > 0){
				zielK.setWertUnskaliert(Math.round((double)qT.getWertUnskaliert() / (double)vT.getWertUnskaliert()));
				
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("kKfz"), zielK.getWertUnskaliert())){ //$NON-NLS-1$
					try {
						GWert qGuete = new GWert(analyseDatum.getItem("qB").getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
						GWert vGuete = new GWert(analyseDatum.getItem("vKfz").getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
						GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
						zielK.setGueteIndex(kGuete.getIndex());
						zielK.setVerfahren(kGuete.getVerfahren().getCode());
					} catch (GueteException e) {
						LOGGER.error("Guete-Index fuer kB nicht berechenbar", e); //$NON-NLS-1$
						e.printStackTrace();
					}
					
					if(qT.isPlausibilisiert() || vT.isPlausibilisiert()){
						zielK.setInterpoliert(true);
					}
				}
			}
		}
		
		if(zielK.getWertUnskaliert() == -4){
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		
		zielK.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Berechnet die Fahrzeugdichte (nach SE-02.00.00.00.00-AFo-4.0 S.116f)
	 * 
	 * @param analyseDatum ein Analysedatum, für das die Fahrzeugdichte ermittelt werden soll
	 * (in dieses Datum wird das Ergebnis geschrieben)
	 * @param kurzZeitDatum ein KZ-Datum, auf dem der zu ermittelnde Analysewert basieren soll
	 * @param fahrZeugKlasse eine Fahrzeugklasse, für die der Analysewert ermittelt werden soll
	 */
	private final void berechneDichte(Data analyseDatum, ResultData kurzZeitDatum, String fahrZeugKlasse){
		DaMesswertUnskaliert vT = new DaMesswertUnskaliert("v" + fahrZeugKlasse, analyseDatum); //$NON-NLS-1$
		
		MesswertUnskaliert zielK = new MesswertUnskaliert("k" + fahrZeugKlasse); //$NON-NLS-1$
		
		if(vT.getWertUnskaliert() == 0 ||
		   vT.getWertUnskaliert() == DUAKonstanten.NICHT_ERMITTELBAR){
			
			ResultData analyseTminus1 = this.fsAufDatenPuffer.get(kurzZeitDatum.getObject());
			if(analyseTminus1 != null &&
			   analyseTminus1.getData() != null){
				MesswertUnskaliert kTminus1 = new MesswertUnskaliert("k" + fahrZeugKlasse, //$NON-NLS-1$
						analyseTminus1.getData());
				
				long kTminus1Wert = kTminus1.getWertUnskaliert();				
				AtgVerkehrsDatenKurzZeitAnalyseFs fsParameter = this.parameter.get(kurzZeitDatum.getObject());
				if(fsParameter.isInitialisiert()){
					long kGrenz = -1;
					long kMax = -1;

					if(fahrZeugKlasse.startsWith("K")){	// Kfz //$NON-NLS-1$
						kGrenz = fsParameter.getKKfzGrenz();
						kGrenz = fsParameter.getKKfzMax();
					}else
					if(fahrZeugKlasse.startsWith("L")){	// Lkw //$NON-NLS-1$
						kGrenz = fsParameter.getKLkwGrenz();
						kGrenz = fsParameter.getKLkwMax();						
					}else{	// Pkw
						kGrenz = fsParameter.getKPkwGrenz();
						kGrenz = fsParameter.getKPkwMax();
					}

					if(kTminus1Wert < kGrenz){
						zielK.setWertUnskaliert(0);
					}else{
						zielK.setWertUnskaliert(kMax);
					}
				}
			}
		}else{
			DaMesswertUnskaliert qT = new DaMesswertUnskaliert("q" + fahrZeugKlasse, analyseDatum); //$NON-NLS-1$
			
			if(qT.getWertUnskaliert() >= 0 && vT.getWertUnskaliert() > 0){
				zielK.setWertUnskaliert(Math.round((double)qT.getWertUnskaliert() / (double)vT.getWertUnskaliert()));
				
				if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem("k" + fahrZeugKlasse), zielK.getWertUnskaliert())){ //$NON-NLS-1$
					try {
						GWert qGuete = new GWert(analyseDatum.getItem("q" + fahrZeugKlasse).getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
						GWert vGuete = new GWert(analyseDatum.getItem("v" + fahrZeugKlasse).getItem("Güte")); //$NON-NLS-1$ //$NON-NLS-2$
						GWert kGuete = GueteVerfahren.quotient(qGuete, vGuete);
						zielK.setGueteIndex(kGuete.getIndex());
						zielK.setVerfahren(kGuete.getVerfahren().getCode());
					} catch (GueteException e) {
						LOGGER.error("Guete-Index fuer k" + fahrZeugKlasse + //$NON-NLS-1$
								" nicht berechenbar", e); //$NON-NLS-1$
						e.printStackTrace();
					}
					
					if(qT.isPlausibilisiert() || vT.isPlausibilisiert()){
						zielK.setInterpoliert(true);
					}
				}
			}
		}
		
		if(zielK.getWertUnskaliert() == -4){
			zielK.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
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
	private final void berechneVerkehrsStaerke(Data analyseDatum, ResultData kurzZeitDatum, String attName){
		long TinS = kurzZeitDatum.getData().getTimeValue("T").getMillis() / 1000; //$NON-NLS-1$
		DaMesswertUnskaliert qMwe = new DaMesswertUnskaliert(attName, kurzZeitDatum.getData());
		MesswertUnskaliert qAnalyse = new MesswertUnskaliert(attName);
		
		boolean nichtErmittelbarFehlerhaft = true;
		if(TinS > 0 && qMwe.getWertUnskaliert() >= 0){
			long q = Math.round(((double)qMwe.getWertUnskaliert() * 3600.0 / (double)TinS));
			if(DUAUtensilien.isWertInWerteBereich(analyseDatum.getItem(attName), q)){
				qAnalyse.setWertUnskaliert(q);
				qAnalyse.setGueteIndex(qMwe.getGueteIndex());
				qAnalyse.setVerfahren(qMwe.getVerfahren());
		
				if(qMwe.isPlausibilisiert()){
					qAnalyse.setInterpoliert(true);
				}
				nichtErmittelbarFehlerhaft = false;
			}
		}
		if(nichtErmittelbarFehlerhaft){
			qAnalyse.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		
		qAnalyse.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * Übernimmt einen Wert aus dem Kurzzeitdatum in das Analysedatum
	 * 
	 * @param analyseDatum das Datum in das die Daten eingetragen werden sollen
	 * @param kurzZeitDatum das Roh-KZ-Datum
	 * @param attName der Attributname des Verkehrswertes, der übernommen werden soll
	 */
	private final void uebernehmeWert(Data analyseDatum, ResultData kurzZeitDatum, String attName){
		DaMesswertUnskaliert mweWert = new DaMesswertUnskaliert(attName, kurzZeitDatum.getData());
		MesswertUnskaliert analyseWert = new MesswertUnskaliert(attName);
		
		boolean nichtErmittelbarFehlerhaft = true;
		if(mweWert.getWertUnskaliert() >= 0){
			analyseWert.setWertUnskaliert(mweWert.getWertUnskaliert());
			analyseWert.setGueteIndex(mweWert.getGueteIndex());
			analyseWert.setVerfahren(mweWert.getVerfahren());
			if(mweWert.isPlausibilisiert()){
				analyseWert.setInterpoliert(true);
			}
			nichtErmittelbarFehlerhaft = false;
		}
		if(nichtErmittelbarFehlerhaft){
			analyseWert.setWertUnskaliert(DUAKonstanten.NICHT_ERMITTELBAR_BZW_FEHLERHAFT);
		}
		
		analyseWert.kopiereInhaltNach(analyseDatum);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public ModulTyp getModulTyp() {
		return null;
	}
	
	
	/**
	 * {@inheritDoc}.<br>
	 * 
	 * Diese Methode macht nichts, da die Publikation in
	 * diesem Modul nicht dynamisch erfolgt
	 */
	public void aktualisierePublikation(IDatenFlussSteuerung dfs) {
		// hier wird nicht dynamisch publiziert
	}

}
