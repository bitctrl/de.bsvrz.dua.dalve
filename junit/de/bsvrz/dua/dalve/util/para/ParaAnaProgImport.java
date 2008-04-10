/** 
 * Segment 4 Datenübernahme und Aufbereitung (DUA)
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

package de.bsvrz.dua.dalve.util.para;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.bitctrl.daf.DaVKonstanten;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAUtensilien;
import de.bsvrz.sys.funclib.bitctrl.dua.test.CSVImporter;

/**
 * Abstrakte Klasse zum Einlesen von Parametern aus der CSV-Datei 
 * innerhalb der Prüfspezifikation
 * 
 * @author BitCtrl Systems GmbH, Thierfelder
 *
 */
public class ParaAnaProgImport
extends CSVImporter
implements ClientSenderInterface {
	
	/**
	 * Verbindung zum Datenverteiler
	 */
	protected static ClientDavInterface DAV = null;
	
	/**
	 * Systemobjekt, für das die Parameter gesetzt werden sollen
	 */
	protected SystemObject[] objekt = null;
	
	/**
	 * Attributgruppe der FS-Analyse-Parameter
	 */
	private AttributeGroup ATG_FSAnalyse;

	/**
	 * Attributgruppe der FS-Prognose-Parameter (Flink)
	 */
	private AttributeGroup ATG_FSPrognoseFlink;
	
	/**
	 * Attributgruppe der FS-Prognose-Parameter (Normal)
	 */
	private AttributeGroup ATG_FSPrognoseNormal;
	
	/**
	 * Attributgruppe der FS-Prognose-Parameter (Träge)
	 */
	private AttributeGroup ATG_FSPrognoseTraege;
	
	/**
	 * Datenbeschreibung FS-Analyse
	 */
	private DataDescription DD_FSAnalyse;

	/**
	 * Datenbeschreibung FS-Prognose (Flink)
	 */
	private DataDescription DD_FSPrognoseFlink;
	
	/**
	 * Datenbeschreibung FS-Prognose (Normal)
	 */
	private DataDescription DD_FSPrognoseNormal;
	
	/**
	 * Datenbeschreibung FS-Prognose (Träge)
	 */
	private DataDescription DD_FSPrognoseTraege;
	
	/**
	 * Standardkonstruktor
	 * 
	 * @param dav Datenverteier-Verbindung
	 * @param objekt das Systemobjekt, für das die Parameter gesetzt werden sollen
	 * @param csvQuelle Quelle der Daten (CSV-Datei)
	 * @throws Exception falls dieses Objekt nicht vollständig initialisiert werden konnte
	 */
	public ParaAnaProgImport(final ClientDavInterface dav, 
								   final SystemObject[] objekt,
						   		   final String csvQuelle)
	throws Exception{
		super(csvQuelle);
		if(DAV == null){
			DAV = dav;
		}
		
		ATG_FSAnalyse = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitAnalyseFs"); //$NON-NLS-1$
		ATG_FSPrognoseFlink = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseFlinkFs"); //$NON-NLS-1$
		ATG_FSPrognoseNormal = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseNormalFs"); //$NON-NLS-1$
		ATG_FSPrognoseTraege = DAV.getDataModel().getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationPrognoseTrägeFs"); //$NON-NLS-1$
		
		DD_FSAnalyse = new DataDescription(
				ATG_FSAnalyse, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_FSPrognoseFlink = new DataDescription(
				ATG_FSPrognoseFlink, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		
		DD_FSPrognoseNormal = new DataDescription(
				ATG_FSPrognoseNormal, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		DD_FSPrognoseTraege = new DataDescription(
				ATG_FSPrognoseTraege, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0);
		
		this.objekt = objekt;
	}
	
		
	/**
	 * Führt den Parameterimport aus
	 * 
	 * @param index der index
	 * @throws Exception wenn die Parameter nicht vollständig
	 * importiert werden konnten
	 */
	public final void importiereParameterAnalyse(int index)
	throws Exception{
		
		this.reset();
		this.getNaechsteZeile();
		
		SystemObject fsObjekt = null;
		for(SystemObject sysObjekt : objekt) {
			if(sysObjekt.getName().endsWith("."+index)) { //$NON-NLS-1$
				fsObjekt = sysObjekt;
			}
		}
		
		DAV.subscribeSender(this, fsObjekt,DD_FSAnalyse, SenderRole.sender());
		
		String[] zeile = null;
		
		Data parameter = DAV.createData(ATG_FSAnalyse);
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
			
			String attPfadAnalyse = getAnalyseAttributPfadVon(attributInCSVDatei, index);
			if(attPfadAnalyse != null){
				try{
					long l = Long.parseLong(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(l);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue().set(l);
				}catch(NumberFormatException ex){
					double d = Double.parseDouble(wert);
					//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(d);
					DUAUtensilien.getAttributDatum(attPfadAnalyse, parameter).asScaledValue().set(d);
				}
			}
		}
		
		ResultData resultat = new ResultData(fsObjekt, new DataDescription(
				ATG_FSAnalyse, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameter);
		DAV.sendData(resultat);
		
		DAV.unsubscribeSender(this, fsObjekt, DD_FSAnalyse);
	}

	/**
	 * Führt den Parameterimport aus
	 * 
	 * @param index der index
	 * @throws Exception wenn die Parameter nicht vollständig
	 * importiert werden konnten
	 */
	public final void importiereParameterPrognose()
	throws Exception{
		
		this.reset();
		this.getNaechsteZeile();
		
		SystemObject fsObjekt = null;
		for(SystemObject sysObjekt : objekt) {
			fsObjekt = sysObjekt;
		}
		
		DAV.subscribeSender(this, fsObjekt,DD_FSPrognoseFlink, SenderRole.sender());
		DAV.subscribeSender(this, fsObjekt,DD_FSPrognoseNormal, SenderRole.sender());
		DAV.subscribeSender(this, fsObjekt,DD_FSPrognoseTraege, SenderRole.sender());
		
		String[] zeile = null;
		
		Data parameterProgFlink = DAV.createData(ATG_FSPrognoseFlink);
		Data parameterProgNorm = DAV.createData(ATG_FSPrognoseNormal);
		Data parameterProgTraege = DAV.createData(ATG_FSPrognoseTraege);
		
		String qStartwert = null;
		String vStartwert = null;
		
		while( (zeile = this.getNaechsteZeile()) != null ){
			String attributInCSVDatei = zeile[0];
			String wert = zeile[1];
			wert = wert.replaceAll(",", "."); //$NON-NLS-1$ //$NON-NLS-2$
			
			//Prognose Flink
			String attPfadPFlink = getPrognoseAttributPfadVon(attributInCSVDatei, "flink");
			if(attPfadPFlink != null){
				setParameterResult(parameterProgFlink, attPfadPFlink, wert);
			}
			
			//Prognose Normal
			String attPfadPNormal = getPrognoseAttributPfadVon(attributInCSVDatei, "normal");
			if(attPfadPNormal != null){
				setParameterResult(parameterProgNorm, attPfadPNormal, wert);
			}
			
			//Prognose Träge
			String attPfadPTraege = getPrognoseAttributPfadVon(attributInCSVDatei, "träge");
			if(attPfadPTraege != null){
				setParameterResult(parameterProgTraege, attPfadPTraege, wert);
			}
			
			if(attributInCSVDatei.startsWith("ZAltStartQ")) {
				qStartwert = wert;
			}
			
			if(attributInCSVDatei.startsWith("ZAltStartV")) {
				vStartwert = wert;
			}
			
			if(qStartwert != null && vStartwert != null) {
				setPrognoseStartwerte(parameterProgFlink, qStartwert, vStartwert);
				setPrognoseStartwerte(parameterProgNorm, qStartwert, vStartwert);
				setPrognoseStartwerte(parameterProgTraege, qStartwert, vStartwert);
			}
		}
		
		ResultData resultatFlink = new ResultData(fsObjekt, new DataDescription(
				ATG_FSPrognoseFlink, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgFlink);
		
		ResultData resultatNormal = new ResultData(fsObjekt, new DataDescription(
				ATG_FSPrognoseNormal, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgNorm);
		
		ResultData resultatTraege = new ResultData(fsObjekt, new DataDescription(
				ATG_FSPrognoseTraege, 
				DAV.getDataModel().getAspect(DaVKonstanten.ASP_PARAMETER_VORGABE),
				(short)0), System.currentTimeMillis(), parameterProgTraege);
		
		DAV.sendData(resultatFlink);
		DAV.sendData(resultatNormal);
		DAV.sendData(resultatTraege);
		
		DAV.unsubscribeSender(this, fsObjekt, DD_FSPrognoseFlink);
		DAV.unsubscribeSender(this, fsObjekt, DD_FSPrognoseNormal);
		DAV.unsubscribeSender(this, fsObjekt, DD_FSPrognoseTraege);
	}
	
	/**
	 * Erfragt den Attributpfad zu einem Analyse-Attribut, das in der CSV-Datei 
	 * den übergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getAnalyseAttributPfadVon(final String attributInCSVDatei,
												 final int index) {

		if(attributInCSVDatei.endsWith(")")){ //$NON-NLS-1$
			String nummerStr = attributInCSVDatei.substring(
					attributInCSVDatei.length() - 2, attributInCSVDatei.length() - 1);
			int nummer = -1;
			try{
				nummer = Integer.parseInt(nummerStr);
			}catch(Exception ex){
				//
			}

			if(nummer == index){
				if(attributInCSVDatei.startsWith("kKfzGrenz")){ //$NON-NLS-1$
					return "kKfz.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kKfzMax")){ //$NON-NLS-1$
					return "kKfz.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwGrenz")){ //$NON-NLS-1$
					return "kLkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kLkwMax")){ //$NON-NLS-1$
					return "kLkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwGrenz")){ //$NON-NLS-1$
					return "kPkw.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kPkwMax")){ //$NON-NLS-1$
					return "kPkw.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBGrenz")){ //$NON-NLS-1$
					return "kB.Grenz"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("kBMax")){ //$NON-NLS-1$
					return "kB.Max"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("k1")){ //$NON-NLS-1$
					return "fl.k1"; //$NON-NLS-1$
				}
				if(attributInCSVDatei.startsWith("k2")){ //$NON-NLS-1$
					return "fl.k2"; //$NON-NLS-1$
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Erfragt den Attributpfad zu einem Prognose-Attribut, das in der CSV-Datei 
	 * den übergebenen Namen hat
	 *  
	 * @param attributInCSVDatei Attributname innerhalb der CSV-Datei
	 * @param index index innerhalb von CVS-Datei
	 * @return den kompletten Attributpfad zum assoziierten DAV-Attribut
	 */
	protected String getPrognoseAttributPfadVon(final String attributInCSVDatei, final String prognoseTyp) {

		if(attributInCSVDatei.startsWith("alpha1"+prognoseTyp)){ //$NON-NLS-1$
			return ".alpha1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("alpha2"+prognoseTyp)){ //$NON-NLS-1$
			return ".alpha2"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("beta1"+prognoseTyp)){ //$NON-NLS-1$
			return ".beta1"; //$NON-NLS-1$
		}
		if(attributInCSVDatei.startsWith("beta2"+prognoseTyp)){ //$NON-NLS-1$
			return ".beta2"; //$NON-NLS-1$
		}
		
		return null;
	}
	
	protected void setParameterResult(final Data parameter, final String attPfad, final String wert) {
		String[] atts = {"qKfz", "vKfz", "qLkw", "vLkw", "qPkw", "vPkw", "aLkw", "kKfz", "kLkw", "kPkw", "qB", "kB"};
		
		for(String att : atts) {
			String attPfadPrognose = att+attPfad;
			try{
				long l = Long.parseLong(wert);
				//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(l);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wert);
				//DUAUtensilien.getAttributDatum(attPfad, parameter).asUnscaledValue().set(d);
				DUAUtensilien.getAttributDatum(attPfadPrognose, parameter).asScaledValue().set(d);
			}
			
		}
	}
	
	protected void setPrognoseStartwerte(final Data parameter, final String wertQ, final String wertV) {
		String[] atts = {"KfzStart", "LkwStart", "PkwStart"};
		
		for(String att : atts) {
			String attPfadQ = "q"+att;
			String attPfadV = "v"+att;
			String attPfadK = "k"+att;
			
			try{
				long l = Long.parseLong(wertQ);
				DUAUtensilien.getAttributDatum(attPfadQ, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wertQ);
				DUAUtensilien.getAttributDatum(attPfadQ, parameter).asScaledValue().set(d);
			}
			
			try{
				long l = Long.parseLong(wertV);
				DUAUtensilien.getAttributDatum(attPfadV, parameter).asScaledValue().set(l);
			}catch(NumberFormatException ex){
				double d = Double.parseDouble(wertV);
				DUAUtensilien.getAttributDatum(attPfadV, parameter).asScaledValue().set(d);
			}
			
			DUAUtensilien.getAttributDatum(attPfadK, parameter).asUnscaledValue().set(0);	
			
		}
		
		DUAUtensilien.getAttributDatum("aLkwStart", parameter).asUnscaledValue().set(0);
		
		try{
			long l = Long.parseLong(wertQ);
			DUAUtensilien.getAttributDatum("qBStart", parameter).asScaledValue().set(l);
		}catch(NumberFormatException ex){
			double d = Double.parseDouble(wertQ);
			DUAUtensilien.getAttributDatum("qBStart", parameter).asScaledValue().set(d);
		}
		
		DUAUtensilien.getAttributDatum("kBStart", parameter).asUnscaledValue().set(0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dataRequest(SystemObject object, DataDescription dataDescription, byte state) {
		// keine Überprüfung
	}
	

	/**
	 * {@inheritDoc}
	 */
	public boolean isRequestSupported(SystemObject object, DataDescription dataDescription) {
		return false;
	}
	
}
