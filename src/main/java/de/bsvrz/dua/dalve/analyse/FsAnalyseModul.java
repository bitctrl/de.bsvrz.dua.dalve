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
package de.bsvrz.dua.dalve.analyse;

import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.dalve.analyse.lib.CommonFunctions;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAInitialisierungsException;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.MesswertUnskaliert;
import de.bsvrz.sys.funclib.bitctrl.dua.adapter.AbstraktBearbeitungsKnotenAdapter;
import de.bsvrz.sys.funclib.bitctrl.dua.av.DAVObjektAnmeldung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.schnittstellen.IDatenFlussSteuerung;
import de.bsvrz.sys.funclib.bitctrl.dua.dfs.typen.ModulTyp;
import de.bsvrz.sys.funclib.bitctrl.dua.schnittstellen.IVerwaltung;
import de.bsvrz.sys.funclib.debug.Debug;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

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

	private static final Debug _debug = Debug.getLogger();
	
	/**
	 * Datenbeschreibung zur Publikation der Fahrstreifen-Analysedaten
	 */
	protected static DataDescription PUB_BESCHREIBUNG = null; 
	
	/**
	 * Mapt jeden Fahrstreifen auf seine aktuellen Analyse-Parameter
	 */
	private Map<SystemObject, CommonFunctions> parameters = new HashMap<>();
	
	/**
	 * Mapt jeden Fahrstreifen auf seinen letzten Analysedatensatz
	 */
	private Map<SystemObject, ResultData> fsAufDatenPuffer = new HashMap<SystemObject, ResultData>();

	
	@Override
	public void initialisiere(IVerwaltung dieVerwaltung)
	throws DUAInitialisierungsException {
		super.initialisiere(dieVerwaltung);
		this.setPublikation(true);
		
		if(PUB_BESCHREIBUNG == null){
			PUB_BESCHREIBUNG = new DataDescription(
					dieVerwaltung.getVerbindung().getDataModel().getAttributeGroup(DUAKonstanten.ATG_KURZZEIT_FS),
					dieVerwaltung.getVerbindung().getDataModel().getAspect(DUAKonstanten.ASP_ANALYSE));
		}
		
		/**
		 * Publikations- und Parameteranmeldungen durchfuehren
		 */
		Collection<DAVObjektAnmeldung> anmeldungen = new TreeSet<DAVObjektAnmeldung>();
		for(SystemObject fsObj:dieVerwaltung.getSystemObjekte()){
			this.parameters.put(fsObj,
			                    new CommonFunctions(dieVerwaltung.getVerbindung(), fsObj));
			try {
				anmeldungen.add(new DAVObjektAnmeldung(fsObj, PUB_BESCHREIBUNG));
			} catch (IllegalArgumentException e) {
				throw new DUAInitialisierungsException("", e); 
			}
		}
	
		this.publikationsAnmeldungen.modifiziereObjektAnmeldung(anmeldungen);		
	}


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
	 * Berechnet ein Analysedatum eines Fahrstreifens in Bezug auf den
	 * übergebenen KZ-Datensatz
	 * 
	 * @param kurzZeitDatum ein aktueller KZ-Datensatz
	 * @return ein Analysedatum eines Fahrstreifens in Bezug auf den
	 * uebergebenen KZ-Datensatz oder <code>null</code>, wenn dieses
	 * nicht ermittelt werden konnte
	 */
	private final ResultData getAnalyseDatum(final ResultData kurzZeitDatum){
		ResultData ergebnis = null;
				
		if(kurzZeitDatum.getData() != null){
			Data analyseDatum = this.verwaltung.getVerbindung().createData(PUB_BESCHREIBUNG.getAttributeGroup());			
			
			analyseDatum.getTimeValue("T").setMillis(//$NON-NLS-1$
					kurzZeitDatum.getData().getTimeValue("T").getMillis());  //$NON-NLS-1$

			CommonFunctions commonFunctions = this.parameters.get(kurzZeitDatum.getObject());

			/**
			 * Berechne Verkehrsstärken
			 */
			commonFunctions.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qKfz"); //$NON-NLS-1$
			commonFunctions.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qLkw"); //$NON-NLS-1$
			commonFunctions.berechneVerkehrsStaerke(analyseDatum, kurzZeitDatum, "qPkw"); //$NON-NLS-1$
			
			/**
			 * Mittlere Geschwindigkeiten,
			 * Belegung und Standardabweichung werden einfach übernommen
			 */
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "vKfz"); //$NON-NLS-1$
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "vLkw"); //$NON-NLS-1$
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "vPkw"); //$NON-NLS-1$
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "vgKfz"); //$NON-NLS-1$
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "b"); //$NON-NLS-1$
			commonFunctions.uebernehmeWert(analyseDatum, kurzZeitDatum, "sKfz"); //$NON-NLS-1$
			
			/**
			 * Berechne Lkw-Anteil (ab hier sind Analysedaten selbst Eingangswerte)
			 */
			commonFunctions.berechneLkwAnteil(analyseDatum);
			
			/**
			 * Berechne Dichten
			 */
			commonFunctions.berechneDichte(analyseDatum, "Kfz", () -> {
				MesswertUnskaliert kTminus11;
				if(this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()) != null &&
						this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData() != null) {
					kTminus11 = new MesswertUnskaliert(
							"k" + "Kfz", //$NON-NLS-1$
							this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData()
					);

				}
				else {
					kTminus11 = new MesswertUnskaliert("k" + "Kfz");
					kTminus11.setWertUnskaliert(0);
				}
				return kTminus11.getWertUnskaliert();
			}); //$NON-NLS-1$
			commonFunctions.berechneDichte(analyseDatum, "Lkw", () -> {
				MesswertUnskaliert kTminus11;
				if(this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()) != null &&
						this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData() != null) {
					kTminus11 = new MesswertUnskaliert(
							"k" + "Lkw", //$NON-NLS-1$
							this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData()
					);

				}
				else {
					kTminus11 = new MesswertUnskaliert("k" + "Lkw");
					kTminus11.setWertUnskaliert(0);
				}
				return kTminus11.getWertUnskaliert();
			}); //$NON-NLS-1$
			commonFunctions.berechneDichte(analyseDatum, "Pkw", () -> {
				MesswertUnskaliert kTminus11;
				if(this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()) != null &&
						this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData() != null) {
					kTminus11 = new MesswertUnskaliert(
							"k" + "Pkw", //$NON-NLS-1$
							this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData()
					);

				}
				else {
					kTminus11 = new MesswertUnskaliert("k" + "Pkw");
					kTminus11.setWertUnskaliert(0);
				}
				return kTminus11.getWertUnskaliert();
			}); //$NON-NLS-1$
			
			/**
			 * Berechne Bemessungsverkehrsstärke
			 */
			commonFunctions.berechneBemessungsVerkehrsStaerke(analyseDatum);
			
			/**
			 * Berechne Bemessungsdichte
			 */
			commonFunctions.berechneBemessungsDichte(analyseDatum, () -> {
				MesswertUnskaliert kTminus11;
				if(this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()) != null &&
						this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData() != null) {
					kTminus11 = new MesswertUnskaliert(
							"kB", //$NON-NLS-1$
							this.fsAufDatenPuffer.get(kurzZeitDatum.getObject()).getData()
					);
				}
				else {
					kTminus11 = new MesswertUnskaliert("kB");
					kTminus11.setWertUnskaliert(0);
				}
				return kTminus11.getWertUnskaliert();
			});
			
			/**
			 * Ergebnisdatensatz
			 */
			ergebnis = new ResultData(
					kurzZeitDatum.getObject(),
					PUB_BESCHREIBUNG, 
					kurzZeitDatum.getDataTime(),
					analyseDatum);
		}else{
			ResultData letzterPublizierterWert = this.fsAufDatenPuffer.get(kurzZeitDatum.getObject());
			if( letzterPublizierterWert == null || letzterPublizierterWert.getData() != null ){
				ergebnis = new ResultData(
						kurzZeitDatum.getObject(),
						PUB_BESCHREIBUNG,
						kurzZeitDatum.getDataTime(),
						null);
			}
		}
		
		return ergebnis;
	}


	public ModulTyp getModulTyp() {
		return null;
	}
	
	
	public void aktualisierePublikation(IDatenFlussSteuerung dfs) {
		// hier wird nicht dynamisch publiziert
	}

}
